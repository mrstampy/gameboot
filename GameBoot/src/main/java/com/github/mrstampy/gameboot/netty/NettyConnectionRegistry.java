/*
 *              ______                        ____              __ 
 *             / ____/___ _____ ___  ___     / __ )____  ____  / /_
 *            / / __/ __ `/ __ `__ \/ _ \   / __  / __ \/ __ \/ __/
 *           / /_/ / /_/ / / / / / /  __/  / /_/ / /_/ / /_/ / /_  
 *           \____/\__,_/_/ /_/ /_/\___/  /_____/\____/\____/\__/  
 *                                                 
 *                                 .-'\
 *                              .-'  `/\
 *                           .-'      `/\
 *                           \         `/\
 *                            \         `/\
 *                             \    _-   `/\       _.--.
 *                              \    _-   `/`-..--\     )
 *                               \    _-   `,','  /    ,')
 *                                `-_   -   ` -- ~   ,','
 *                                 `-              ,','
 *                                  \,--.    ____==-~
 *                                   \   \_-~\
 *                                    `_-~_.-'
 *                                     \-~
 * 
 *                       http://mrstampy.github.io/gameboot/
 *
 * Copyright (C) 2015, 2016 Burton Alexander
 * 
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 51
 * Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 * 
 */
package com.github.mrstampy.gameboot.netty;

import static com.github.mrstampy.gameboot.messaging.MessagingGroups.ALL;
import static org.apache.commons.lang3.StringUtils.isEmpty;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.github.mrstampy.gameboot.metrics.MetricsHelper;
import com.github.mrstampy.gameboot.systemid.SystemIdKey;
import com.github.mrstampy.gameboot.util.GameBootUtils;
import com.github.mrstampy.gameboot.util.registry.AbstractRegistryKey;
import com.github.mrstampy.gameboot.util.registry.GameBootRegistry;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.ChannelGroupFuture;
import io.netty.channel.group.ChannelMatcher;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.util.concurrent.ImmediateEventExecutor;

/**
 * Keeps up-to-date maps of key/{@link Channel} pairs and facilitates easy
 * creation of {@link ChannelGroup}s. Messages can be sent to individuals or
 * groups.
 * 
 * @see AbstractNettyMessageHandler
 */
@Component
public class NettyConnectionRegistry extends GameBootRegistry<Channel> {
  private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private static final String NETTY_CONNECTIONS = "Netty Connections";

  private static final ChannelMatcher NOOP_MATCHER = new ChannelMatcher() {

    @Override
    public boolean matches(Channel channel) {
      return true;
    }
  };

  @Autowired
  private MetricsHelper helper;

  @Autowired
  private GameBootUtils utils;

  private Map<String, ChannelGroup> groups = new ConcurrentHashMap<>();

  /**
   * Post construct.
   *
   * @throws Exception
   *           the exception
   */
  @PostConstruct
  public void postConstruct() throws Exception {
    helper.gauge(() -> allConnected(), NETTY_CONNECTIONS, getClass(), "netty", "connections");
  }

  /**
   * Returns true if the group specified by the groupName exists.
   *
   * @param groupName
   *          the group key
   * @return true, if successful
   */
  public boolean containsGroup(String groupName) {
    groupCheck(groupName);
    return groups.containsKey(groupName);
  }

  /**
   * Adds the channel mapped by the key to the registry.
   *
   * @param key
   *          the key
   * @param channel
   *          the channel
   */
  public void put(AbstractRegistryKey<?> key, Channel channel) {
    super.put(key, channel);
    channel.closeFuture().addListener(f -> map.remove(key));
  }

  /**
   * Sends the message to the {@link Channel} specified by the
   * {@link AbstractRegistryKey}.
   *
   * @param key
   *          the key
   * @param message
   *          the message
   * @param listeners
   *          the listeners
   */
  public void send(AbstractRegistryKey<?> key, String message, ChannelFutureListener... listeners) {
    checkKey(key);
    checkMessage(message);

    Channel channel = get(key);

    sendMessage(key, message, channel, listeners);
  }

  /**
   * Sends the message to the {@link Channel} specified by the
   * {@link AbstractRegistryKey}.
   *
   * @param key
   *          the key
   * @param message
   *          the message
   * @param listeners
   *          the listeners
   */
  public void send(AbstractRegistryKey<?> key, byte[] message, ChannelFutureListener... listeners) {
    checkKey(key);
    checkMessage(message);

    Channel channel = get(key);

    sendMessage(key, message, channel, listeners);
  }

  /**
   * Put in all.
   *
   * @param channel
   *          the channel
   */
  public void putInAll(Channel channel) {
    putInGroup(ALL, channel);
  }

  /**
   * Puts the channel in the specified group, creating the group if it does not
   * yet exist.
   *
   * @param groupName
   *          the group key
   * @param channel
   *          the channel
   */
  public void putInGroup(String groupName, Channel channel) {
    groupCheck(groupName, channel);

    ChannelGroup group = groups.get(groupName);
    if (group == null) {
      group = new DefaultChannelGroup(groupName, ImmediateEventExecutor.INSTANCE);
      groups.put(groupName, group);
    }

    if (!group.contains(channel)) group.add(channel);
  }

  /**
   * Gets the group.
   *
   * @param groupName
   *          the group key
   * @return the group
   */
  public ChannelGroup getGroup(String groupName) {
    groupCheck(groupName);
    return groups.get(groupName);
  }

  /**
   * Removes the channel from the group specified by groupName.
   *
   * @param groupName
   *          the group key
   * @param channel
   *          the channel
   */
  public void removeFromGroup(String groupName, Channel channel) {
    groupCheck(groupName, channel);

    ChannelGroup group = groups.get(groupName);
    if (group == null) return;

    group.remove(channel);
  }

  /**
   * Removes the channel from group specified by groupName and the key.
   *
   * @param groupName
   *          the group key
   * @param key
   *          the key
   */
  public void removeFromGroup(String groupName, AbstractRegistryKey<?> key) {
    checkKey(key);
    Channel channel = get(key);
    if (channel != null) removeFromGroup(groupName, channel);
  }

  /**
   * Removes the channel from all groups.
   *
   * @param channel
   *          the channel
   */
  public void removeFromGroups(Channel channel) {
    if (channel == null) return;

    groups.forEach((key, g) -> {
      if (g.contains(channel)) g.remove(channel);
    });
  }

  /**
   * Removes the group.
   *
   * @param groupName
   *          the group key
   */
  public void removeGroup(String groupName) {
    groupCheck(groupName);
    if (!groups.containsKey(groupName)) return;

    ChannelGroup group = groups.remove(groupName);

    group.clear();
  }

  /**
   * Send the message to all connected channels.
   *
   * @param message
   *          the message
   * @param listeners
   *          the listeners
   */
  public void sendToAll(String message, ChannelFutureListener... listeners) {
    sendToGroup(ALL, message, listeners);
  }

  /**
   * Send to all.
   *
   * @param message
   *          the message
   * @param matcher
   *          the matcher
   * @param listeners
   *          the listeners
   */
  public void sendToAll(String message, ChannelMatcher matcher, ChannelFutureListener... listeners) {
    sendToGroup(ALL, message, matcher, listeners);
  }

  /**
   * Send the message to all connected channels.
   *
   * @param message
   *          the message
   * @param listeners
   *          the listeners
   */
  public void sendToAll(byte[] message, ChannelFutureListener... listeners) {
    sendToGroup(ALL, message, listeners);
  }

  /**
   * Send to all.
   *
   * @param message
   *          the message
   * @param matcher
   *          the matcher
   * @param listeners
   *          the listeners
   */
  public void sendToAll(byte[] message, ChannelMatcher matcher, ChannelFutureListener... listeners) {
    sendToGroup(ALL, message, matcher, listeners);
  }

  /**
   * Send the message to a specific group.
   *
   * @param groupName
   *          the group key
   * @param message
   *          the message
   * @param listeners
   *          the listeners
   */
  public void sendToGroup(String groupName, String message, ChannelFutureListener... listeners) {
    groupCheck(groupName);
    if (!groups.containsKey(groupName)) {
      log.warn("No group {} to send message {}", groupName, message);
      return;
    }

    ChannelGroup group = groups.get(groupName);

    ChannelFutureListener[] all = utils.prependArray(f -> log((ChannelGroupFuture) f, groupName), listeners);
    ChannelGroupFuture cf = group.writeAndFlush(message);
    cf.addListeners(all);
  }

  /**
   * Send the message to a specific group.
   *
   * @param groupName
   *          the group key
   * @param message
   *          the message
   * @param listeners
   *          the listeners
   */
  public void sendToGroup(String groupName, byte[] message, ChannelFutureListener... listeners) {
    groupCheck(groupName);
    if (!groups.containsKey(groupName)) {
      log.warn("No group {} to send message {}", groupName, message);
      return;
    }

    ChannelGroup group = groups.get(groupName);

    ChannelFutureListener[] all = utils.prependArray(f -> log((ChannelGroupFuture) f, groupName), listeners);
    ChannelGroupFuture cf = group.writeAndFlush(message);
    cf.addListeners(all);
  }

  /**
   * Send to group.
   *
   * @param groupName
   *          the group key
   * @param message
   *          the message
   * @param matcher
   *          the matcher
   * @param listeners
   *          the listeners
   */
  public void sendToGroup(String groupName, String message, ChannelMatcher matcher,
      ChannelFutureListener... listeners) {
    groupCheck(groupName);
    if (!groups.containsKey(groupName)) {
      log.warn("No group {} to send message {}", groupName, message);
      return;
    }

    ChannelGroup group = groups.get(groupName);

    ChannelFutureListener[] all = utils.prependArray(f -> log((ChannelGroupFuture) f, groupName), listeners);
    ChannelGroupFuture cf = group.writeAndFlush(message, matcher);
    cf.addListeners(all);
  }

  /**
   * Send to group.
   *
   * @param groupName
   *          the group key
   * @param message
   *          the message
   * @param matcher
   *          the matcher
   * @param listeners
   *          the listeners
   */
  public void sendToGroup(String groupName, byte[] message, ChannelMatcher matcher,
      ChannelFutureListener... listeners) {
    groupCheck(groupName);
    checkMessage(message);

    if (!groups.containsKey(groupName)) {
      log.warn("No group {} to send message {}", groupName, message);
      return;
    }

    ChannelGroup group = groups.get(groupName);

    ChannelFutureListener[] all = utils.prependArray(f -> log((ChannelGroupFuture) f, groupName), listeners);
    ChannelGroupFuture cf = group.writeAndFlush(message, matcher);
    cf.addListeners(all);
  }

  /**
   * Send to group.
   *
   * @param groupName
   *          the group name
   * @param message
   *          the message
   * @param except
   *          the except
   */
  public void sendToGroup(String groupName, byte[] message, SystemIdKey... except) {
    if (!containsGroup(groupName)) return;

    ChannelMatcher exceptions = createMatcher(except);

    sendToGroup(groupName, message, exceptions);
  }

  /**
   * Send to group.
   *
   * @param groupName
   *          the group name
   * @param message
   *          the message
   * @param except
   *          the except
   */
  public void sendToGroup(String groupName, String message, SystemIdKey... except) {
    if (!containsGroup(groupName)) return;

    ChannelMatcher exceptions = createMatcher(except);

    sendToGroup(groupName, message, exceptions);
  }

  private ChannelMatcher createMatcher(SystemIdKey... except) {
    if (except == null || except.length == 0) return NOOP_MATCHER;

    List<Channel> exceptions = new ArrayList<>();
    for (SystemIdKey key : except) {
      Channel c = get(key);
      if (c != null) exceptions.add(c);
    }

    return exceptions.isEmpty() ? NOOP_MATCHER : new ChannelMatcher() {

      @Override
      public boolean matches(Channel channel) {
        return !exceptions.contains(channel);
      }
    };
  }

  private void sendMessage(Comparable<?> key, String message, Channel channel, ChannelFutureListener... listeners) {
    if (channel == null || !channel.isActive()) {
      log.warn("Cannot send {} to {}", message, channel);
      return;
    }

    ChannelFutureListener[] all = utils.prependArray(f -> log((ChannelFuture) f, key), listeners);
    ChannelFuture f = channel.writeAndFlush(message);
    f.addListeners(all);
  }

  private void sendMessage(Comparable<?> key, byte[] message, Channel channel, ChannelFutureListener... listeners) {
    if (channel == null || !channel.isActive()) {
      log.warn("Cannot send {} to {}", message, channel);
      return;
    }

    ChannelFutureListener[] all = utils.prependArray(f -> log((ChannelFuture) f, key), listeners);
    ChannelFuture f = channel.writeAndFlush(message);
    f.addListeners(all);
  }

  private void log(ChannelGroupFuture e, String groupName) {
    e.iterator().forEachRemaining(cf -> log(cf, groupName));
  }

  private void log(ChannelFuture f, Object key) {
    if (f.isSuccess()) {
      log.debug("Successful send to {} on {}", key, f.channel());
    } else {
      log.error("Failed sending to {} on {}", key, f.channel(), f.cause());
    }
  }

  private int allConnected() {
    ChannelGroup group = getGroup(ALL);

    return group == null ? 0 : group.size();
  }

  private void checkMessage(String message) {
    if (isEmpty(message)) fail("No message");
  }

  private void checkMessage(byte[] message) {
    if (message == null || message.length == 0) fail("No message");
  }

  private void groupCheck(String groupName) {
    if (isEmpty(groupName)) fail("groupName not defined");
  }

  private void groupCheck(String groupName, Channel channel) {
    groupCheck(groupName);
    checkValue(channel);
  }
}

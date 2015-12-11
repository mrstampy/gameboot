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
 *
 * Copyright (C) 2015 Burton Alexander
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

import static org.apache.commons.lang3.StringUtils.isEmpty;

import java.lang.invoke.MethodHandles;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.github.mrstampy.gameboot.metrics.MetricsHelper;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.ChannelGroupFuture;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.util.concurrent.ImmediateEventExecutor;

/**
 * Keeps up-to-date maps of key/key/Channel pairs and facilitates easy creation
 * of {@link ChannelGroup}s. Messages can be sent to individuals or groups.
 * 
 * @see FiberForkJoinNettyMessageHandler
 */
@Component
public class NettyConnectionRegistry {
  private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  /** Group key for ALL Netty connections. */
  public static final String ALL = "ALL";

  private static final String NETTY_CONNECTIONS = "Netty Connections";

  @Autowired
  private MetricsHelper helper;

  private Map<String, Channel> byString = new ConcurrentHashMap<>();

  private Map<Long, Channel> byLong = new ConcurrentHashMap<>();

  private Map<String, ChannelGroup> groups = new ConcurrentHashMap<>();

  private Map<Object, Channel> byCustomKey = new ConcurrentHashMap<>();

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
   * Returns true if the channel specified by the key exists.
   *
   * @param key
   *          the key specifying the channel
   * @return true, if successful
   */
  public boolean contains(String key) {
    check(key);
    return byString.containsKey(key);
  }

  /**
   * Returns true if the channel specified by the key exists.
   *
   * @param key
   *          the key specifying the channel
   * @return true, if successful
   */
  public boolean contains(Long key) {
    check(key);
    return byLong.containsKey(key);
  }

  /**
   * Returns true if the channel specified by the {@link Map}-keyable generic
   * type exists.
   *
   * @param <T>
   *          the generic type
   * @param key
   *          the key
   * @return true, if successful
   */
  public <T> boolean contains(T key) {
    check(key);
    return byCustomKey.containsKey(key);
  }

  /**
   * Returns true if the group specified by the groupKey exists.
   *
   * @param groupKey
   *          the group key
   * @return true, if successful
   */
  public boolean containsGroup(String groupKey) {
    groupCheck(groupKey);
    return groups.containsKey(groupKey);
  }

  /**
   * Gets the channel specified by the key.
   *
   * @param key
   *          the key specifying the channel
   * @return the channel
   */
  public Channel get(String key) {
    check(key);
    return byString.get(key);
  }

  /**
   * Gets the channel specified by the key.
   *
   * @param key
   *          the key specifying the channel
   * @return the channel
   */
  public Channel get(Long key) {
    check(key);
    return byLong.get(key);
  }

  /**
   * Gets the channel specified by the {@link Map}-keyable generic type.
   *
   * @param <T>
   *          the generic type
   * @param key
   *          the key
   * @return the channel
   */
  public <T> Channel get(T key) {
    check(key);
    return byCustomKey.get(key);
  }

  /**
   * Adds the channel mapped by key to the registry.
   *
   * @param key
   *          the key specifying the channel
   * @param channel
   *          the channel
   */
  public void put(Long key, Channel channel) {
    check(key);
    check(channel);
    byLong.put(key, channel);
    channel.closeFuture().addListener(f -> byLong.remove(key));
  }

  /**
   * Adds the channel mapped by key to the registry.
   *
   * @param key
   *          the key specifying the channel
   * @param channel
   *          the channel
   */
  public void put(String key, Channel channel) {
    check(key);
    check(channel);
    byString.put(key, channel);
    channel.closeFuture().addListener(f -> byString.remove(key));
  }

  /**
   * Adds the channel mapped by the {@link Map}-keyable generic type to the
   * registry.
   *
   * @param <T>
   *          the generic type
   * @param key
   *          the key
   * @param channel
   *          the channel
   */
  public <T> void put(T key, Channel channel) {
    check(key);
    check(channel);
    byCustomKey.put(key, channel);
    channel.closeFuture().addListener(f -> byCustomKey.remove(key));
  }

  /**
   * Send the message to the specified key.
   *
   * @param key
   *          the key specifying the channel
   * @param message
   *          the message
   */
  public void send(String key, String message) {
    check(key);

    Channel channel = byString.get(key);

    sendMessage(key, message, channel);
  }

  /**
   * Send the message to the specified key.
   *
   * @param key
   *          the key specifying the channel
   * @param message
   *          the message
   */
  public void send(Long key, String message) {
    check(key);

    Channel channel = byLong.get(key);

    sendMessage(key, message, channel);
  }

  /**
   * Sends the message to the specified {@link Map}-keyable channel.
   *
   * @param <T>
   *          the generic type
   * @param key
   *          the key
   * @param message
   *          the message
   */
  public <T> void send(T key, String message) {
    check(key);

    Channel channel = byCustomKey.get(key);

    sendMessage(key, message, channel);
  }

  /**
   * Puts the channel in the specified group, creating the group if it does not
   * yet exist.
   *
   * @param groupKey
   *          the group key
   * @param channel
   *          the channel
   */
  public void putInGroup(String groupKey, Channel channel) {
    groupCheck(groupKey, channel);

    ChannelGroup group = groups.get(groupKey);
    if (group == null) {
      group = new DefaultChannelGroup(groupKey, ImmediateEventExecutor.INSTANCE);
      groups.put(groupKey, group);
    }

    if (!group.contains(channel)) group.add(channel);
  }

  /**
   * Gets the group.
   *
   * @param groupKey
   *          the group key
   * @return the group
   */
  public ChannelGroup getGroup(String groupKey) {
    groupCheck(groupKey);
    return groups.get(groupKey);
  }

  /**
   * Removes the channel from the group specified by groupKey.
   *
   * @param groupKey
   *          the group key
   * @param channel
   *          the channel
   */
  public void removeFromGroup(String groupKey, Channel channel) {
    groupCheck(groupKey, channel);

    ChannelGroup group = groups.get(groupKey);
    if (group == null) return;

    group.remove(channel);
  }

  /**
   * Removes the channel from group specified by groupKey and key.
   *
   * @param groupKey
   *          the group key
   * @param key
   *          the key specifying the channel
   */
  public void removeFromGroup(String groupKey, String key) {
    check(key);
    Channel channel = get(key);
    if (channel != null) removeFromGroup(groupKey, channel);
  }

  /**
   * Removes the channel from group specified by groupKey and key.
   *
   * @param groupKey
   *          the group key
   * @param key
   *          the key specifying the channel
   */
  public void removeFromGroup(String groupKey, Long key) {
    check(key);
    Channel channel = get(key);
    if (channel != null) removeFromGroup(groupKey, channel);
  }

  /**
   * Removes the channel from group specified by groupKey and the {@link Map}
   * -keyable generic type.
   *
   * @param <T>
   *          the generic type
   * @param groupKey
   *          the group key
   * @param key
   *          the key
   */
  public <T> void removeFromGroup(String groupKey, T key) {
    check(key);
    Channel channel = get(key);
    if (channel != null) removeFromGroup(groupKey, channel);
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
   * @param groupKey
   *          the group key
   */
  public void removeGroup(String groupKey) {
    groupCheck(groupKey);
    if (!groups.containsKey(groupKey)) return;

    ChannelGroup group = groups.remove(groupKey);

    group.clear();
  }

  /**
   * Send the message to all connected channels.
   *
   * @param message
   *          the message
   */
  public void sendToAll(String message) {
    sendToGroup(ALL, message);
  }

  /**
   * Send the message to a specific group.
   *
   * @param groupKey
   *          the group key
   * @param message
   *          the message
   */
  public void sendToGroup(String groupKey, String message) {
    groupCheck(groupKey);
    if (!groups.containsKey(groupKey)) {
      log.warn("No group {} to send message {}", groupKey, message);
      return;
    }

    ChannelGroup group = groups.get(groupKey);

    ChannelGroupFuture f = group.writeAndFlush(message);
    f.addListener(e -> log((ChannelGroupFuture) e, groupKey, message));
  }

  /**
   * Removes the channel from key pairing.
   *
   * @param key
   *          the key specifying the channel
   * @return the channel
   */
  public Channel remove(String key) {
    check(key);
    return byString.remove(key);
  }

  /**
   * Removes the channel from the key pairing.
   *
   * @param key
   *          the key specifying the channel
   * @return the channel
   */
  public Channel remove(Long key) {
    check(key);
    return byLong.remove(key);
  }

  /**
   * Removes the channel from the {@link Map}-keyable generic type pairing.
   *
   * @param <T>
   *          the generic type
   * @param key
   *          the key
   * @return the channel
   */
  public <T> Channel remove(T key) {
    check(key);
    return byCustomKey.remove(key);
  }

  private <T> void sendMessage(T key, String message, Channel channel) {
    checkMessage(message);
    if (channel == null || !channel.isWritable()) {
      log.warn("Cannot send {} to {}", message, channel);
      return;
    }
    ChannelFuture f = channel.writeAndFlush(message);
    f.addListener(e -> log((ChannelFuture) e, key, message));
  }

  private void log(ChannelGroupFuture e, String groupKey, String message) {
    e.iterator().forEachRemaining(cf -> log(cf, groupKey, message));
  }

  private void log(ChannelFuture f, Object key, String message) {
    if (f.isSuccess()) {
      log.debug("Successful send of {} to {} on {}", message, key, f.channel());
    } else {
      log.error("Failed sending {} to {} on {}", message, key, f.channel(), f.cause());
    }
  }

  private int allConnected() {
    ChannelGroup group = getGroup(ALL);

    return group == null ? 0 : group.size();
  }

  private void check(String key) {
    if (isEmpty(key)) fail("key not specified");
  }

  private void check(Long key) {
    if (key == null || key <= 0) fail("Invalid key: " + key);
  }

  private void check(Channel channel) {
    if (channel == null) fail("Null channel");
  }

  private <T> void check(T key) {
    if (key == null) fail("Null key");
  }

  private void checkMessage(String message) {
    if (isEmpty(message)) fail("No message");
  }

  private void groupCheck(String groupKey) {
    if (isEmpty(groupKey)) fail("groupKey not defined");
  }

  private void groupCheck(String groupKey, Channel channel) {
    groupCheck(groupKey);
    check(channel);
  }

  private void fail(String message) {
    throw new IllegalArgumentException(message);
  }
}

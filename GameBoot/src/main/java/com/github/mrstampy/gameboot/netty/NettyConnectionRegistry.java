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
import com.github.mrstampy.gameboot.util.GameBootRegistry;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.ChannelGroupFuture;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.util.concurrent.ImmediateEventExecutor;

/**
 * Keeps up-to-date maps of key/{@link Channel} pairs and facilitates easy creation
 * of {@link ChannelGroup}s. Messages can be sent to individuals or groups.
 * 
 * @see AbstractGameBootNettyMessageHandler
 */
@Component
public class NettyConnectionRegistry extends GameBootRegistry<Channel> {
  private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  /** Group key for ALL Netty connections. */
  public static final String ALL = "ALL";

  private static final String NETTY_CONNECTIONS = "Netty Connections";

  @Autowired
  private MetricsHelper helper;

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
   * Adds the channel mapped by the key to the registry.
   *
   * @param key
   *          the key
   * @param channel
   *          the channel
   */
  public void put(Comparable<?> key, Channel channel) {
    super.put(key, channel);
    channel.closeFuture().addListener(f -> map.remove(key));
  }

  /**
   * Sends the message to the specified {@link Map}-keyable channel.
   *
   * @param key
   *          the key
   * @param message
   *          the message
   */
  public void send(Comparable<?> key, String message) {
    checkKey(key);

    Channel channel = get(key);

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
   * Removes the channel from group specified by groupKey and the key.
   *
   * @param groupKey
   *          the group key
   * @param key
   *          the key
   */
  public void removeFromGroup(String groupKey, Comparable<?> key) {
    checkKey(key);
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

  private void sendMessage(Comparable<?> key, String message, Channel channel) {
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

  private void checkMessage(String message) {
    if (isEmpty(message)) fail("No message");
  }

  private void groupCheck(String groupKey) {
    if (isEmpty(groupKey)) fail("groupKey not defined");
  }

  private void groupCheck(String groupKey, Channel channel) {
    groupCheck(groupKey);
    checkValue(channel);
  }
}

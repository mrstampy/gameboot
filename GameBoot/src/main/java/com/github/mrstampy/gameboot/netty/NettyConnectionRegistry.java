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
 * Keeps up-to-date maps of userName/sessionId/Channel pairs and facilitates
 * easy creation of {@link ChannelGroup}s. Messages can be sent to individuals
 * or groups.
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

  private Map<String, Channel> byUserName = new ConcurrentHashMap<>();

  private Map<Long, Channel> bySessionId = new ConcurrentHashMap<>();

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
   * Returns true if the channel specified by the userName exists.
   *
   * @param userName
   *          the user name
   * @return true, if successful
   */
  public boolean contains(String userName) {
    check(userName);
    return byUserName.containsKey(userName);
  }

  /**
   * Returns true if the channel specified by the sessionId exists.
   *
   * @param sessionId
   *          the session id
   * @return true, if successful
   */
  public boolean contains(Long sessionId) {
    check(sessionId);
    return bySessionId.containsKey(sessionId);
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
   * Gets the channel specified by the userName.
   *
   * @param userName
   *          the user name
   * @return the channel
   */
  public Channel get(String userName) {
    check(userName);
    return byUserName.get(userName);
  }

  /**
   * Gets the channel specified by the sessionId.
   *
   * @param sessionId
   *          the session id
   * @return the channel
   */
  public Channel get(Long sessionId) {
    check(sessionId);
    return bySessionId.get(sessionId);
  }

  /**
   * Adds the channel mapped by sessionId to the registry.
   *
   * @param sessionId
   *          the session id
   * @param channel
   *          the channel
   */
  public void put(Long sessionId, Channel channel) {
    check(sessionId);
    check(channel);
    bySessionId.put(sessionId, channel);
    channel.closeFuture().addListener(f -> bySessionId.remove(sessionId));
  }

  /**
   * Adds the channel mapped by userName to the registry.
   *
   * @param userName
   *          the user name
   * @param channel
   *          the channel
   */
  public void put(String userName, Channel channel) {
    check(userName);
    check(channel);
    byUserName.put(userName, channel);
    channel.closeFuture().addListener(f -> byUserName.remove(userName));
  }

  /**
   * Send the message to the specified userName.
   *
   * @param userName
   *          the user name
   * @param message
   *          the message
   */
  public void send(String userName, String message) {
    check(userName);
    if (isEmpty(message)) fail("No message");

    Channel channel = byUserName.get(userName);

    if (channel == null) fail("User " + userName + " not connected");

    ChannelFuture f = channel.writeAndFlush(message);
    f.addListener(e -> log((ChannelFuture) e, userName, message));
  }

  /**
   * Send the message to the specified sessionId.
   *
   * @param sessionId
   *          the session id
   * @param message
   *          the message
   */
  public void send(Long sessionId, String message) {
    check(sessionId);
    if (isEmpty(message)) fail("No message");

    Channel channel = bySessionId.get(sessionId);

    if (channel == null) fail("Session id " + sessionId + " not connected");

    ChannelFuture f = channel.writeAndFlush(message);
    f.addListener(e -> log((ChannelFuture) e, sessionId, message));
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
    if (!groups.containsKey(groupKey)) fail("No group for " + groupKey);

    ChannelGroup group = groups.get(groupKey);

    ChannelGroupFuture f = group.writeAndFlush(message);
    f.addListener(e -> log((ChannelFuture) e, groupKey, message));
  }

  /**
   * Removes the channel from userName pairing.
   *
   * @param userName
   *          the user name
   * @return the channel
   */
  public Channel remove(String userName) {
    check(userName);
    return byUserName.remove(userName);
  }

  /**
   * Removes the channel from the sessionId pairing.
   *
   * @param sessionId
   *          the session id
   * @return the channel
   */
  public Channel remove(Long sessionId) {
    check(sessionId);
    return bySessionId.remove(sessionId);
  }

  private void log(ChannelFuture f, Object key, String message) {
    if (f.isSuccess()) {
      log.debug("Successful send of {} to {}", message, key);
    } else {
      log.error("Failed sending {} to {}", message, key, f.cause());
    }
  }

  private int allConnected() {
    ChannelGroup group = getGroup(ALL);

    return group == null ? 0 : group.size();
  }

  private void check(String userName) {
    if (isEmpty(userName)) fail("userName not specified");
  }

  private void check(Long sessionId) {
    if (sessionId == null || sessionId <= 0) fail("Invalid sessionId: " + sessionId);
  }

  private void check(Channel channel) {
    if (channel == null) fail("Null channel");
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

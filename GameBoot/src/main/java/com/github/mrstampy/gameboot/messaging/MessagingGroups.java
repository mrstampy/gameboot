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
package com.github.mrstampy.gameboot.messaging;

import static org.apache.commons.lang3.StringUtils.isEmpty;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import com.github.mrstampy.gameboot.netty.NettyConnectionRegistry;
import com.github.mrstampy.gameboot.systemid.SystemIdKey;
import com.github.mrstampy.gameboot.websocket.WebSocketSessionRegistry;

import io.netty.channel.Channel;
import io.netty.channel.group.ChannelMatcher;

/**
 * MessagingGroups facilitates sending messages to a group of connections,
 * either web sockets, Netty connections or a mix of the two.
 */
@Component
public class MessagingGroups {

  /** Group key for ALL connections. */
  public static final String ALL = "ALL";

  private static final ChannelMatcher NOOP_MATCHER = new ChannelMatcher() {

    @Override
    public boolean matches(Channel channel) {
      return true;
    }
  };

  @Autowired
  private NettyConnectionRegistry nettyRegistry;

  @Autowired
  private WebSocketSessionRegistry webSocketRegistry;

  /**
   * Adds the to group.
   *
   * @param groupName
   *          the group name
   * @param channel
   *          the channel
   */
  public void addToGroup(String groupName, Channel channel) {
    nettyRegistry.putInGroup(groupName, channel);
  }

  /**
   * Adds the to group.
   *
   * @param groupName
   *          the group name
   * @param session
   *          the session
   */
  public void addToGroup(String groupName, WebSocketSession session) {
    webSocketRegistry.putInGroup(groupName, session);
  }

  /**
   * Removes the from group.
   *
   * @param groupName
   *          the group name
   * @param channel
   *          the channel
   */
  public void removeFromGroup(String groupName, Channel channel) {
    nettyRegistry.removeFromGroup(groupName, channel);
  }

  /**
   * Removes the from group.
   *
   * @param groupName
   *          the group name
   * @param session
   *          the session
   */
  public void removeFromGroup(String groupName, WebSocketSession session) {
    webSocketRegistry.removeFromGroup(groupName, session);
  }

  /**
   * Send to all.
   *
   * @param message
   *          the message
   * @param except
   *          the except
   */
  public void sendToAll(String message, SystemIdKey... except) {
    sendMessage(NettyConnectionRegistry.ALL, message, except);
  }

  /**
   * Send to all.
   *
   * @param message
   *          the message
   * @param except
   *          the except
   */
  public void sendToAll(byte[] message, SystemIdKey... except) {
    sendMessage(NettyConnectionRegistry.ALL, message, except);
  }

  /**
   * Send message.
   *
   * @param groupName
   *          the group name
   * @param message
   *          the message
   * @param except
   *          the except
   */
  public void sendMessage(String groupName, String message, SystemIdKey... except) {
    groupNameCheck(groupName);
    if (isEmpty(message)) throw new IllegalArgumentException("No message");

    webSocketRegistry.sendToGroup(groupName, message, except);
    sendToNetty(groupName, message, except);
  }

  /**
   * Send message.
   *
   * @param groupName
   *          the group name
   * @param message
   *          the message
   * @param except
   *          the except
   */
  public void sendMessage(String groupName, byte[] message, SystemIdKey... except) {
    groupNameCheck(groupName);
    if (message == null || message.length == 0) throw new IllegalArgumentException("No message");

    webSocketRegistry.sendToGroup(groupName, message, except);
    sendToNetty(groupName, message, except);
  }

  private void sendToNetty(String groupName, byte[] message, SystemIdKey... except) {
    if (!nettyRegistry.containsGroup(groupName)) return;

    ChannelMatcher exceptions = createMatcher(except);

    nettyRegistry.sendToGroup(groupName, message, exceptions);
  }

  private void sendToNetty(String groupName, String message, SystemIdKey[] except) {
    if (!nettyRegistry.containsGroup(groupName)) return;

    ChannelMatcher exceptions = createMatcher(except);

    nettyRegistry.sendToGroup(groupName, message, exceptions);
  }

  private ChannelMatcher createMatcher(SystemIdKey... except) {
    if (except == null || except.length == 0) return NOOP_MATCHER;

    List<Channel> exceptions = new ArrayList<>();
    for (SystemIdKey key : except) {
      Channel c = nettyRegistry.get(key);
      if (c != null) exceptions.add(c);
    }

    return exceptions.isEmpty() ? NOOP_MATCHER : new ChannelMatcher() {

      @Override
      public boolean matches(Channel channel) {
        return !exceptions.contains(channel);
      }
    };
  }

  private void groupNameCheck(String groupName) {
    if (isEmpty(groupName)) throw new NullPointerException("No groupName");
  }
}

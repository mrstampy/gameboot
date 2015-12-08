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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

import io.netty.channel.Channel;

/**
 * Keeps up-to-date maps of userName/sessionId/Channel pairs.
 * 
 * @see GameBootNettyMessageHandler
 */
@Component
public class NettyConnectionRegistry {

  private Map<String, Channel> byUserName = new ConcurrentHashMap<>();

  private Map<Long, Channel> bySessionId = new ConcurrentHashMap<>();

  /**
   * Contains.
   *
   * @param userName
   *          the user name
   * @return true, if successful
   */
  public boolean contains(String userName) {
    return byUserName.containsKey(userName);
  }

  /**
   * Contains.
   *
   * @param sessionId
   *          the session id
   * @return true, if successful
   */
  public boolean contains(Long sessionId) {
    return bySessionId.containsKey(sessionId);
  }

  /**
   * Gets the channel.
   *
   * @param userName
   *          the user name
   * @return the channel
   */
  public Channel get(String userName) {
    return byUserName.get(userName);
  }

  /**
   * Gets the channel.
   *
   * @param sessionId
   *          the session id
   * @return the channel
   */
  public Channel get(Long sessionId) {
    return bySessionId.get(sessionId);
  }

  /**
   * Put.
   *
   * @param sessionId
   *          the session id
   * @param channel
   *          the channel
   */
  public void put(Long sessionId, Channel channel) {
    bySessionId.put(sessionId, channel);
  }

  /**
   * Put.
   *
   * @param userName
   *          the user name
   * @param channel
   *          the channel
   */
  public void put(String userName, Channel channel) {
    byUserName.put(userName, channel);
  }

  /**
   * Removes the.
   *
   * @param userName
   *          the user name
   */
  public void remove(String userName) {
    byUserName.remove(userName);
  }

  /**
   * Removes the.
   *
   * @param sessionId
   *          the session id
   */
  public void remove(Long sessionId) {
    bySessionId.remove(sessionId);
  }
}

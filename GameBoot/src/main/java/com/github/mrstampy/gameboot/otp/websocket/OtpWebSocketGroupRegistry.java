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
package com.github.mrstampy.gameboot.otp.websocket;

import static org.apache.commons.lang3.StringUtils.isEmpty;

import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import com.github.mrstampy.gameboot.metrics.MetricsHelper;
import com.github.mrstampy.gameboot.netty.NettyConnectionRegistry;
import com.github.mrstampy.gameboot.otp.KeyRegistry;
import com.github.mrstampy.gameboot.otp.OneTimePad;
import com.github.mrstampy.gameboot.otp.OtpConfiguration;
import com.github.mrstampy.gameboot.systemid.SystemIdKey;
import com.github.mrstampy.gameboot.util.registry.AbstractRegistryKey;
import com.github.mrstampy.gameboot.util.registry.GameBootRegistry;
import com.github.mrstampy.gameboot.websocket.WebSocketSessionRegistry;

/**
 * The Class OtpWebSocketGroupRegistry facilitates communication between
 * {@link OtpClearWebSocketProcessor} connections, encrypting the message for
 * each connection as required.
 * 
 * @see WebSocketSessionRegistry
 */
@Component
@Profile(OtpConfiguration.OTP_PROFILE)
public class OtpWebSocketGroupRegistry extends GameBootRegistry<WebSocketSession> {
  private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private static final String OTP_WEB_SOCKET_CONNECTIONS = "OTP Web Socket Connections";

  @Autowired
  private WebSocketSessionRegistry registry;

  @Autowired
  private KeyRegistry keyRegistry;

  @Autowired
  private OneTimePad pad;

  @Autowired
  private MetricsHelper helper;

  /**
   * Post construct.
   *
   * @throws Exception
   *           the exception
   */
  @PostConstruct
  public void postConstruct() throws Exception {
    helper.gauge(() -> size(), OTP_WEB_SOCKET_CONNECTIONS, getClass(), "web", "socket", "connections");
  }

  /**
   * Put in group, delegating to the {@link NettyConnectionRegistry}.
   *
   * @param groupName
   *          the group name
   * @param channel
   *          the channel
   */
  public void putInGroup(String groupName, WebSocketSession channel) {
    registry.putInGroup(groupName, channel);
  }

  /**
   * Removes the from group.
   *
   * @param groupName
   *          the group name
   * @param channel
   *          the channel
   */
  public void removeFromGroup(String groupName, WebSocketSession channel) {
    registry.removeFromGroup(groupName, channel);
  }

  /**
   * Send.
   *
   * @param key
   *          the key
   * @param message
   *          the message
   * @param listeners
   *          the listeners
   * @throws Exception
   *           the exception
   */
  public void send(AbstractRegistryKey<?> key, byte[] message) throws Exception {
    SystemIdKey systemId = (key instanceof SystemIdKey) ? (SystemIdKey) key
        : getKeyForWebSocketSession(registry.get(key));

    byte[] otp = systemId == null ? null : keyRegistry.get(systemId);

    if (otp == null) {
      registry.send(key, message);
    } else {
      byte[] converted = pad.convert(otp, message);
      registry.send(key, converted);
    }
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
    List<WebSocketSession> group = getGroup(groupName);

    if (group == null) return;

    group.forEach(c -> {
      try {
        AbstractRegistryKey<?> systemId = getKeyForWebSocketSession(c);
        if (!excepted(systemId, except)) send(systemId, message);
      } catch (Exception e) {
        log.error("Unexpected exception sending message to {}", c, e);
      }
    });

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
    if (!messageCheck(message)) return;

    sendToGroup(groupName, message.getBytes(), except);
  }

  /**
   * Send to group.
   *
   * @param groupName
   *          the group name
   * @param message
   *          the message
   * @param listeners
   *          the listeners
   */
  public void sendToGroup(String groupName, String message) {
    if (!messageCheck(message)) return;

    sendToGroup(groupName, message.getBytes());
  }

  /**
   * Send.
   *
   * @param key
   *          the key
   * @param message
   *          the message
   * @param listeners
   *          the listeners
   * @throws Exception
   *           the exception
   */
  public void send(AbstractRegistryKey<?> key, String message) throws Exception {
    if (!messageCheck(message)) return;

    send(key, message.getBytes());
  }

  /**
   * Gets the group.
   *
   * @param groupName
   *          the group name
   * @return the group
   */
  public List<WebSocketSession> getGroup(String groupName) {
    List<WebSocketSession> group = registry.getGroup(groupName);

    if (group == null) {
      log.warn("No group {}", groupName);
      return null;
    }

    return group;
  }

  /**
   * Contains group.
   *
   * @param groupName
   *          the group name
   * @return true, if successful
   */
  public boolean containsGroup(String groupName) {
    return registry.containsGroup(groupName);
  }

  /**
   * Removes the group.
   *
   * @param groupName
   *          the group name
   */
  public void removeGroup(String groupName) {
    registry.removeGroup(groupName);
  }

  private boolean messageCheck(String message) {
    if (isEmpty(message)) {
      log.warn("No message");
      return false;
    }

    return true;
  }

  private boolean excepted(AbstractRegistryKey<?> systemId, SystemIdKey[] except) {
    if (except == null || except.length == 0) return false;

    for (SystemIdKey key : except) {
      if (key.equals(systemId)) return true;
    }

    return false;
  }

  private SystemIdKey getKeyForWebSocketSession(WebSocketSession c) {
    Optional<Entry<AbstractRegistryKey<?>, WebSocketSession>> o = registry.getKeysForValue(c).stream()
        .filter(e -> e.getKey() instanceof SystemIdKey).findFirst();

    return o.isPresent() ? (SystemIdKey) o.get().getKey() : null;
  }
}

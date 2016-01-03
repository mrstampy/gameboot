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
package com.github.mrstampy.gameboot.websocket;

import static com.github.mrstampy.gameboot.messaging.MessagingGroups.ALL;
import static org.apache.commons.lang3.StringUtils.isEmpty;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import com.github.mrstampy.gameboot.metrics.MetricsHelper;
import com.github.mrstampy.gameboot.systemid.SystemIdKey;
import com.github.mrstampy.gameboot.util.registry.AbstractRegistryKey;
import com.github.mrstampy.gameboot.util.registry.GameBootRegistry;
import com.github.mrstampy.gameboot.util.registry.RegistryCleanerListener;

/**
 * The Class WebSocketSessionRegistry.
 */
@Component
public class WebSocketSessionRegistry extends GameBootRegistry<WebSocketSession> implements RegistryCleanerListener {
  private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private static final String REGISTRY_SIZE = "Web Socket Connections";

  @Autowired
  private MetricsHelper helper;

  private Map<String, List<WebSocketSession>> sessionGroups = new ConcurrentHashMap<>();

  private Map<SystemIdKey, WebSocketSession> activeInGroups = new ConcurrentHashMap<>();

  private ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock();
  private ReadLock rLock = rwLock.readLock();
  private WriteLock wLock = rwLock.writeLock();

  /**
   * Post construct.
   *
   * @throws Exception
   *           the exception
   */
  @PostConstruct
  public void postConstruct() throws Exception {
    helper.gauge(() -> allConnected(), REGISTRY_SIZE, getClass(), "web", "socket", "connections");
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * com.github.mrstampy.gameboot.util.registry.RegistryCleanerListener#cleanup(
   * com.github.mrstampy.gameboot.util.registry.AbstractRegistryKey)
   */
  @Override
  public void cleanup(AbstractRegistryKey<?> key) {
    if (!(key instanceof SystemIdKey)) return;

    WebSocketSession session = activeInGroups.remove(key);
    if (session == null) return;

    sessionGroups.entrySet().forEach(e -> removeFromGroups(e, session));
  }

  /**
   * Put in all.
   *
   * @param key
   *          the key
   * @param session
   *          the session
   */
  public void putInAll(SystemIdKey key, WebSocketSession session) {
    putInGroup(ALL, session);
    activeInGroups.put(key, session);
  }

  /**
   * Adds the session to the group.
   *
   * @param groupName
   *          the group name
   * @param session
   *          the session
   */
  public void putInGroup(String groupName, WebSocketSession session) {
    groupAndSessionCheck(groupName, session);

    List<WebSocketSession> list = getSessionsForGroup(groupName);

    rLock.lock();
    try {
      if (list.contains(session)) return;
    } finally {
      rLock.unlock();
    }

    wLock.lock();
    try {
      list.add(session);
    } finally {
      wLock.unlock();
    }

    addToActiveInGroups(session);
  }

  /**
   * Removes the session from the group.
   *
   * @param groupName
   *          the group name
   * @param session
   *          the session
   */
  public void removeFromGroup(String groupName, WebSocketSession session) {
    groupAndSessionCheck(groupName, session);

    List<WebSocketSession> list = sessionGroups.get(groupName);

    rLock.lock();
    try {
      if (list == null || !list.contains(session)) return;
    } finally {
      rLock.unlock();
    }

    wLock.lock();
    try {
      list.remove(session);
    } finally {
      wLock.unlock();
    }

    rLock.lock();
    try {
      if (!list.isEmpty()) return;
    } finally {
      rLock.unlock();
    }

    sessionGroups.remove(groupName);
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
    sendToGroup(ALL, message, except);
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
    sendToGroup(ALL, message, except);
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
    List<WebSocketSession> list = sessionGroups.get(groupName);
    List<WebSocketSession> toSend = null;

    rLock.lock();
    try {
      if (list == null || list.isEmpty()) return;

      List<WebSocketSession> exceptions = getExceptions(except);
      toSend = list.stream().filter(wss -> !exceptions.contains(wss)).collect(Collectors.toList());
    } finally {
      rLock.unlock();
    }

    toSend.forEach(wss -> sendMessage(groupName, wss, message));
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
    List<WebSocketSession> list = sessionGroups.get(groupName);
    List<WebSocketSession> toSend = null;

    rLock.lock();
    try {
      if (list == null || list.isEmpty()) return;

      List<WebSocketSession> exceptions = getExceptions(except);

      toSend = list.stream().filter(wss -> !exceptions.contains(wss)).collect(Collectors.toList());
    } finally {
      rLock.unlock();
    }

    toSend.forEach(wss -> sendMessage(groupName, wss, message));
  }

  private void sendMessage(String groupName, WebSocketSession wss, byte[] message) {
    if (!wss.isOpen()) {
      removeFromGroup(groupName, wss);
      return;
    }

    BinaryMessage bm = new BinaryMessage(message);
    try {
      wss.sendMessage(bm);
      log.trace("Sent message to web socket session {} in group {}", wss.getId(), groupName);
    } catch (IOException e) {
      log.error("Unexpected exception sending message to web socket session {}", wss.getId(), e);
    }
  }

  private void sendMessage(String groupName, WebSocketSession wss, String message) {
    if (!wss.isOpen()) {
      removeFromGroup(groupName, wss);
      return;
    }

    TextMessage bm = new TextMessage(message);
    try {
      wss.sendMessage(bm);
      log.trace("Sent message to web socket session {} in group {}", wss.getId(), groupName);
    } catch (IOException e) {
      log.error("Unexpected exception sending message to web socket session {}", wss.getId(), e);
    }
  }

  @SuppressWarnings("unchecked")
  private List<WebSocketSession> getExceptions(SystemIdKey... except) {
    if (except == null || except.length == 0) return Collections.EMPTY_LIST;

    List<WebSocketSession> exceptions = new ArrayList<>();
    for (SystemIdKey key : except) {
      WebSocketSession session = get(key);
      if (session != null) exceptions.add(session);
    }

    return exceptions;
  }

  private List<WebSocketSession> getSessionsForGroup(String groupName) {
    List<WebSocketSession> list = sessionGroups.get(groupName);

    if (list == null) {
      list = new ArrayList<>();
      sessionGroups.put(groupName, list);
    }

    return list;
  }

  private void addToActiveInGroups(WebSocketSession session) {
    Optional<Entry<AbstractRegistryKey<?>, WebSocketSession>> o = getKeysForValue(session).stream()
        .filter(k -> k instanceof SystemIdKey).findFirst();

    if (!o.isPresent()) return;

    SystemIdKey key = (SystemIdKey) o.get().getKey();

    if (!activeInGroups.containsKey(key)) activeInGroups.put(key, session);
  }

  private void removeFromGroups(Entry<String, List<WebSocketSession>> e, WebSocketSession session) {
    List<WebSocketSession> list = e.getValue();

    rLock.lock();
    try {
      if (!list.contains(session)) return;
    } finally {
      rLock.unlock();
    }

    wLock.lock();
    try {
      list.remove(session);
    } finally {
      wLock.unlock();
    }
  }

  private int allConnected() {
    List<WebSocketSession> group = sessionGroups.get(ALL);

    rLock.lock();
    try {
      return group == null ? 0 : group.size();
    } finally {
      rLock.unlock();
    }
  }

  private void groupAndSessionCheck(String groupName, WebSocketSession session) {
    groupNameCheck(groupName);
    if (session == null) throw new NullPointerException("No web socket session");
  }

  private void groupNameCheck(String groupName) {
    if (isEmpty(groupName)) throw new NullPointerException("No groupName");
  }

}

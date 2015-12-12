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
package com.github.mrstampy.gameboot.otp.websocket;

import static org.apache.commons.lang3.StringUtils.isEmpty;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.Map.Entry;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.WebSocketSession;

import com.github.mrstampy.gameboot.util.GameBootRegistry;

import io.netty.handler.ssl.SslHandler;

/**
 * The Class OtpRegistry keeps an {@link OtpConnections} pairing to be able to
 * send OTP keys thru the {@link OtpConnections#getEncryptedWebSocketSession()}
 * and OTP-encrypted messages in the
 * {@link OtpConnections#getClearWebSocketSession()}.
 */
@Component
public class OtpRegistry extends GameBootRegistry<OtpConnections> {

  private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  /**
   * Sets the clear session.
   *
   * @param key
   *          the key
   * @param session
   *          the session
   * @throws IOException
   *           Signals that an I/O exception has occurred.
   */
  public void setClearWebSocketSession(Comparable<?> key, WebSocketSession session) throws IOException {
    OtpConnections connections = getContainer(key, session);

    if (connections.isClearWebSocketSessionActive()) connections.getClearWebSocketSession().close();

    log.debug("Setting clear session {} for key {}", session, key);

    connections.setClearWebSocketSession(session);
  }

  /**
   * Sets the encrypted session.<br>
   * <br>
   * Attempting to add a session as encrypted, not containing an instance of
   * {@link SslHandler} in the pipeline will cause an
   * {@link IllegalArgumentException} to be thrown.
   *
   * @param key
   *          the key
   * @param session
   *          the session
   * @throws IOException
   *           Signals that an I/O exception has occurred.
   */
  public void setEncryptedWebSocketSession(Comparable<?> key, WebSocketSession session) throws IOException {
    OtpConnections connections = getContainer(key, session);

    if (connections.isEncryptedWebSocketSessionActive()) connections.getEncryptedWebSocketSession().close();

    log.debug("Setting encrypted session {} for key {}", session, key);

    connections.setEncryptedWebSocketSession(session);
  }

  /**
   * Send new otp key.
   *
   * @param key
   *          the key
   * @param otpKey
   *          the otp key
   * @return true, if successful
   * @throws IOException
   *           Signals that an I/O exception has occurred.
   */
  public boolean sendNewOtpKey(Comparable<?> key, byte[] otpKey) throws IOException {
    if (otpKey == null || otpKey.length == 0) fail("No OTP key");

    OtpConnections connections = getOrLog(key);

    if (connections == null || !connections.isEncryptedWebSocketSessionActive()) {
      log.warn("No encrypted session, cannot send new OTP key for {}", key);
      return false;
    }

    return send(otpKey, connections.getEncryptedWebSocketSession());
  }

  /**
   * Send clear message.
   *
   * @param key
   *          the key
   * @param message
   *          the message
   * @return true, if successful
   * @throws IOException
   *           Signals that an I/O exception has occurred.
   */
  public boolean sendClearMessage(Comparable<?> key, String message) throws IOException {
    if (isEmpty(message)) fail("No message");

    return sendClearImpl(key, message);
  }

  /**
   * Send clear message.
   *
   * @param key
   *          the key
   * @param message
   *          the message
   * @return true, if successful
   * @throws IOException
   *           Signals that an I/O exception has occurred.
   */
  public boolean sendClearMessage(Comparable<?> key, byte[] message) throws IOException {
    if (message == null || message.length == 0) fail("No message");

    return sendClearImpl(key, message);
  }

  /**
   * Checks if both the encrypted and clear sessions are active.
   *
   * @param key
   *          the key
   * @return true, if is active
   */
  public boolean isActive(Comparable<?> key) {
    checkKey(key);

    OtpConnections connections = get(key);

    return connections == null ? false : connections.isActive();
  }

  /**
   * Checks if the clear session is active.
   *
   * @param key
   *          the key
   * @return true, if is active
   */
  public boolean isClearWebSocketSessionActive(Comparable<?> key) {
    checkKey(key);

    OtpConnections connections = get(key);

    return connections == null ? false : connections.isClearWebSocketSessionActive();
  }

  /**
   * Checks if the encrypted session is active.
   *
   * @param key
   *          the key
   * @return true, if is active
   */
  public boolean isEncryptedWebSocketSessionActive(Comparable<?> key) {
    checkKey(key);

    OtpConnections connections = get(key);

    return connections == null ? false : connections.isEncryptedWebSocketSessionActive();
  }

  /**
   * Gets the key by clear session, null if not found.
   *
   * @param clearWebSocketSession
   *          the clear session
   * @return the key by clear session
   */
  public Comparable<?> getKeyByClearWebSocketSession(WebSocketSession clearWebSocketSession) {
    checkSession(clearWebSocketSession);

    Optional<Entry<Comparable<?>, OtpConnections>> e = map.entrySet().stream()
        .filter(c -> clearWebSocketSession.equals(c.getValue().getClearWebSocketSession())).findFirst();

    return e.isPresent() ? e.get().getKey() : null;
  }

  /**
   * Gets the key by encrypted session, null if not found.
   *
   * @param encryptedWebSocketSession
   *          the encrypted session
   * @return the key by encrypted session
   */
  public Comparable<?> getKeyByEncryptedWebSocketSession(WebSocketSession encryptedWebSocketSession) {
    checkSession(encryptedWebSocketSession);

    Optional<Entry<Comparable<?>, OtpConnections>> e = map.entrySet().stream()
        .filter(c -> encryptedWebSocketSession.equals(c.getValue().getEncryptedWebSocketSession())).findFirst();

    return e.isPresent() ? e.get().getKey() : null;
  }

  private <T> boolean sendClearImpl(Comparable<?> key, T message) throws IOException {
    OtpConnections connections = getOrLog(key);

    if (connections == null || !connections.isClearWebSocketSessionActive()) {
      log.warn("No clear session, cannot send message for {}", key);
      return false;
    }

    return send(message, connections.getClearWebSocketSession());
  }

  private <T> boolean send(T message, WebSocketSession session) throws IOException {
    checkSession(session);

    BinaryMessage bm = null;
    if (message instanceof String) {
      bm = new BinaryMessage(((String) message).getBytes());
    } else {
      bm = new BinaryMessage((byte[]) message);
    }

    session.sendMessage(bm);

    return true;
  }

  private OtpConnections getOrLog(Comparable<?> key) {
    checkKey(key);
    OtpConnections connections = get(key);
    if (connections == null) log.warn("No OTP connections for {}", key);

    return connections;
  }

  private OtpConnections getContainer(Comparable<?> key, WebSocketSession session) {
    checkKey(key);
    checkSession(session);

    OtpConnections connections = getOrInit(key);

    return connections;
  }

  private OtpConnections getOrInit(Comparable<?> key) {
    OtpConnections connections = get(key);
    if (connections == null) {
      connections = new OtpConnections();
      put(key, connections);
    }
    return connections;
  }

  private void checkSession(WebSocketSession session) {
    if (session == null || !session.isOpen()) fail("No session");
  }
}

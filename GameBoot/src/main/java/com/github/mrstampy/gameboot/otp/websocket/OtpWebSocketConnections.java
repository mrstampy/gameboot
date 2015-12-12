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

import org.springframework.web.socket.WebSocketSession;

import com.github.mrstampy.gameboot.otp.KeyRegistry;

/**
 * The Class OtpConnections.
 */
public class OtpWebSocketConnections {

  private WebSocketSession encryptedWebSocketSession;

  private WebSocketSession clearWebSocketSession;

  /**
   * Gets the encrypted channel.
   *
   * @return the encrypted channel
   */
  public WebSocketSession getEncryptedWebSocketSession() {
    return encryptedWebSocketSession;
  }

  /**
   * Sets the encrypted channel.
   *
   * @param encryptedWebSocketSession
   *          the new encrypted channel
   */
  public void setEncryptedWebSocketSession(WebSocketSession encryptedWebSocketSession) {
    this.encryptedWebSocketSession = encryptedWebSocketSession;
  }

  /**
   * Gets the clear channel.
   *
   * @return the clear channel
   */
  public WebSocketSession getClearWebSocketSession() {
    return clearWebSocketSession;
  }

  /**
   * Sets the clear channel.
   *
   * @param clearWebSocketSession
   *          the new clear channel
   */
  public void setClearWebSocketSession(WebSocketSession clearWebSocketSession) {
    this.clearWebSocketSession = clearWebSocketSession;
  }

  /**
   * Checks if both {@link #isClearWebSocketSessionActive()} &&
   * {@link #isEncryptedWebSocketSessionActive()}.
   *
   * @return true, if is active
   */
  public boolean isActive() {
    return isClearWebSocketSessionActive() && isEncryptedWebSocketSessionActive();
  }

  /**
   * Checks if the clear channel is active.
   *
   * @return true, if is clear channel active
   */
  public boolean isClearWebSocketSessionActive() {
    return clearWebSocketSession != null && clearWebSocketSession.isOpen();
  }

  /**
   * Checks if the encrypted channel is active.
   *
   * @return true, if is encrypted channel active
   */
  public boolean isEncryptedWebSocketSessionActive() {
    return encryptedWebSocketSession != null && encryptedWebSocketSession.isOpen();
  }

  /**
   * Creates the otp key used by an {@link OtpWebSocketHandler} instance to
   * check the {@link KeyRegistry} for the OTP key and in the
   * {@link OtpWebSocketRegistry#setClearWebSocketSession(Comparable, WebSocketSession)}
   * via {@link #getClearWebSocketSession()#remoteAddress()#toString()} or null
   * if not {@link #isClearWebSocketSessionActive()}.
   *
   * @return the otp key
   * @see OtpWebSocketHandler
   */
  public String createOtpKey() {
    if (!isClearWebSocketSessionActive()) return null;

    return getClearWebSocketSession().getRemoteAddress().toString();
  }

}

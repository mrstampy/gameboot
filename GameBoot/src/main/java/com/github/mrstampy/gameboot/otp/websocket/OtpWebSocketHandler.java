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

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.concurrent.ExecutorService;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;

import com.github.mrstampy.gameboot.concurrent.GameBootConcurrentConfiguration;
import com.github.mrstampy.gameboot.otp.KeyRegistry;
import com.github.mrstampy.gameboot.otp.OneTimePad;
import com.github.mrstampy.gameboot.websocket.AbstractGameBootWebSocketHandler;

/**
 * The Class OtpHandler.
 */
@Component
public class OtpWebSocketHandler extends AbstractGameBootWebSocketHandler {

  private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Autowired
  private KeyRegistry keyRegistry;

  @Autowired
  private OtpWebSocketRegistry otpRegistry;

  @Autowired
  private OneTimePad pad;

  @Autowired
  @Qualifier(GameBootConcurrentConfiguration.GAME_BOOT_EXECUTOR)
  private ExecutorService svc;

  /**
   * Post construct.
   *
   * @throws Exception
   *           the exception
   */
  @PostConstruct
  public void postConstruct() throws Exception {
    super.postConstruct();
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.springframework.web.socket.handler.AbstractWebSocketHandler#
   * afterConnectionEstablished(org.springframework.web.socket.WebSocketSession)
   */
  @Override
  public void afterConnectionEstablished(WebSocketSession session) throws Exception {
    super.afterConnectionEstablished(session);
    if (!otpRegistry.contains(session.getId())) otpRegistry.setClearWebSocketSession(session.getId(), session);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.springframework.web.socket.handler.AbstractWebSocketHandler#
   * afterConnectionClosed(org.springframework.web.socket.WebSocketSession,
   * org.springframework.web.socket.CloseStatus)
   */
  @Override
  public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
    otpRegistry.remove(session.getId());
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * com.github.mrstampy.gameboot.websocket.AbstractGameBootWebSocketHandler#
   * handleBinaryMessageImpl(org.springframework.web.socket.WebSocketSession,
   * byte[])
   */
  @Override
  protected void handleBinaryMessageImpl(WebSocketSession session, byte[] message) {
    svc.execute(() -> {
      try {
        processForBinary(session, message);
      } catch (Exception e) {
        log.error("Unexpected exception", e);
      }
    });
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * com.github.mrstampy.gameboot.websocket.AbstractGameBootWebSocketHandler#
   * processForBinary(org.springframework.web.socket.WebSocketSession, byte[])
   */
  protected void processForBinary(WebSocketSession session, byte[] message) throws Exception {
    byte[] key = keyRegistry.get(session.getId());
    byte[] msg = otp(key, session, message);

    String response = process(session, new String(msg));
    if (response == null) return;

    byte[] r = otp(key, session, response.getBytes());

    BinaryMessage m = new BinaryMessage(r);
    session.sendMessage(m);
  }

  private byte[] otp(byte[] key, WebSocketSession session, byte[] message) throws Exception {
    return key == null ? message : pad.convert(key, message);
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * com.github.mrstampy.gameboot.websocket.AbstractGameBootWebSocketHandler#
   * handleTextMessageImpl(org.springframework.web.socket.WebSocketSession,
   * java.lang.String)
   */
  @Override
  protected void handleTextMessageImpl(WebSocketSession session, String message) throws Exception {
    try {
      session.close(CloseStatus.NOT_ACCEPTABLE.withReason("Text messages not supported"));
    } catch (IOException e) {
      // ignore
    }
  }

}

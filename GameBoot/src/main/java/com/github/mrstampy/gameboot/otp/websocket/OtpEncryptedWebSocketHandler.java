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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketSession;

import com.github.mrstampy.gameboot.concurrent.GameBootConcurrentConfiguration;
import com.github.mrstampy.gameboot.controller.GameBootMessageController;
import com.github.mrstampy.gameboot.exception.GameBootException;
import com.github.mrstampy.gameboot.exception.GameBootRuntimeException;
import com.github.mrstampy.gameboot.messages.AbstractGameBootMessage;
import com.github.mrstampy.gameboot.messages.GameBootMessageConverter;
import com.github.mrstampy.gameboot.messages.Response;
import com.github.mrstampy.gameboot.otp.OtpConfiguration;
import com.github.mrstampy.gameboot.otp.messages.OtpKeyRequest;
import com.github.mrstampy.gameboot.otp.messages.OtpKeyRequest.KeyFunction;
import com.github.mrstampy.gameboot.otp.messages.OtpMessage;
import com.github.mrstampy.gameboot.otp.messages.OtpNewKeyAck;
import com.github.mrstampy.gameboot.util.GameBootUtils;
import com.github.mrstampy.gameboot.websocket.AbstractGameBootWebSocketHandler;
import com.github.mrstampy.gameboot.websocket.WebSocketSessionRegistry;

/**
 * The Class OtpEncryptedWebSocketHandler is the {@link WebSocketHandler}
 * intended to process {@link OtpMessage}s. The connection must be encrypted
 * sending byte arrays as messages and must originate from the same host as the
 * connection containing the {@link OtpClearWebSocketHandler}. Should these
 * conditions fail the connection will be terminated.<br>
 * <br>
 * 
 * The client connects to the socket containing this handler and sends a message
 * of type {@link OtpKeyRequest}. The {@link OtpKeyRequest#getSystemId()} value
 * will have been set in the client as the value obtained from the clear
 * connection containing the {@link OtpClearWebSocketHandler} in the pipeline.
 * <br>
 * <br>
 * 
 * If the key generation is successful a {@link Response} object is returned
 * containing the key as the only element of the {@link Response#getResponse()}
 * array. The client then sends a message of type {@link OtpNewKeyAck}. When
 * received the GameBoot server activates the new key for all traffic on the
 * {@link OtpClearWebSocketHandler} channel and disconnects this connection.<br>
 * <br>
 * 
 * Should any failures occur the old key, should it exist, is considered active.
 * 
 */
@Component
@Profile(OtpConfiguration.OTP_PROFILE)
public class OtpEncryptedWebSocketHandler extends AbstractGameBootWebSocketHandler {

  private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Autowired
  @Qualifier(GameBootConcurrentConfiguration.GAME_BOOT_EXECUTOR)
  private ExecutorService svc;

  @Autowired
  private GameBootMessageConverter converter;

  @Autowired
  private WebSocketSessionRegistry registry;

  @Autowired
  private GameBootUtils utils;

  /*
   * (non-Javadoc)
   * 
   * @see org.springframework.web.socket.handler.AbstractWebSocketHandler#
   * afterConnectionEstablished(org.springframework.web.socket.WebSocketSession)
   */
  /**
   * After connection established.
   *
   * @param session
   *          the session
   * @throws Exception
   *           the exception
   */
  @Override
  public void afterConnectionEstablished(WebSocketSession session) throws Exception {
    String s = session.getUri().toString();

    if (!s.startsWith("wss")) {
      log.error("Not an encrypted web socket {}, disconnecting", session.getRemoteAddress());
      session.close();
    }

    super.afterConnectionEstablished(session);
  }

  /**
   * Handle binary message impl.
   *
   * @param session
   *          the session
   * @param message
   *          the message
   */
  @Override
  protected void handleBinaryMessageImpl(WebSocketSession session, byte[] message) {
    svc.execute(() -> {
      try {
        processForBinary(session, message);
      } catch (GameBootException | GameBootRuntimeException e) {
        sendErrorBinary(session, e.getMessage());
      } catch (Exception e) {
        log.error("Unexpected exception", e);
        sendUnexpectedErrorBinary(session);
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
  /**
   * Process for binary.
   *
   * @param session
   *          the session
   * @param msg
   *          the msg
   * @throws Exception
   *           the exception
   */
  protected void processForBinary(WebSocketSession session, byte[] msg) throws Exception {
    OtpMessage message = null;
    try {
      message = converter.fromJson(msg);
    } catch (Exception e) {
      log.error("Message received on {} not an OTP message, disconnecting", session.getRemoteAddress());
      session.close();
      return;
    }

    if (!validateChannel(session, message)) return;

    GameBootMessageController controller = utils.getBean(GameBootMessageController.class);

    Response r = process(session, new String(msg), controller, message);
    if (r == null) return;

    BinaryMessage bm = new BinaryMessage(converter.toJsonArray(r));
    session.sendMessage(bm);

    log.debug("Successful send of {} to {}", message.getType(), session.getRemoteAddress());

    Thread.sleep(50);
    session.close(CloseStatus.NORMAL);
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * com.github.mrstampy.gameboot.websocket.AbstractGameBootWebSocketHandler#
   * inspect(org.springframework.web.socket.WebSocketSession,
   * com.github.mrstampy.gameboot.messages.AbstractGameBootMessage)
   */
  protected <AGBM extends AbstractGameBootMessage> boolean inspect(WebSocketSession session, AGBM agbm)
      throws Exception {
    boolean ok = agbm instanceof OtpKeyRequest && KeyFunction.NEW == ((OtpKeyRequest) agbm).getKeyFunction();

    if (!ok) {
      log.error("Unexpected message received, disconnecting: {}", agbm);
      session.close();
    }

    return ok;
  }

  private boolean validateChannel(WebSocketSession session, OtpMessage message) throws IOException {
    Long systemId = message.getSystemId();
    WebSocketSession clearChannel = registry.get(systemId);

    if (clearChannel == null || !clearChannel.isOpen()) {
      log.error("No clear channel for {}, from encrypted channel {}, disconnecting",
          systemId,
          session.getRemoteAddress());
      session.close();
      return false;
    }

    String encryptedHost = session.getRemoteAddress().getAddress().getHostAddress();
    String clearHost = clearChannel.getRemoteAddress().getAddress().getHostAddress();

    if (encryptedHost.equals(clearHost)) return true;

    log.error("OTP request type {} from {} does not match host {} using system id {}, disconnecting.",
        message.getType(),
        session.getRemoteAddress(),
        clearChannel,
        systemId);

    session.close();

    return false;
  }

  /**
   * Handle text message impl.
   *
   * @param session
   *          the session
   * @param message
   *          the message
   * @throws Exception
   *           the exception
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

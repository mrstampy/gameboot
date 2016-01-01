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
import java.nio.ByteBuffer;
import java.util.concurrent.ExecutorService;

import javax.websocket.Session;

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
import org.springframework.web.socket.adapter.AbstractWebSocketSession;
import org.springframework.web.socket.handler.BinaryWebSocketHandler;

import com.github.mrstampy.gameboot.concurrent.GameBootConcurrentConfiguration;
import com.github.mrstampy.gameboot.messages.GameBootMessageConverter;
import com.github.mrstampy.gameboot.messages.Response;
import com.github.mrstampy.gameboot.otp.OtpConfiguration;
import com.github.mrstampy.gameboot.otp.messages.OtpKeyRequest;
import com.github.mrstampy.gameboot.otp.messages.OtpMessage;
import com.github.mrstampy.gameboot.otp.messages.OtpNewKeyAck;
import com.github.mrstampy.gameboot.otp.processor.OtpKeyRequestProcessor;
import com.github.mrstampy.gameboot.systemid.SystemIdKey;
import com.github.mrstampy.gameboot.websocket.WebSocketSessionRegistry;

/**
 * The Class OtpEncryptedWebSocketHandler is the {@link WebSocketHandler}
 * intended to process {@link OtpMessage}s. The connection must be encrypted
 * sending byte arrays as messages. Should these conditions fail the connection
 * will be terminated.<br>
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
 * containing the key as the only element of the {@link Response#getPayload()}
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
public class OtpEncryptedWebSocketHandler extends BinaryWebSocketHandler {

  private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Autowired
  @Qualifier(GameBootConcurrentConfiguration.GAME_BOOT_EXECUTOR)
  private ExecutorService svc;

  @Autowired
  private GameBootMessageConverter converter;

  @Autowired
  private WebSocketSessionRegistry registry;

  @Autowired
  private OtpKeyRequestProcessor processor;

  /**
   * After connection established.
   *
   * @param session
   *          the session
   * @throws Exception
   *           the exception
   */
  @SuppressWarnings("unchecked")
  @Override
  public void afterConnectionEstablished(WebSocketSession session) throws Exception {
    boolean encrypted = ((AbstractWebSocketSession<Session>) session).getNativeSession().isSecure();

    if (!encrypted) {
      log.error("Not an encrypted web socket {}, disconnecting", session.getRemoteAddress());
      session.close();
    }

    super.afterConnectionEstablished(session);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.springframework.web.socket.handler.AbstractWebSocketHandler#
   * handleBinaryMessage(org.springframework.web.socket.WebSocketSession,
   * org.springframework.web.socket.BinaryMessage)
   */
  protected void handleBinaryMessage(WebSocketSession session, BinaryMessage message) throws Exception {
    svc.execute(() -> {
      try {
        byte[] array = extractArray(session, message);
        if (array != null) processForBinary(session, array);
      } catch (Exception e) {
        log.error("Unexpected exception, disconnecting {}", session, e);
        try {
          session.close();
        } catch (Exception e1) {
          log.error("Unexpected exception closing session {}", session, e1);
        }
      }
    });
  }

  private byte[] extractArray(WebSocketSession session, BinaryMessage message) throws IOException {
    ByteBuffer buf = message.getPayload();

    if (buf.hasArray()) return buf.array();

    int size = buf.remaining();

    if (size == 0) {
      log.error("No message, closing session {}", session);
      session.close();
      return null;
    }

    byte[] b = new byte[size];

    buf.get(b, 0, b.length);

    return b;
  }

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
    OtpKeyRequest message = converter.fromJson(msg);

    if (!validateChannel(session, message)) return;

    Response r = processor.process(message);

    if (r == null || !r.isSuccess()) {
      log.error("Unexpected response {}, disconnecting {}", r, session);
      session.close();
      return;
    }

    BinaryMessage bm = new BinaryMessage(converter.toJsonArray(r));
    session.sendMessage(bm);

    log.debug("Successful send of {} to {}", message.getType(), session.getRemoteAddress());

    Thread.sleep(50);
    session.close(CloseStatus.NORMAL);
  }

  /**
   * Validates that the clear channel exists. Override to provide additional
   * validation.
   *
   * @param session
   *          the session
   * @param message
   *          the message
   * @return true, if successful
   * @throws Exception
   *           the exception
   */
  protected boolean validateChannel(WebSocketSession session, OtpKeyRequest message) throws Exception {
    if (message.getKeyFunction() == null) {
      session.close();
      return false;
    }

    switch (message.getKeyFunction()) {
    case NEW:
      break;
    default:
      log.error("Cannot process {}, closing OTP New Key session {}", message, session);
      session.close();
      return false;
    }

    Long systemId = message.getOtpSystemId();
    if (systemId == null) {
      log.error("System id missing from {}, disconnecting {}", message, session);

      session.close();
      return false;
    }

    SystemIdKey sik = new SystemIdKey(systemId);

    WebSocketSession clearChannel = registry.get(sik);
    if (clearChannel == null || !clearChannel.isOpen()) {
      log.error("No clear channel for {}, from encrypted channel {}, disconnecting",
          systemId,
          session.getRemoteAddress());
      session.close();
      return false;
    }

    return true;
  }

}

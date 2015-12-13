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
import com.github.mrstampy.gameboot.exception.GameBootException;
import com.github.mrstampy.gameboot.exception.GameBootRuntimeException;
import com.github.mrstampy.gameboot.messages.AbstractGameBootMessage;
import com.github.mrstampy.gameboot.messages.GameBootMessageConverter;
import com.github.mrstampy.gameboot.messages.Response;
import com.github.mrstampy.gameboot.messages.Response.ResponseCode;
import com.github.mrstampy.gameboot.otp.KeyRegistry;
import com.github.mrstampy.gameboot.otp.OneTimePad;
import com.github.mrstampy.gameboot.otp.OtpConfiguration;
import com.github.mrstampy.gameboot.otp.messages.OtpKeyRequest;
import com.github.mrstampy.gameboot.otp.messages.OtpKeyRequest.KeyFunction;
import com.github.mrstampy.gameboot.otp.messages.OtpMessage;
import com.github.mrstampy.gameboot.otp.messages.OtpNewKeyAck;
import com.github.mrstampy.gameboot.websocket.AbstractGameBootWebSocketHandler;

/**
 * The Class OtpClearWebSocketHandler is intended to provide a transparent means
 * of using the {@link OneTimePad} utility to encrypt outgoing and decrypt
 * incoming messages on unencrypted web socket connections. It is intended that
 * the message is a byte array at the point in which this class is inserted into
 * the pipeline. Inbound messages are later converted to strings, all outbound
 * messages are byte arrays.<br>
 * <br>
 * 
 * By default messages are unencrypted. An INFO message is sent to the client
 * containing the {@link Response#getSystemId()} value upon connection. The
 * client then creates a connection to the socket server containing the
 * {@link OtpEncryptedWebSocketHandler} in the pipeline and sends a message of
 * type {@link OtpKeyRequest} thru it to the server. The
 * {@link OtpKeyRequest#getSystemId()} value will have been set in the client as
 * the value obtained from the clear connection's INFO message.<br>
 * <br>
 * 
 * If the key generation is successful a {@link Response} object is returned in
 * the encrypted channel containing the new OTP key as the only element of the
 * {@link Response#getResponse()} array. When sending is complete the encrypted
 * channel is disconnected. The client then sends a message of type
 * {@link OtpNewKeyAck} in the clear channel. When received the GameBoot server
 * activates the new key for all traffic on the {@link OtpClearWebSocketHandler}
 * channel and disconnects the encrypted connection.<br>
 * <br>
 * 
 * To delete a key a message of type {@link OtpKeyRequest} with a
 * {@link KeyFunction} of {@link KeyFunction#DELETE} is sent to the server on
 * the encrypting clear channel. A {@link Response} of
 * {@link ResponseCode#SUCCESS} will be sent on success, clear text.<br>
 * <br>
 * 
 * Should any failures occur the old key, should it exist, is considered active.
 * 
 * It is intended that full implementations of GameBoot will implement
 * subclasses of this class to restrict message type processing to a whitelist.
 * <br>
 * <br>
 * 
 * @see KeyRegistry
 * @see OneTimePad
 * @see OtpConfiguration
 * @see #inspect(WebSocketSession, AbstractGameBootMessage)
 * @see #isValidType(WebSocketSession, AbstractGameBootMessage)
 */
@Component
public class OtpClearWebSocketHandler extends AbstractGameBootWebSocketHandler {

  private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Autowired
  private KeyRegistry keyRegistry;

  @Autowired
  private OneTimePad pad;

  @Autowired
  @Qualifier(GameBootConcurrentConfiguration.GAME_BOOT_EXECUTOR)
  private ExecutorService svc;

  @Autowired
  private GameBootMessageConverter converter;

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

    Response r = new Response(ResponseCode.INFO);
    r.setSystemId(getKey());

    BinaryMessage bm = new BinaryMessage(converter.toJsonArray(r));

    session.sendMessage(bm);
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
   * inspect(org.springframework.web.socket.WebSocketSession,
   * com.github.mrstampy.gameboot.messages.AbstractGameBootMessage)
   */
  protected <AGBM extends AbstractGameBootMessage> boolean inspect(WebSocketSession session, AGBM agbm) {
    boolean ok = true;

    switch (agbm.getType()) {
    case OtpKeyRequest.TYPE:
      ok = isDeleteRequest(session, (OtpKeyRequest) agbm);
    case OtpNewKeyAck.TYPE:
      ((OtpMessage) agbm).setProcessorKey(getKey());
      break;
    default:
      ok = isValidType(session, agbm);
    }

    return ok;
  }

  /**
   * Checks if is valid type.
   *
   * @param <AGBM>
   *          the generic type
   * @param session
   *          the session
   * @param agbm
   *          the agbm
   * @return true, if is valid type
   */
  protected <AGBM extends AbstractGameBootMessage> boolean isValidType(WebSocketSession session, AGBM agbm) {
    return true;
  }

  private <AGBM extends AbstractGameBootMessage> boolean isDeleteRequest(WebSocketSession session, AGBM agbm) {
    OtpKeyRequest keyRequest = (OtpKeyRequest) agbm;
    keyRequest.setProcessorKey(getKey());

    boolean d = KeyFunction.DELETE == keyRequest.getKeyFunction();

    Long sysId = keyRequest.getSystemId();
    boolean ok = d && isEncrypting() && getKey().equals(sysId);

    if (!ok) log.error("Delete key for {} received on {}, key {}", sysId, session.getRemoteAddress(), getKey());

    return ok;
  }

  private boolean isEncrypting() {
    return keyRegistry.contains(getKey());
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * com.github.mrstampy.gameboot.websocket.AbstractGameBootWebSocketHandler#
   * processForBinary(org.springframework.web.socket.WebSocketSession, byte[])
   */
  protected void processForBinary(WebSocketSession session, byte[] message) throws Exception {
    byte[] key = keyRegistry.get(getKey());
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

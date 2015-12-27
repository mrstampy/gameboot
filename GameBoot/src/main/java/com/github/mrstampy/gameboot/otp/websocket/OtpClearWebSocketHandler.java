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
import java.util.concurrent.ExecutorService;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import com.github.mrstampy.gameboot.concurrent.GameBootConcurrentConfiguration;
import com.github.mrstampy.gameboot.messages.GameBootMessageConverter;
import com.github.mrstampy.gameboot.messages.Response;
import com.github.mrstampy.gameboot.messages.Response.ResponseCode;
import com.github.mrstampy.gameboot.messages.SystemIdResponse;
import com.github.mrstampy.gameboot.otp.KeyRegistry;
import com.github.mrstampy.gameboot.otp.OneTimePad;
import com.github.mrstampy.gameboot.otp.OtpConfiguration;
import com.github.mrstampy.gameboot.otp.messages.OtpKeyRequest;
import com.github.mrstampy.gameboot.otp.messages.OtpKeyRequest.KeyFunction;
import com.github.mrstampy.gameboot.otp.messages.OtpNewKeyAck;
import com.github.mrstampy.gameboot.websocket.AbstractGameBootWebSocketHandler;

/**
 * The Class OtpClearWebSocketHandler is intended to provide a transparent means
 * of using the {@link OneTimePad} utility to encrypt outgoing and decrypt
 * incoming messages on unencrypted web socket connections. It is intended that
 * this is a last-in-pipeline handler andthe message is a byte array. Inbound
 * messages are later converted to strings, all outbound messages are byte
 * arrays.<br>
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
 * {@link OtpNewKeyAck} encrypted with the new key in the clear channel. When
 * received the GameBoot server activates the new key for all traffic on the
 * {@link OtpClearWebSocketHandler} channel and disconnects the encrypted
 * connection.<br>
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
 * subclasses of the {@link OtpClearWebSocketProcessor} class to restrict
 * message type processing to a whitelist. <br>
 * <br>
 * 
 * @see KeyRegistry
 * @see OneTimePad
 * @see OtpConfiguration
 */
@Component
@Profile(OtpConfiguration.OTP_PROFILE)
public class OtpClearWebSocketHandler
    extends AbstractGameBootWebSocketHandler<WebSocketSession, OtpClearWebSocketProcessor> {

  @Autowired
  @Qualifier(GameBootConcurrentConfiguration.GAME_BOOT_EXECUTOR)
  private ExecutorService svc;

  @Autowired
  private GameBootMessageConverter converter;

  /*
   * (non-Javadoc)
   * 
   * @see
   * com.github.mrstampy.gameboot.websocket.AbstractGameBootWebSocketHandler#
   * postConstruct()
   */
  @PostConstruct
  public void postConstruct() throws Exception {
    super.postConstruct();
  }

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
    super.afterConnectionEstablished(session);

    OtpClearWebSocketProcessor webSocketProcessor = getConnectionProcessor();

    Response r = new Response(ResponseCode.INFO, new SystemIdResponse(webSocketProcessor.getSystemId(session)));

    webSocketProcessor.sendMessage(session, converter.toJsonArray(r));
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * com.github.mrstampy.gameboot.websocket.AbstractGameBootWebSocketHandler#
   * setConnectionProcessor(com.github.mrstampy.gameboot.processor.connection.
   * ConnectionProcessor)
   */
  @Autowired
  public void setConnectionProcessor(OtpClearWebSocketProcessor connectionProcessor) {
    super.setConnectionProcessor(connectionProcessor);
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * com.github.mrstampy.gameboot.websocket.AbstractGameBootWebSocketHandler#
   * handleTextMessage(org.springframework.web.socket.WebSocketSession,
   * org.springframework.web.socket.TextMessage)
   */
  protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
    try {
      session.close(CloseStatus.NOT_ACCEPTABLE.withReason("Text messages not supported"));
    } catch (IOException e) {
      // ignore
    }
  }

}

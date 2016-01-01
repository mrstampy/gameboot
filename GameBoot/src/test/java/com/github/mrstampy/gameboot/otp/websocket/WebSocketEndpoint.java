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

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import javax.resource.spi.IllegalStateException;
import javax.websocket.CloseReason;
import javax.websocket.Endpoint;
import javax.websocket.EndpointConfig;
import javax.websocket.MessageHandler;
import javax.websocket.OnMessage;
import javax.websocket.Session;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.mrstampy.gameboot.messages.Response;
import com.github.mrstampy.gameboot.messages.Response.ResponseCode;
import com.github.mrstampy.gameboot.otp.OneTimePad;
import com.github.mrstampy.gameboot.otp.messages.OtpKeyRequest;

/**
 * The Class WebSocketEndpoint.
 */
public class WebSocketEndpoint extends Endpoint {
  private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Autowired
  private ObjectMapper mapper;

  @Autowired
  private OneTimePad pad;

  private byte[] otpKey;

  private Session session;

  private Long systemId;

  private Response lastResponse;

  private CountDownLatch responseLatch;

  /*
   * (non-Javadoc)
   * 
   * @see javax.websocket.Endpoint#onOpen(javax.websocket.Session,
   * javax.websocket.EndpointConfig)
   */
  @Override
  public void onOpen(Session session, EndpointConfig config) {
    log.debug("Session {} open on channel", session.getId());

    session.addMessageHandler(new MessageHandler.Whole<byte[]>() {

      @Override
      public void onMessage(byte[] message) {
        try {
          WebSocketEndpoint.this.onMessage(message, session);
        } catch (Exception e) {
          log.error("Unexpected exception", e);
        }
      }
    });
  }

  /*
   * (non-Javadoc)
   * 
   * @see javax.websocket.Endpoint#onClose(javax.websocket.Session,
   * javax.websocket.CloseReason)
   */
  @Override
  public void onClose(Session session, CloseReason closeReason) {
    log.debug("Session {} closed on channel", session.getId());
    if (this.session == session) this.session = null;
  }

  /**
   * On message.
   *
   * @param msg
   *          the msg
   * @param session
   *          the session
   * @throws Exception
   *           the exception
   */
  @OnMessage
  public void onMessage(byte[] msg, Session session) throws Exception {
    log.debug("Message received for channel {}", session.getId());
    try {
      if (isEncrypting(session)) {
        try {
          decrypt(msg);
          return;
        } catch (Exception e) {
          log.error("Cannot decrypt: assuming delete request sent: {}", e.getMessage());
        }
      }

      unencrypted(session, msg);
    } finally {
      if (responseLatch != null) responseLatch.countDown();
    }
  }

  /**
   * Send message.
   *
   * @param msg
   *          the msg
   * @param session
   *          the session
   * @throws Exception
   *           the exception
   */
  public void sendMessage(byte[] msg, Session session) throws Exception {
    byte[] converted = isEncrypting(session) ? pad.convert(otpKey, msg) : msg;

    session.getBasicRemote().sendBinary(ByteBuffer.wrap(converted));
  }

  private void decrypt(byte[] msg) throws Exception {
    byte[] converted = pad.convert(otpKey, msg);

    Response r = getResponse(converted);
    lastResponse = r;

    log.info("Encrypted: \n{}", mapper.writeValueAsString(r));
  }

  private void unencrypted(Session ctx, byte[] msg) throws Exception {
    Response r = getResponse(msg);
    lastResponse = r;

    log.info("Unencrypted: on session {}\n{}", ctx.getId(), mapper.writeValueAsString(r));

    if (!ok(r.getResponseCode())) return;

    if (ResponseCode.INFO == r.getResponseCode()) {
      Object[] payload = r.getPayload();
      if (payload == null || payload.length == 0 || !(payload[0] instanceof Map<?, ?>)) {
        throw new IllegalStateException("Expecting map of systemId:[value]");
      }

      systemId = (Long) ((Map<?, ?>) payload[0]).get("systemId");

      log.info("Setting system id {}", systemId);
      this.session = ctx;
      return;
    }

    JsonNode node = mapper.readTree(msg);
    JsonNode response = node.get("payload");

    boolean hasKey = response != null && response.isArray() && response.size() == 1;

    if (hasKey) {
      log.info("Setting key");
      otpKey = response.get(0).binaryValue();
      return;
    }

    switch (r.getType()) {
    case OtpKeyRequest.TYPE:
      log.info("Deleting key");
      otpKey = null;
      break;
    default:
      break;
    }
  }

  private Response getResponse(byte[] msg) throws IOException, JsonParseException, JsonMappingException {
    return mapper.readValue(msg, Response.class);
  }

  private boolean ok(ResponseCode responseCode) {
    if (responseCode == null) return false;
    switch (responseCode) {
    case SUCCESS:
    case INFO:
      return true;
    default:
      return false;
    }
  }

  private boolean isEncrypting(Session ctx) {
    return otpKey != null && this.session == ctx;
  }

  /**
   * Gets the system id.
   *
   * @return the system id
   */
  public Long getSystemId() {
    return systemId;
  }

  /**
   * Gets the session.
   *
   * @return the session
   */
  public Session getSession() {
    return session;
  }

  /**
   * Gets the last response.
   *
   * @return the last response
   */
  public Response getLastResponse() {
    try {
      return lastResponse;
    } finally {
      lastResponse = null;
    }
  }

  /**
   * Sets the response latch.
   *
   * @param responseLatch
   *          the new response latch
   */
  public void setResponseLatch(CountDownLatch responseLatch) {
    this.responseLatch = responseLatch;
  }

  /**
   * Checks for key.
   *
   * @return true, if successful
   */
  public boolean hasKey() {
    return otpKey != null;
  }

}

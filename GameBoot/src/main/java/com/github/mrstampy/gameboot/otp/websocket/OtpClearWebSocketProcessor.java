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

import java.lang.invoke.MethodHandles;
import java.util.concurrent.ExecutorService;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Profile;
import org.springframework.web.socket.WebSocketSession;

import com.github.mrstampy.gameboot.concurrent.GameBootConcurrentConfiguration;
import com.github.mrstampy.gameboot.exception.GameBootException;
import com.github.mrstampy.gameboot.exception.GameBootRuntimeException;
import com.github.mrstampy.gameboot.messages.AbstractGameBootMessage;
import com.github.mrstampy.gameboot.messages.GameBootMessageConverter;
import com.github.mrstampy.gameboot.messages.Response;
import com.github.mrstampy.gameboot.metrics.MetricsHelper;
import com.github.mrstampy.gameboot.otp.KeyRegistry;
import com.github.mrstampy.gameboot.otp.OneTimePad;
import com.github.mrstampy.gameboot.otp.OtpConfiguration;
import com.github.mrstampy.gameboot.otp.messages.OtpKeyRequest;
import com.github.mrstampy.gameboot.otp.messages.OtpKeyRequest.KeyFunction;
import com.github.mrstampy.gameboot.otp.messages.OtpMessage;
import com.github.mrstampy.gameboot.otp.messages.OtpNewKeyAck;
import com.github.mrstampy.gameboot.otp.processor.OtpNewKeyRegistry;
import com.github.mrstampy.gameboot.util.concurrent.MDCRunnable;
import com.github.mrstampy.gameboot.websocket.AbstractWebSocketProcessor;

/**
 * The Class OtpClearWebSocketProcessor.
 */
@Profile(OtpConfiguration.OTP_PROFILE)
public class OtpClearWebSocketProcessor extends AbstractWebSocketProcessor {
  private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private static final String OTP_DECRYPT_COUNTER = "Web Socket OTP Decrypt Counter";

  private static final String OTP_ENCRYPT_COUNTER = "Web Socket OTP Encrypt Counter";

  @Autowired
  private GameBootMessageConverter converter;

  @Autowired
  private OneTimePad oneTimePad;

  @Autowired
  private MetricsHelper helper;

  @Autowired
  @Qualifier(GameBootConcurrentConfiguration.GAME_BOOT_EXECUTOR)
  private ExecutorService svc;

  @Autowired
  private KeyRegistry keyRegistry;

  @Autowired
  private OtpNewKeyRegistry newKeyRegistry;

  /*
   * (non-Javadoc)
   * 
   * @see com.github.mrstampy.gameboot.websocket.AbstractWebSocketProcessor#
   * postConstruct()
   */
  @PostConstruct
  public void postConstruct() throws Exception {
    super.postConstruct();

    if (!helper.containsCounter(OTP_DECRYPT_COUNTER)) {
      helper.counter(OTP_DECRYPT_COUNTER, getClass(), "otp", "decrypt", "counter");
    }

    if (!helper.containsCounter(OTP_ENCRYPT_COUNTER)) {
      helper.counter(OTP_ENCRYPT_COUNTER, getClass(), "otp", "encrypt", "counter");
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * com.github.mrstampy.gameboot.websocket.AbstractWebSocketProcessor#onMessage
   * (org.springframework.web.socket.WebSocketSession, java.lang.Object)
   */
  public void onMessage(WebSocketSession session, Object msg) throws Exception {
    if (!(msg instanceof byte[])) {
      sendError(getResponseContext(NOT_BYTE_ARRAY, session), session, "Message must be a byte array");
      return;
    }

    byte[] mb = (byte[]) msg;

    byte[] key = keyRegistry.get(session.getId());

    byte[] b = evaluateForNewKeyAck(session, mb);

    if (key == null) {
      super.onMessage(session, b);
      return;
    }

    helper.incr(OTP_DECRYPT_COUNTER);

    byte[] converted = b == mb ? oneTimePad.convert(key, mb) : b;

    super.onMessage(session, converted);
  }

  @SuppressWarnings("unused")
  private byte[] evaluateForNewKeyAck(WebSocketSession session, byte[] msg) {
    Long systemId = getSystemId(session);
    if (!newKeyRegistry.contains(systemId)) return msg;

    byte[] newKey = newKeyRegistry.get(systemId);

    try {
      byte[] converted = oneTimePad.convert(newKey, msg);
      OtpNewKeyAck ack = converter.fromJson(converted);
      return converted;
    } catch (Exception e) {
      String s = keyRegistry.contains(systemId) ? "old key" : "unencrypted";
      log.warn("Awaiting new key ack, assuming {} for {}, system id {}.", s, session, systemId);
    }

    return msg;
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.github.mrstampy.gameboot.websocket.AbstractWebSocketProcessor#
   * onMessageImpl(org.springframework.web.socket.WebSocketSession, byte[])
   */
  protected void onMessageImpl(WebSocketSession session, byte[] msg) throws Exception {
    svc.execute(new MDCRunnable() {

      @Override
      protected void runImpl() {
        try {
          process(session, msg);
        } catch (GameBootException | GameBootRuntimeException e) {
          sendError(session, e);
        } catch (Exception e) {
          log.error("Unexpected exception", e);
          sendUnexpectedError(session);
        }
      }
    });
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.github.mrstampy.gameboot.processor.connection.ConnectionProcessor#
   * preProcess(java.lang.Object,
   * com.github.mrstampy.gameboot.messages.AbstractGameBootMessage)
   */
  @Override
  public <AGBM extends AbstractGameBootMessage> boolean preProcess(WebSocketSession session, AGBM agbm)
      throws Exception {
    boolean ok = true;

    switch (agbm.getType()) {
    case OtpKeyRequest.TYPE:
      ok = isDeleteRequest(session, (OtpKeyRequest) agbm);
      if (!ok) {
        Response fail = fail(getResponseContext(UNEXPECTED_MESSAGE, session), agbm);
        sendMessage(session, converter.toJsonArray(fail));
      }
    case OtpNewKeyAck.TYPE:
      ((OtpMessage) agbm).setProcessorKey(getSystemId(session));
      break;
    default:
      ok = isValidType(session, agbm);
    }

    return ok;
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.github.mrstampy.gameboot.processor.connection.ConnectionProcessor#
   * postProcess(java.lang.Object,
   * com.github.mrstampy.gameboot.messages.AbstractGameBootMessage,
   * com.github.mrstampy.gameboot.messages.Response)
   */
  @Override
  public <AGBM extends AbstractGameBootMessage> void postProcess(WebSocketSession session, AGBM agbm, Response r) {
    // TODO Auto-generated method stub

  }

  /**
   * Checks if is encrypting.
   *
   * @param session
   *          the session
   * @return true, if is encrypting
   */
  public boolean isEncrypting(WebSocketSession session) {
    return keyRegistry.contains(session.getId());
  }

  /**
   * Encrypt if required.
   *
   * @param session
   *          the session
   * @param msg
   *          the msg
   * @return the byte[]
   * @throws Exception
   *           the exception
   */
  public byte[] encryptIfRequired(WebSocketSession session, Object msg) throws Exception {
    if (!(msg instanceof String) && !(msg instanceof byte[])) {
      log.error("Internal error; object is not a string or byte array: {}", msg.getClass());
      return null;
    }

    byte[] processed = (msg instanceof byte[]) ? (byte[]) msg : ((String) msg).getBytes();
    if (!isEncrypting(session)) return processed;

    byte[] key = keyRegistry.get(session.getId());

    helper.incr(OTP_ENCRYPT_COUNTER);

    return oneTimePad.convert(key, processed);
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.github.mrstampy.gameboot.websocket.AbstractWebSocketProcessor#
   * onMessageImpl(org.springframework.web.socket.WebSocketSession,
   * java.lang.String)
   */
  @Override
  protected void onMessageImpl(WebSocketSession session, String msg) throws Exception {
    throw new UnsupportedOperationException("Must be a byte array");
  }

  /**
   * Implement in subclasses to white list {@link AbstractGameBootMessage}s.
   *
   * @param <AGBM>
   *          the generic type
   * @param session
   *          the session
   * @param agbm
   *          the agbm
   * @return true, if is valid type
   * @see OtpConfiguration
   */
  protected <AGBM extends AbstractGameBootMessage> boolean isValidType(WebSocketSession session, AGBM agbm) {
    return true;
  }

  private <AGBM extends AbstractGameBootMessage> boolean isDeleteRequest(WebSocketSession session, AGBM agbm) {
    OtpKeyRequest keyRequest = (OtpKeyRequest) agbm;

    boolean d = KeyFunction.DELETE == keyRequest.getKeyFunction();

    Long sysId = keyRequest.getSystemId();
    Long thisSysId = getSystemId(session);
    boolean ok = d && isEncrypting(session) && thisSysId.equals(sysId);

    if (!ok) log.error("Delete key for {} received on {}, key {}", sysId, session.getRemoteAddress(), thisSysId);

    return ok;
  }

}

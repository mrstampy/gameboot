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
package com.github.mrstampy.gameboot.websocket;

import java.lang.invoke.MethodHandles;
import java.util.Map.Entry;
import java.util.Set;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.AbstractWebSocketHandler;

import com.github.mrstampy.gameboot.SystemId;
import com.github.mrstampy.gameboot.controller.GameBootMessageController;
import com.github.mrstampy.gameboot.exception.GameBootException;
import com.github.mrstampy.gameboot.exception.GameBootRuntimeException;
import com.github.mrstampy.gameboot.messages.AbstractGameBootMessage;
import com.github.mrstampy.gameboot.messages.AbstractGameBootMessage.Transport;
import com.github.mrstampy.gameboot.messages.GameBootMessageConverter;
import com.github.mrstampy.gameboot.messages.Response;
import com.github.mrstampy.gameboot.messages.Response.ResponseCode;
import com.github.mrstampy.gameboot.messages.error.Error;
import com.github.mrstampy.gameboot.messages.error.ErrorCodes;
import com.github.mrstampy.gameboot.messages.error.ErrorLookup;
import com.github.mrstampy.gameboot.metrics.MetricsHelper;
import com.github.mrstampy.gameboot.util.GameBootUtils;
import com.github.mrstampy.gameboot.util.RegistryCleaner;

/**
 * The Class AbstractGameBootWebSocketHandler is the superclass of
 * {@link WebSocketHandler}s which can handle either text or binary GameBoot web
 * socket messages. Messages are automatically processed and responses returned
 * as appropriate.
 * 
 * @see GameBootMessageController
 */
public abstract class AbstractGameBootWebSocketHandler extends AbstractWebSocketHandler implements ErrorCodes {
  private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private static final String MESSAGE_COUNTER = "GameBoot Web Socket Message Counter";

  private static final String FAILED_MESSAGE_COUNTER = "GameBoot Web Socket Failed Message Counter";

  @Autowired
  private MetricsHelper helper;

  @Autowired
  private GameBootUtils utils;

  @Autowired
  private WebSocketSessionRegistry registry;

  @Autowired
  private GameBootMessageConverter converter;

  @Autowired
  private SystemId generator;

  @Autowired
  private RegistryCleaner cleaner;

  @Autowired
  private ErrorLookup lookup;

  private Long systemId;

  /**
   * Post construct created message counters if necessary. Subclasses will need
   * to invoke this in an annotated {@link PostConstruct} method.
   *
   * @throws Exception
   *           the exception
   */
  protected void postConstruct() throws Exception {
    if (!helper.containsCounter(MESSAGE_COUNTER)) {
      helper.counter(MESSAGE_COUNTER, getClass(), "inbound", "messages");
    }

    if (!helper.containsCounter(FAILED_MESSAGE_COUNTER)) {
      helper.counter(FAILED_MESSAGE_COUNTER, getClass(), "failed", "messages");
    }
  }

  /**
   * Sets the {@link SystemId#next()} key and adds the WebSocketSession to the
   * {@link WebSocketSessionRegistry}. Subclasses overriding must invoke super.
   *
   * @param session
   *          the session
   * @throws Exception
   *           the exception
   */
  @Override
  public void afterConnectionEstablished(WebSocketSession session) throws Exception {
    this.systemId = generator.next();
    addToRegistry(session);
  }

  /**
   * Removes this {@link WebSocketSession} from the registry. Subclasses
   * overriding must invoke super.
   *
   * @param session
   *          the session
   * @param status
   *          the status
   * @throws Exception
   *           the exception
   */
  @Override
  public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
    cleaner.cleanup(getSystemId());

    Set<Entry<Comparable<?>, WebSocketSession>> set = registry.getKeysForValue(session);

    set.forEach(e -> registry.remove(e.getKey()));
  }

  /**
   * Handle text message.
   *
   * @param session
   *          the session
   * @param message
   *          the message
   * @throws Exception
   *           the exception
   */
  protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
    if (message.getPayloadLength() <= 0) return;

    helper.incr(MESSAGE_COUNTER);

    handleTextMessageImpl(session, message.getPayload());
  }

  /**
   * Handle binary message.
   *
   * @param session
   *          the session
   * @param message
   *          the message
   * @throws Exception
   *           the exception
   */
  protected void handleBinaryMessage(WebSocketSession session, BinaryMessage message) throws Exception {
    if (message.getPayloadLength() <= 0) return;

    helper.incr(MESSAGE_COUNTER);

    handleBinaryMessageImpl(session, message.getPayload().array());
  }

  /**
   * Handle binary message impl.
   *
   * @param session
   *          the session
   * @param message
   *          the message
   */
  protected abstract void handleBinaryMessageImpl(WebSocketSession session, byte[] message);

  /**
   * Process for binary should be invoked from
   * {@link #handleBinaryMessageImpl(WebSocketSession, byte[])}. Responses are
   * automatically sent. Override to intercept the response message.
   *
   * @param session
   *          the session
   * @param message
   *          the message
   * @throws Exception
   *           the exception
   */
  protected void processForBinary(WebSocketSession session, byte[] message) throws Exception {
    String response = process(session, new String(message));
    if (response == null) return;

    BinaryMessage m = new BinaryMessage(response.getBytes());
    session.sendMessage(m);
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
  protected abstract void handleTextMessageImpl(WebSocketSession session, String message) throws Exception;

  /**
   * Process for text should be invoked from
   * {@link #handleTextMessageImpl(WebSocketSession, String)}. Responses are
   * automatically sent. Override to intercept the response message.
   *
   * @param session
   *          the session
   * @param message
   *          the message
   * @throws Exception
   *           the exception
   */
  protected void processForText(WebSocketSession session, String message) throws Exception {
    String response = process(session, message);
    if (response == null) return;

    TextMessage m = new TextMessage(response.getBytes());
    session.sendMessage(m);
  }

  /**
   * Process.
   *
   * @param <AGBM>
   *          the generic type
   * @param session
   *          the session
   * @param msg
   *          the msg
   * @return the string
   * @throws Exception
   *           the exception
   */
  protected <AGBM extends AbstractGameBootMessage> String process(WebSocketSession session, String msg)
      throws Exception {
    GameBootMessageController controller = utils.getBean(GameBootMessageController.class);

    Response response = null;
    AGBM agbm = null;
    try {
      agbm = converter.fromJson(msg);

      if (!inspect(session, agbm)) return null;

      response = process(session, msg, controller, agbm);
    } catch (GameBootException | GameBootRuntimeException e) {
      helper.incr(FAILED_MESSAGE_COUNTER);
      response = fail(agbm, e);
    } catch (Exception e) {
      helper.incr(FAILED_MESSAGE_COUNTER);
      log.error("Unexpected exception processing message {} on channel {}", msg, session.getRemoteAddress(), e);
      response = fail(UNEXPECTED_ERROR, agbm, "An unexpected error has occurred");
    }

    postProcess(session, agbm, response);

    return response == null ? null : converter.toJson(response);
  }

  /**
   * Process, can be invoked in lieu of
   * {@link #process(WebSocketSession, String)} should the message have been
   * {@link GameBootMessageConverter}'ed for inspection in an override of
   * {@link #handleTextMessageImpl(WebSocketSession, String)} or
   * {@link #handleBinaryMessageImpl(WebSocketSession, byte[])}. If invoking
   * this method directly ensure that the instance of
   * {@link GameBootMessageController} is obtained via
   * {@link GameBootUtils#getBean(Class)}.
   *
   * @param <AGBM>
   *          the generic type
   * @param session
   *          the session
   * @param msg
   *          the msg
   * @param controller
   *          the controller
   * @param agbm
   *          the agbm
   * @return the response
   * @throws Exception
   *           the exception
   */
  protected <AGBM extends AbstractGameBootMessage> Response process(WebSocketSession session, String msg,
      GameBootMessageController controller, AGBM agbm) throws Exception {
    if (agbm.getSystemId() == null) agbm.setSystemId(getSystemId());
    agbm.setTransport(Transport.WEB_SOCKET);
    agbm.setLocal(session.getLocalAddress());
    agbm.setRemote(session.getRemoteAddress());

    Response r = controller.process(msg, agbm);
    processMappingKeys(r, session);
    r.setSystemId(agbm.getSystemId());
    return r;
  }

  private void processMappingKeys(Response r, WebSocketSession session) {
    Comparable<?>[] keys = r.getMappingKeys();
    if (keys == null || keys.length == 0) return;

    for (int i = 0; i < keys.length; i++) {
      registry.put(keys[i], session);
    }
  }

  /**
   * Investigate the message prior to processing. Overrides which fail
   * inspection are responsible for sending any failure messages to the client
   * prior to returning false.
   *
   * @param <AGBM>
   *          the generic type
   * @param session
   *          the session
   * @param agbm
   *          the agbm
   * @return true, if successful
   * @throws Exception
   *           the exception
   */
  protected <AGBM extends AbstractGameBootMessage> boolean inspect(WebSocketSession session, AGBM agbm)
      throws Exception {
    return true;
  }

  /**
   * Post process the request and response. Empty implementation; override as
   * appropriate.
   *
   * @param <AGBM>
   *          the generic type
   * @param session
   *          the session
   * @param agbm
   *          the agbm
   * @param response
   *          the response
   */
  protected <AGBM extends AbstractGameBootMessage> void postProcess(WebSocketSession session, AGBM agbm,
      Response response) {
  }

  /**
   * Adds the to registry.
   *
   * @param session
   *          the session
   */
  protected void addToRegistry(WebSocketSession session) {
    if (!registry.contains(getSystemId())) registry.put(getSystemId(), session);
  }

  /**
   * Gets the key in {@link #afterConnectionEstablished(WebSocketSession)} from
   * {@link SystemId#next()}.
   *
   * @return the key
   */
  public Long getSystemId() {
    return systemId;
  }

  /**
   * Send unexpected failure.
   *
   * @param session
   *          the session
   */
  protected void sendUnexpectedError(WebSocketSession session) {
    sendError(null, session, "An unexpected error has occurred");
  }

  /**
   * Send unexpected failure binary.
   *
   * @param session
   *          the session
   */
  protected void sendUnexpectedErrorBinary(WebSocketSession session) {
    sendErrorBinary(null, session, "An unexpected error has occurred");
  }

  /**
   * Send failure.
   *
   * @param messageId
   *          the message id
   * @param session
   *          the session
   * @param msg
   *          the msg
   */
  protected void sendError(Integer messageId, WebSocketSession session, String msg) {
    try {
      Response r = fail(UNEXPECTED_ERROR, msg);
      r.setId(messageId);
      TextMessage fail = new TextMessage(converter.toJsonArray(r));
      session.sendMessage(fail);
    } catch (Exception e) {
      log.error("Unexpected exception sending failure {} for {}", msg, getSystemId(), e);
    }
  }

  /**
   * Send failure.
   *
   * @param session
   *          the session
   * @param e
   *          the e
   */
  protected void sendError(WebSocketSession session, Exception e) {
    try {
      Response r = fail(null, e);
      TextMessage fail = new TextMessage(converter.toJsonArray(r));
      session.sendMessage(fail);
    } catch (Exception f) {
      log.error("Unexpected exception sending failure {} for {}, systemId {}",
          e.getMessage(),
          session,
          getSystemId(),
          f);
    }
  }

  /**
   * Send failure binary.
   *
   * @param messageId
   *          the message id
   * @param session
   *          the session
   * @param msg
   *          the msg
   */
  protected void sendErrorBinary(Integer messageId, WebSocketSession session, String msg) {
    try {
      Response r = fail(UNEXPECTED_ERROR, msg);
      r.setId(messageId);
      BinaryMessage fail = new BinaryMessage(converter.toJsonArray(r));
      session.sendMessage(fail);
    } catch (Exception e) {
      log.error("Unexpected exception sending failure {} for {}", msg, getSystemId(), e);
    }
  }

  /**
   * Send failure binary.
   *
   * @param session
   *          the session
   * @param e
   *          the e
   */
  protected void sendErrorBinary(WebSocketSession session, Exception e) {
    try {
      Response r = fail(null, e);
      BinaryMessage fail = new BinaryMessage(converter.toJsonArray(r));
      session.sendMessage(fail);
    } catch (Exception f) {
      log.error("Unexpected exception sending failure {} for {}, systemId {}",
          e.getMessage(),
          session,
          getSystemId(),
          f);
    }
  }

  /**
   * Fail.
   *
   * @param message
   *          the message
   * @param e
   *          the e
   * @return the response
   */
  protected Response fail(AbstractGameBootMessage message, Exception e) {
    Error error = extractError(e);
    Object[] payload = extractPayload(e);

    Response r = new Response(message, ResponseCode.FAILURE, payload);
    r.setError(error);

    return r;
  }

  private Error extractError(Exception e) {
    boolean rt = e instanceof GameBootRuntimeException;

    return rt ? ((GameBootRuntimeException) e).getError() : ((GameBootException) e).getError();
  }

  private Object[] extractPayload(Exception e) {
    boolean rt = e instanceof GameBootRuntimeException;

    return rt ? ((GameBootRuntimeException) e).getPayload() : ((GameBootException) e).getPayload();
  }

  /**
   * Returns a fail message.
   *
   * @param code
   *          the code
   * @param message
   *          the message
   * @return the string
   */
  protected Response fail(int code, String message) {
    Response r = new Response(ResponseCode.FAILURE, message);
    r.setError(lookup.lookup(code));

    return r;
  }

  /**
   * Returns a fail message.
   *
   * @param code
   *          the code
   * @param message
   *          the message
   * @param payload
   *          the payload
   * @return the string
   */
  protected Response fail(int code, AbstractGameBootMessage message, String payload) {
    Response r = new Response(message, ResponseCode.FAILURE, payload);
    r.setError(lookup.lookup(code));

    return r;
  }
}

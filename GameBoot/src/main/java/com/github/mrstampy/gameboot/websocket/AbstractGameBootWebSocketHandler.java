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
package com.github.mrstampy.gameboot.websocket;

import java.lang.invoke.MethodHandles;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.AbstractWebSocketHandler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.github.mrstampy.gameboot.controller.GameBootMessageController;
import com.github.mrstampy.gameboot.exception.GameBootException;
import com.github.mrstampy.gameboot.exception.GameBootRuntimeException;
import com.github.mrstampy.gameboot.messages.AbstractGameBootMessage;
import com.github.mrstampy.gameboot.messages.AbstractGameBootMessage.Transport;
import com.github.mrstampy.gameboot.messages.GameBootMessageConverter;
import com.github.mrstampy.gameboot.messages.Response;
import com.github.mrstampy.gameboot.messages.Response.ResponseCode;
import com.github.mrstampy.gameboot.metrics.MetricsHelper;
import com.github.mrstampy.gameboot.util.GameBootUtils;

/**
 * The Class AbstractGameBootWebSocketHandler.
 */
public abstract class AbstractGameBootWebSocketHandler extends AbstractWebSocketHandler {
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

  /*
   * (non-Javadoc)
   * 
   * @see org.springframework.web.socket.handler.AbstractWebSocketHandler#
   * afterConnectionEstablished(org.springframework.web.socket.WebSocketSession)
   */
  @Override
  public void afterConnectionEstablished(WebSocketSession session) throws Exception {
    addToRegistry(session);
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
    registry.remove(session.getId());
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.springframework.web.socket.handler.AbstractWebSocketHandler#
   * handleTextMessage(org.springframework.web.socket.WebSocketSession,
   * org.springframework.web.socket.TextMessage)
   */
  protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
    if (message.getPayloadLength() <= 0) return;
    
    helper.incr(MESSAGE_COUNTER);

    handleTextMessageImpl(session, message.getPayload());
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.springframework.web.socket.handler.AbstractWebSocketHandler#
   * handleBinaryMessage(org.springframework.web.socket.WebSocketSession,
   * org.springframework.web.socket.BinaryMessage)
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
   * Process.
   *
   * @param <AGBM>
   *          the generic type
   * @param session
   *          the session
   * @param msg
   *          the msg
   * @throws Exception
   *           the exception
   */
  protected <AGBM extends AbstractGameBootMessage> void process(WebSocketSession session, String msg) throws Exception {
    GameBootMessageController controller = utils.getBean(GameBootMessageController.class);

    String response = null;
    try {
      AGBM agbm = converter.fromJson(msg);

      agbm.setSystemSessionId(session.getId());
      agbm.setTransport(Transport.WEB_SOCKET);

      Response r = controller.process(msg, agbm);
      response = converter.toJson(r);
    } catch (GameBootException | GameBootRuntimeException e) {
      helper.incr(FAILED_MESSAGE_COUNTER);
      response = fail(e.getMessage());
    } catch (Exception e) {
      helper.incr(FAILED_MESSAGE_COUNTER);
      log.error("Unexpected exception processing message {} on channel {}", msg, session.getRemoteAddress(), e);
      response = fail("An unexpected error has occurred");
    }

    if (response == null) return;

    TextMessage r = new TextMessage(response.getBytes());

    session.sendMessage(r);
  }

  /**
   * Adds the to registry.
   *
   * @param session
   *          the session
   */
  protected void addToRegistry(WebSocketSession session) {
    String id = session.getId();
    if (!registry.contains(id)) registry.put(id, session);
  }

  /**
   * Returns a fail message.
   *
   * @param message
   *          the message
   * @return the string
   * @throws GameBootException
   *           the game boot exception
   */
  protected String fail(String message) throws GameBootException {
    try {
      return converter.toJson(new Response(ResponseCode.FAILURE, message));
    } catch (JsonProcessingException e) {
      log.error("Unexpected exception", e);
      throw new RuntimeException("Unexpected JSON error", e);
    }
  }
}

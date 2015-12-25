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

import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.AbstractWebSocketHandler;

import com.github.mrstampy.gameboot.controller.GameBootMessageController;
import com.github.mrstampy.gameboot.messages.context.ResponseContextCodes;
import com.github.mrstampy.gameboot.processor.connection.ConnectionProcessor;

/**
 * The Class AbstractGameBootWebSocketHandler is the superclass of
 * {@link WebSocketHandler}s which can handle either text or binary GameBoot web
 * socket messages. Messages are automatically processed and responses returned
 * as appropriate.
 *
 * @param <C>
 *          the generic type
 * @param <CP>
 *          the generic type
 * @see GameBootMessageController
 */
public abstract class AbstractGameBootWebSocketHandler<C, CP extends ConnectionProcessor<WebSocketSession>>
    extends AbstractWebSocketHandler implements ResponseContextCodes {

  private CP webSocketProcessor;

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
    webSocketProcessor.onMessage(session, message);
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
    webSocketProcessor.onMessage(session, message);
  }

  /**
   * Gets the web socket processor.
   *
   * @return the web socket processor
   */
  public CP getConnectionProcessor() {
    return webSocketProcessor;
  }

  /**
   * Sets the web socket processor.
   *
   * @param webSocketProcessor
   *          the new web socket processor
   */
  public void setConnectionProcessor(CP webSocketProcessor) {
    this.webSocketProcessor = webSocketProcessor;
  }
}

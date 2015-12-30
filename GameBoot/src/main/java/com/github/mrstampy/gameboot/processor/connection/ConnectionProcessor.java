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
package com.github.mrstampy.gameboot.processor.connection;

import java.util.Locale;

import com.github.mrstampy.gameboot.controller.GameBootMessageController;
import com.github.mrstampy.gameboot.exception.GameBootThrowable;
import com.github.mrstampy.gameboot.messages.AbstractGameBootMessage;
import com.github.mrstampy.gameboot.messages.Response;
import com.github.mrstampy.gameboot.messages.context.ResponseContext;
import com.github.mrstampy.gameboot.messages.context.ResponseContextCodes;
import com.github.mrstampy.gameboot.messages.context.ResponseContextLoader;
import com.github.mrstampy.gameboot.messages.context.ResponseContextLookup;
import com.github.mrstampy.gameboot.systemid.SystemIdWrapper;

/**
 * The Interface ConnectionProcessor provides the structure for processing
 * messages arriving from connections to GameBoot, currently WebSocket or Netty.
 *
 * @param <C>
 *          the generic type
 */
public interface ConnectionProcessor<C> extends ResponseContextCodes {

  /**
   * Call this method when the connection has been established.
   *
   * @param ctx
   *          the ctx
   * @throws Exception
   *           the exception
   */
  void onConnection(C ctx) throws Exception;

  /**
   * Call this method when the connection has been terminated.
   *
   * @param ctx
   *          the ctx
   * @throws Exception
   *           the exception
   */
  void onDisconnection(C ctx) throws Exception;

  /**
   * Call this method on receipt of a message.
   *
   * @param ctx
   *          the ctx
   * @param msg
   *          the msg
   * @throws Exception
   *           the exception
   */
  void onMessage(C ctx, Object msg) throws Exception;

  /**
   * Implement to process String messages.
   *
   * @param <AGBM>
   *          the generic type
   * @param ctx
   *          the ctx
   * @param msg
   *          the msg
   * @throws Exception
   *           the exception
   */
  <AGBM extends AbstractGameBootMessage> void process(C ctx, String msg) throws Exception;

  /**
   * Implement to process byte arrays.
   *
   * @param <AGBM>
   *          the generic type
   * @param ctx
   *          the ctx
   * @param msg
   *          the msg
   * @throws Exception
   *           the exception
   */
  <AGBM extends AbstractGameBootMessage> void process(C ctx, byte[] msg) throws Exception;

  /**
   * Implement to process the converted {@link AbstractGameBootMessage}.
   *
   * @param <AGBM>
   *          the generic type
   * @param ctx
   *          the ctx
   * @param controller
   *          the controller
   * @param agbm
   *          the agbm
   * @return the response
   * @throws Exception
   *           the exception
   */
  <AGBM extends AbstractGameBootMessage> Response process(C ctx, GameBootMessageController controller, AGBM agbm)
      throws Exception;

  /**
   * Send the {@link Response} to the client.
   *
   * @param ctx
   *          the ctx
   * @param msg
   *          the msg
   * @param response
   *          the response
   * @throws Exception
   *           the exception
   */
  void sendMessage(C ctx, Object msg, Response response) throws Exception;

  /**
   * Implement to perform any pre processing logic.
   *
   * @param <AGBM>
   *          the generic type
   * @param ctx
   *          the ctx
   * @param agbm
   *          the agbm
   * @return true, if successful
   * @throws Exception
   *           the exception
   */
  <AGBM extends AbstractGameBootMessage> boolean preProcess(C ctx, AGBM agbm) throws Exception;

  /**
   * Implement to perform any post processing logic.
   *
   * @param <AGBM>
   *          the generic type
   * @param ctx
   *          the ctx
   * @param agbm
   *          the agbm
   * @param r
   *          the r
   */
  <AGBM extends AbstractGameBootMessage> void postProcess(C ctx, AGBM agbm, Response r);

  /**
   * Invoke to create a {@link Response} on catching a {@link GameBootThrowable}
   * .
   *
   * @param ctx
   *          the ctx
   * @param message
   *          the message
   * @param e
   *          the e
   * @return the response
   */
  Response fail(C ctx, AbstractGameBootMessage message, GameBootThrowable e);

  /**
   * Invoke on error supplying the code for the {@link ResponseContextLookup},
   * the message causing the error and any qualifying messages if required.
   *
   * @param rc
   *          the rc
   * @param message
   *          the message
   * @param payload
   *          the payload
   * @return the response
   */
  Response fail(ResponseContext rc, AbstractGameBootMessage message, Object... payload);

  /**
   * Send the unexpected error message.
   *
   * @param ctx
   *          the ctx
   */
  void sendUnexpectedError(C ctx);

  /**
   * Send a {@link GameBootThrowable} error.
   *
   * @param ctx
   *          the ctx
   * @param e
   *          the e
   */
  void sendError(C ctx, GameBootThrowable e);

  /**
   * Send supplying the code for the {@link ResponseContextLookup} and the
   * String representation of the message causing the error.
   *
   * @param rc
   *          the rc
   * @param ctx
   *          the ctx
   * @param message
   *          the message
   */
  void sendError(ResponseContext rc, C ctx, String message);

  /**
   * Gets the system id.
   *
   * @param ctx
   *          the ctx
   * @return the system id
   */
  SystemIdWrapper getSystemId(C ctx);

  /**
   * Gets the locale.
   *
   * @param ctx
   *          the ctx
   * @return the locale
   * @see ResponseContextLookup
   * @see ResponseContextLoader
   */
  Locale getLocale(C ctx);

}
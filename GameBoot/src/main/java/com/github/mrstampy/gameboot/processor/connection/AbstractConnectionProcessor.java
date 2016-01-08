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
package com.github.mrstampy.gameboot.processor.connection;

import java.lang.invoke.MethodHandles;
import java.util.Locale;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;

import com.github.mrstampy.gameboot.controller.GameBootMessageController;
import com.github.mrstampy.gameboot.exception.GameBootException;
import com.github.mrstampy.gameboot.exception.GameBootRuntimeException;
import com.github.mrstampy.gameboot.exception.GameBootThrowable;
import com.github.mrstampy.gameboot.locale.processor.LocaleRegistry;
import com.github.mrstampy.gameboot.messages.AbstractGameBootMessage;
import com.github.mrstampy.gameboot.messages.GameBootMessageConverter;
import com.github.mrstampy.gameboot.messages.Response;
import com.github.mrstampy.gameboot.messages.Response.ResponseCode;
import com.github.mrstampy.gameboot.messages.context.ResponseContext;
import com.github.mrstampy.gameboot.messages.context.ResponseContextLookup;
import com.github.mrstampy.gameboot.systemid.SystemIdKey;
import com.github.mrstampy.gameboot.util.GameBootUtils;

/**
 * The Class AbstractConnectionProcessor.
 *
 * @param <C>
 *          the generic type
 */
public abstract class AbstractConnectionProcessor<C> implements ConnectionProcessor<C> {
  private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Autowired
  private ResponseContextLookup lookup;

  @Autowired
  private LocaleRegistry localeRegistry;

  @Autowired
  private GameBootMessageConverter converter;

  @Autowired
  private GameBootUtils utils;

  /*
   * (non-Javadoc)
   * 
   * @see
   * com.github.mrstampy.gameboot.processor.connection.ConnectionProcessor#fail(
   * com.github.mrstampy.gameboot.messages.AbstractGameBootMessage,
   * com.github.mrstampy.gameboot.exception.GameBootThrowable)
   */
  @Override
  public Response fail(C ctx, AbstractGameBootMessage message, GameBootThrowable e) {
    ResponseContext error = getError(ctx, e);
    Object[] payload = e.getError() == null ? null : e.getPayload();

    Response r = new Response(message, ResponseCode.FAILURE, payload);
    r.setContext(error);

    return r;
  }

  private ResponseContext getError(C ctx, GameBootThrowable e) {
    if (e.getError() != null) return e.getError();
    if (e.getErrorCode() == null) return null;

    return getResponseContext(e.getErrorCode(), ctx, e.getPayload());
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * com.github.mrstampy.gameboot.processor.connection.ConnectionProcessor#fail(
   * int, com.github.mrstampy.gameboot.messages.AbstractGameBootMessage,
   * java.lang.Object[])
   */
  @Override
  public Response fail(ResponseContext rc, AbstractGameBootMessage message, Object... payload) {
    Response r = new Response(message, ResponseCode.FAILURE, payload);

    r.setContext(rc);

    return r;
  }

  /**
   * Gets the response context.
   *
   * @param code
   *          the code
   * @param ctx
   *          the ctx
   * @param parameters
   *          the parameters
   * @return the response context
   */
  protected ResponseContext getResponseContext(Integer code, C ctx, Object... parameters) {
    SystemIdKey systemId = getSystemId(ctx);
    Locale locale = systemId == null ? Locale.getDefault() : localeRegistry.get(systemId);
    return lookup.lookup(code, locale, parameters);
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.github.mrstampy.gameboot.processor.connection.ConnectionProcessor#
   * sendUnexpectedError(java.lang.Object)
   */
  public void sendUnexpectedError(C ctx) {
    sendError(getResponseContext(UNEXPECTED_ERROR, ctx), ctx, "An unexpected error has occurred");
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.github.mrstampy.gameboot.processor.connection.ConnectionProcessor#
   * getLocale()
   */
  public Locale getLocale(C ctx) {
    SystemIdKey systemId = getSystemId(ctx);
    if (!localeRegistry.contains(systemId)) localeRegistry.put(systemId, Locale.getDefault());
    return localeRegistry.get(systemId);
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * com.github.mrstampy.gameboot.netty.ConnectionProcessor#process(io.netty.
   * channel.WebSocketSession, java.lang.String)
   */
  @Override
  public <AGBM extends AbstractGameBootMessage> Response process(C ctx, String msg) throws Exception {
    if (StringUtils.isEmpty(msg)) return null;

    GameBootMessageController controller = utils.getBean(GameBootMessageController.class);

    Response response = null;
    AGBM agbm = null;
    String type = null;
    Integer id = null;
    try {
      agbm = converter.fromJson(msg);
      type = agbm.getType();
      id = agbm.getId();

      if (!preProcess(ctx, agbm)) return null;

      response = process(ctx, controller, agbm);
    } catch (GameBootException | GameBootRuntimeException e) {
      response = fail(ctx, agbm, e);
    } catch (Exception e) {
      log.error("Unexpected exception processing message type {}, id {} on channel {}", type, id, ctx, e);
      response = fail(getResponseContext(UNEXPECTED_ERROR, ctx), agbm, "An unexpected error has occurred");
    }

    postProcess(ctx, agbm, response);

    if (response == null) return null;

    String r = converter.toJson(response);

    sendMessage(ctx, r, response);

    return response;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * com.github.mrstampy.gameboot.netty.ConnectionProcessor#process(io.netty.
   * channel.WebSocketSession, byte[])
   */
  @Override
  public <AGBM extends AbstractGameBootMessage> Response process(C ctx, byte[] msg) throws Exception {
    if (msg == null || msg.length == 0) return null;

    GameBootMessageController controller = utils.getBean(GameBootMessageController.class);

    Response response = null;
    AGBM agbm = null;
    String type = null;
    Integer id = null;
    try {
      agbm = converter.fromJson(msg);
      type = agbm.getType();
      id = agbm.getId();

      if (!preProcess(ctx, agbm)) return null;

      response = process(ctx, controller, agbm);
    } catch (GameBootException | GameBootRuntimeException e) {
      response = fail(ctx, agbm, e);
    } catch (Exception e) {
      log.error("Unexpected exception processing message type {}, id {} on channel {}", type, id, ctx, e);
      response = fail(getResponseContext(UNEXPECTED_ERROR, ctx), agbm, "An unexpected error has occurred");
    }

    postProcess(ctx, agbm, response);

    if (response == null) return null;

    byte[] r = converter.toJsonArray(response);

    sendMessage(ctx, r, response);

    return response;
  }

  /**
   * Sets the system id in the {@link MDC}.
   * 
   * @param ctx
   */
  protected void setMDC(C ctx) {
    MDC.put("systemId", getSystemId(ctx).toString());
  }

}

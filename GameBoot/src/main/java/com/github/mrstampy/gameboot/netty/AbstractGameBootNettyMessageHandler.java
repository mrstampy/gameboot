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
package com.github.mrstampy.gameboot.netty;

import static org.apache.commons.lang3.StringUtils.isNotEmpty;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.mrstampy.gameboot.controller.GameBootMessageController;
import com.github.mrstampy.gameboot.exception.GameBootException;
import com.github.mrstampy.gameboot.exception.GameBootRuntimeException;
import com.github.mrstampy.gameboot.messages.AbstractGameBootMessage;
import com.github.mrstampy.gameboot.messages.Response;
import com.github.mrstampy.gameboot.messages.Response.ResponseCode;
import com.github.mrstampy.gameboot.metrics.MetricsHelper;
import com.github.mrstampy.gameboot.util.GameBootUtils;

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.concurrent.Future;

/**
 * This class is the superclass for last-in-pipeline GameBoot Netty handlers.
 * Messages are presumed to have been converted to JSON strings representing an
 * {@link AbstractGameBootMessage} and are processed by the
 * {@link GameBootMessageController}. Channels are added to the
 * {@link NettyConnectionRegistry#ALL} group. <br>
 * <br>
 * 
 * The {@link #inspect(ChannelHandlerContext, String)} method searches incoming
 * messages for 'userName' and 'sessionId' JSON nodes and if they exist they are
 * used to register the channel against the {@link NettyConnectionRegistry}.
 */
public abstract class AbstractGameBootNettyMessageHandler extends ChannelDuplexHandler {
  private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  /** The Constant MESSAGE_COUNTER. */
  protected static final String MESSAGE_COUNTER = "Netty Message Counter";

  /** The Constant FAILED_MESSAGE_COUNTER. */
  protected static final String FAILED_MESSAGE_COUNTER = "Netty Failed Message Counter";

  /** The Constant USER_NAME. */
  public static final String USER_NAME = "userName";

  /** The Constant SESSION_ID. */
  public static final String SESSION_ID = "sessionId";

  @Autowired
  private ObjectMapper mapper;

  @Autowired
  private MetricsHelper helper;

  @Autowired
  private GameBootUtils utils;

  @Autowired
  private NettyConnectionRegistry registry;

  /** The user name. */
  protected String userName;

  /** The session id. */
  protected Long sessionId;

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
   * @see io.netty.channel.ChannelInboundHandlerAdapter#channelActive(io.netty.
   * channel.ChannelHandlerContext)
   */
  @Override
  public void channelActive(ChannelHandlerContext ctx) throws Exception {
    log.info("Connected to {}", ctx.channel());
    registry.putInGroup(NettyConnectionRegistry.ALL, ctx.channel());
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * io.netty.channel.ChannelInboundHandlerAdapter#channelInactive(io.netty.
   * channel.ChannelHandlerContext)
   */
  @Override
  public void channelInactive(ChannelHandlerContext ctx) throws Exception {
    log.info("Disconnected from {}", ctx.channel());

    userName = null;
    sessionId = null;
    mapper = null;
    helper = null;
    registry = null;
    utils = null;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * io.netty.channel.ChannelInboundHandlerAdapter#exceptionCaught(io.netty.
   * channel.ChannelHandlerContext, java.lang.Throwable)
   */
  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
    log.error("Unexpected error on {}, closing channel", ctx.channel(), cause);

    ctx.disconnect();
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * io.netty.channel.ChannelInboundHandlerAdapter#channelRead(io.netty.channel.
   * ChannelHandlerContext, java.lang.Object)
   */
  @Override
  public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
    helper.incr(MESSAGE_COUNTER);

    log.debug("Received message {} on {}", msg, ctx.channel());

    inspect(ctx, (String) msg);

    channelReadImpl(ctx, (String) msg);
  }

  /**
   * Channel read impl.
   *
   * @param ctx
   *          the ctx
   * @param msg
   *          the msg
   * @throws Exception
   *           the exception
   */
  protected abstract void channelReadImpl(ChannelHandlerContext ctx, String msg) throws Exception;

  /**
   * Process, should be invoked from
   * {@link #channelReadImpl(ChannelHandlerContext, String)}.
   *
   * @param ctx
   *          the ctx
   * @param msg
   *          the msg
   * @throws Exception
   *           the exception
   */
  protected void process(ChannelHandlerContext ctx, String msg) throws Exception {
    GameBootMessageController controller = utils.getBean(GameBootMessageController.class);

    String response = null;
    try {
      response = controller.process(msg);
    } catch (GameBootException | GameBootRuntimeException e) {
      helper.incr(FAILED_MESSAGE_COUNTER);
      response = fail(e.getMessage());
    } catch (Exception e) {
      helper.incr(FAILED_MESSAGE_COUNTER);
      log.error("Unexpected exception processing message {} on channel {}", msg, ctx.channel(), e);
      response = fail("An unexpected error has occurred");
    }

    if (response == null) return;

    ChannelFuture f = ctx.channel().writeAndFlush(response);
    try {
      f.await(5, TimeUnit.SECONDS);

      log(f, msg, response, ctx);
    } catch (InterruptedException e) {
      log.error("Sending response {} for message {} on {} was interrupted", response, msg, ctx.channel(), e);
    }
  }

  private void log(Future<? super Void> f, String msg, String response, ChannelHandlerContext ctx) {
    if (f.isSuccess()) {
      log.debug("Successfully sent {} for message {} to {}", response, msg, ctx.channel());
    } else {
      log.error("Could not send {} for message {} to {}", response, msg, ctx.channel(), f.cause());
    }
  }

  /**
   * Inspect, setting {@link #userName} and {@link #sessionId} and registering
   * the channel with the {@link NettyConnectionRegistry}.
   *
   * @param ctx
   *          the ctx
   * @param msg
   *          the msg
   * @see NettyConnectionRegistry#put(Long, io.netty.channel.Channel)
   * @see NettyConnectionRegistry#put(String, io.netty.channel.Channel)
   */
  protected void inspect(ChannelHandlerContext ctx, String msg) {
    if (isNotEmpty(userName) && sessionId != null) return;

    JsonNode node;
    try {
      node = mapper.readTree(msg);
    } catch (IOException e) {
      log.error("Unexpected exception processing message {} on {}", msg, ctx.channel(), e);
      return;
    }

    if (userName == null && hasValue(node, USER_NAME)) {
      userName = node.get(USER_NAME).asText();

      registry.put(userName, ctx.channel());
    }

    if (sessionId == null && hasValue(node, SESSION_ID)) {
      sessionId = node.get(SESSION_ID).asLong();

      registry.put(sessionId, ctx.channel());
    }
  }

  /**
   * Checks the node for the specified key, as existing and non-mt text.
   *
   * @param node
   *          the node
   * @param key
   *          the key
   * @return true, if successful
   */
  protected boolean hasValue(JsonNode node, String key) {
    return node.has(key) && isNotEmpty(node.get(key).asText());
  }

  /**
   * Returns a fail message.
   *
   * @param message
   *          the message
   * @return the string
   */
  protected String fail(String message) {
    try {
      return mapper.writeValueAsString(new Response(ResponseCode.FAILURE, message));
    } catch (JsonProcessingException e) {
      log.error("Unexpected exception", e);
      throw new RuntimeException("Unexpected JSON error", e);
    }
  }

}

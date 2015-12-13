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

import java.lang.invoke.MethodHandles;
import java.net.InetSocketAddress;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.github.mrstampy.gameboot.concurrent.SystemId;
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

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.concurrent.Future;

/**
 * This class is the superclass for last-in-pipeline GameBoot Netty handlers.
 * Messages are presumed to have been converted to JSON strings representing an
 * {@link AbstractGameBootMessage} and are processed by the
 * {@link GameBootMessageController}. Channels are added to the
 * {@link NettyConnectionRegistry#ALL} group and registering the channel against
 * the {@link SystemId#next()} value obtained on connection. (The
 * {@link AbstractGameBootNettyMessageHandler#channelActive(ChannelHandlerContext)}
 * and
 * {@link AbstractGameBootNettyMessageHandler#channelInactive(ChannelHandlerContext)}
 * must be called by subclasses overriding them.) <br>
 * <br>
 * 
 * Subclasses should have an annotated {@link PostConstruct} method which calls
 * the {@link AbstractGameBootNettyMessageHandler#postConstruct()}.<br>
 * <br>
 * 
 * @see GameBootMessageController
 * 
 */
public abstract class AbstractGameBootNettyMessageHandler extends ChannelDuplexHandler {
  private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  /** The Constant MESSAGE_COUNTER. */
  protected static final String MESSAGE_COUNTER = "Netty Message Counter";

  /** The Constant FAILED_MESSAGE_COUNTER. */
  protected static final String FAILED_MESSAGE_COUNTER = "Netty Failed Message Counter";

  @Autowired
  private MetricsHelper helper;

  @Autowired
  private GameBootUtils utils;

  @Autowired
  private NettyConnectionRegistry registry;

  @Autowired
  private GameBootMessageConverter converter;

  @Autowired
  private SystemId generator;

  private Long key;

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
   * Subclasses overriding this method should remember to invoke it with a call
   * to 'super.'.
   *
   * @param ctx
   *          the ctx
   * @throws Exception
   *           the exception
   */
  @Override
  public void channelActive(ChannelHandlerContext ctx) throws Exception {
    this.key = generator.next();

    log.info("Connected to {}, adding to registry with key {}", ctx.channel(), key);

    registry.putInGroup(NettyConnectionRegistry.ALL, ctx.channel());
    registry.put(key, ctx.channel());
  }

  /**
   * Subclasses overriding this method should remember to invoke it with a call
   * to 'super.'.
   *
   * @param ctx
   *          the ctx
   * @throws Exception
   *           the exception
   */
  @Override
  public void channelInactive(ChannelHandlerContext ctx) throws Exception {
    log.info("Disconnected from {}", ctx.channel());

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

    if (msg instanceof String) {
      channelReadImpl(ctx, (String) msg);
    } else if (msg instanceof byte[]) {
      channelReadImpl(ctx, (byte[]) msg);
    } else {
      log.error("Only strings or byte arrays: {} from {}. Disconnecting", msg.getClass(), ctx.channel());
      ctx.close();
    }
  }

  /**
   * Channel read impl, blank by default. Override to handle (or exclude) byte
   * array messages.
   *
   * @param ctx
   *          the ctx
   * @param msg
   *          the msg
   * @throws Exception
   *           the exception
   */
  protected void channelReadImpl(ChannelHandlerContext ctx, byte[] msg) throws Exception {
  }

  /**
   * Channel read impl, override to handle (or exclude) string messages.
   *
   * @param ctx
   *          the ctx
   * @param msg
   *          the msg
   * @throws Exception
   *           the exception
   */
  protected void channelReadImpl(ChannelHandlerContext ctx, String msg) throws Exception {
  }

  /**
   * Process, should be invoked from
   * {@link #channelReadImpl(ChannelHandlerContext, String)} or
   * {@link #channelReadImpl(ChannelHandlerContext, byte[])}.
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
  protected <AGBM extends AbstractGameBootMessage> void process(ChannelHandlerContext ctx, String msg)
      throws Exception {
    GameBootMessageController controller = utils.getBean(GameBootMessageController.class);

    String response = null;
    try {
      AGBM agbm = converter.fromJson(msg);

      Response r = process(ctx, msg, controller, agbm);
      if (r != null) response = converter.toJson(r);
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

    String r = response;

    f.addListener(e -> log(e, msg, r, ctx));
  }

  /**
   * Process, can be invoked in lieu of
   * {@link #process(ChannelHandlerContext, String)} should the message have
   * been {@link GameBootMessageConverter}'ed for inspection in an override of
   * {@link #channelReadImpl(ChannelHandlerContext, byte[])} or
   * {@link #channelReadImpl(ChannelHandlerContext, String)}. If invoking this
   * method directly ensure that the instance of
   * {@link GameBootMessageController} is obtained via
   * {@link GameBootUtils#getBean(Class)}.
   *
   * @param <AGBM>
   *          the generic type
   * @param ctx
   *          the ctx
   * @param msg
   *          the msg
   * @param controller
   *          the controller
   * @param agbm
   *          the agbm
   * @return the response
   * @throws Exception
   *           the exception
   * @throws JsonProcessingException
   *           the json processing exception
   * @throws GameBootException
   *           the game boot exception
   */
  protected <AGBM extends AbstractGameBootMessage> Response process(ChannelHandlerContext ctx, String msg,
      GameBootMessageController controller, AGBM agbm) throws Exception, JsonProcessingException, GameBootException {
    if (!investigate(ctx, agbm)) return null;

    if (agbm.getSystemId() == null) agbm.setSystemId(getKey());
    agbm.setTransport(Transport.NETTY);
    agbm.setLocal((InetSocketAddress) ctx.channel().localAddress());
    agbm.setRemote((InetSocketAddress) ctx.channel().remoteAddress());

    Response r = controller.process(msg, agbm);
    r.setSystemId(agbm.getSystemId());

    return r;
  }

  /**
   * Investigate the message prior to processing. Overrides which fail
   * inspection are responsible for sending any failure messages to the client
   * prior to returning false.
   *
   * @param <AGBM>
   *          the generic type
   * @param ctx
   *          the ctx
   * @param agbm
   *          the agbm
   * @return true, if successful
   */
  protected <AGBM extends AbstractGameBootMessage> boolean investigate(ChannelHandlerContext ctx, AGBM agbm) {
    return true;
  }

  /**
   * Send unexpected error.
   *
   * @param ctx
   *          the ctx
   */
  protected void sendUnexpectedError(ChannelHandlerContext ctx) {
    sendError(ctx, "An unexpected error has occurred");
  }

  /**
   * Send error.
   *
   * @param ctx
   *          the ctx
   * @param message
   *          the message
   */
  protected void sendError(ChannelHandlerContext ctx, String message) {
    try {
      ctx.channel().writeAndFlush(fail(message));
    } catch (Exception e) {
      log.error("Unexpected exception", e);
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
      throw new GameBootException("Unexpected JSON error", e);
    }
  }

  /**
   * Gets the key set in {@link #channelActive(ChannelHandlerContext)} from
   * {@link SystemId#next()}.
   *
   * @return the key
   */
  protected Long getKey() {
    return key;
  }

}

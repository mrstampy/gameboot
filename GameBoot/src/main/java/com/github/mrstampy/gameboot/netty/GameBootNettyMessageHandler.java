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

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.mrstampy.gameboot.concurrent.FiberCreator;
import com.github.mrstampy.gameboot.controller.GameBootMessageController;
import com.github.mrstampy.gameboot.exception.GameBootException;
import com.github.mrstampy.gameboot.exception.GameBootRuntimeException;
import com.github.mrstampy.gameboot.messages.AbstractGameBootMessage;
import com.github.mrstampy.gameboot.messages.Response;
import com.github.mrstampy.gameboot.messages.Response.ResponseCode;
import com.github.mrstampy.gameboot.metrics.MetricsHelper;
import com.github.mrstampy.gameboot.util.GameBootUtils;

import co.paralleluniverse.fibers.Fiber;
import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.fibers.Suspendable;
import co.paralleluniverse.strands.SuspendableCallable;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.DefaultChannelPromise;

/**
 * This class is intended to be added last to the {@link ChannelPipeline}
 * created for Netty sockets. The inbound message will have been converted to a
 * JSON string representing any {@link AbstractGameBootMessage}. The
 * {@link GameBootMessageController} is used to process the message and return
 * the result.<br<br>
 * 
 * Do not instantiate directly as this is a prototype Spring managed bean. Use
 * {@link GameBootUtils#getBean(Class)} to obtain a unique instance when
 * constructing the {@link ChannelPipeline}.
 */
@Component
@Scope("prototype")
public class GameBootNettyMessageHandler extends ChannelDuplexHandler {
  private static final String USER_NAME = "userName";

  private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private static final String MESSAGE_COUNTER = "Netty Message Counter";

  private static final String FAILED_MESSAGE_COUNTER = "Netty Failed Message Counter";

  /** Logback {@link MDC} key for local address (nettyLocal). */
  public static final String LOCAL_ADDRESS = "nettyLocal";

  /** Logback {@link MDC} key for remote address (nettyRemote). */
  public static final String REMOTE_ADDRESS = "nettyRemote";

  private static final String SESSION_ID = null;

  @Autowired
  private ObjectMapper mapper;

  @Autowired
  private MetricsHelper helper;

  @Autowired
  private FiberCreator creator;

  @Autowired
  private GameBootUtils utils;

  @Autowired
  private NettyConnectionRegistry registry;

  private String userName;

  private Long sessionId;

  /**
   * Post construct.
   *
   * @throws Exception
   *           the exception
   */
  @PostConstruct
  public void postConstruct() throws Exception {
    helper.counter(MESSAGE_COUNTER, GameBootNettyMessageHandler.class, "inbound", "messages");
    helper.counter(FAILED_MESSAGE_COUNTER, GameBootNettyMessageHandler.class, "failed", "messages");
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

    if (isNotEmpty(userName)) registry.remove(userName);

    if (sessionId != null) registry.remove(sessionId);

    userName = null;
    sessionId = null;
    mapper = null;
    helper = null;
    creator = null;
    registry = null;
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
  @SuppressWarnings("serial")
  public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
    helper.incr(MESSAGE_COUNTER);

    log.debug("Received message {} on {}", msg, ctx.channel());

    inspect(ctx, (String) msg);

    Fiber<Void> fiber = creator.newFiberForkJoin(new SuspendableCallable<Void>() {

      @Override
      public Void run() throws SuspendExecution, InterruptedException {
        process(ctx, (String) msg);

        return null;
      }
    });

    fiber.start();
  }

  /**
   * Exposed for instrumentation.
   *
   * @param ctx
   *          the ctx
   * @param msg
   *          the msg
   */
  @Suspendable
  public void process(ChannelHandlerContext ctx, String msg) {
    initMDC(ctx);

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

    try {
      write(ctx, response, new DefaultChannelPromise(ctx.channel()));
    } catch (Exception e) {
      log.error("Unexpected exception processing message {} with response {} on channel {}",
          msg,
          response,
          ctx.channel(),
          e);
    } finally {
      clearMDC();
    }
  }

  private void inspect(ChannelHandlerContext ctx, String msg) {
    if (isNotEmpty(userName) && sessionId != null) return;

    JsonNode node;
    try {
      node = mapper.readTree(msg);
    } catch (IOException e) {
      log.error("Unexpected exception processing message {} on {}", msg, ctx.channel(), e);
      return;
    }

    if (hasValue(node, USER_NAME)) {
      userName = node.get(USER_NAME).asText();

      registry.put(msg, ctx.channel());
    }

    if (hasValue(node, SESSION_ID)) {
      sessionId = node.get(SESSION_ID).asLong();

      registry.put(sessionId, ctx.channel());
    }
  }

  private boolean hasValue(JsonNode node, String key) {
    return node.has(key) && isNotEmpty(node.get(key).asText());
  }

  private void initMDC(ChannelHandlerContext ctx) {
    MDC.put(REMOTE_ADDRESS, ctx.channel().remoteAddress().toString());
    MDC.put(LOCAL_ADDRESS, ctx.channel().localAddress().toString());
  }

  private void clearMDC() {
    MDC.remove(REMOTE_ADDRESS);
    MDC.remove(LOCAL_ADDRESS);
  }

  private String fail(String message) {
    try {
      return mapper.writeValueAsString(new Response(ResponseCode.FAILURE, message));
    } catch (JsonProcessingException e) {
      log.error("Unexpected exception", e);
      throw new RuntimeException("Unexpected JSON error", e);
    }
  }

}

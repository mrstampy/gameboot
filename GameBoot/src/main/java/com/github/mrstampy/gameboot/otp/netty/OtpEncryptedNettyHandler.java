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
package com.github.mrstampy.gameboot.otp.netty;

import java.lang.invoke.MethodHandles;
import java.util.concurrent.ExecutorService;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Profile;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.github.mrstampy.gameboot.concurrent.GameBootConcurrentConfiguration;
import com.github.mrstampy.gameboot.controller.GameBootMessageController;
import com.github.mrstampy.gameboot.exception.GameBootException;
import com.github.mrstampy.gameboot.exception.GameBootRuntimeException;
import com.github.mrstampy.gameboot.messages.AbstractGameBootMessage;
import com.github.mrstampy.gameboot.messages.GameBootMessageConverter;
import com.github.mrstampy.gameboot.messages.Response;
import com.github.mrstampy.gameboot.netty.AbstractGameBootNettyMessageHandler;
import com.github.mrstampy.gameboot.netty.NettyConnectionRegistry;
import com.github.mrstampy.gameboot.otp.OtpConfiguration;
import com.github.mrstampy.gameboot.otp.messages.OtpKeyRequest;
import com.github.mrstampy.gameboot.otp.messages.OtpKeyRequest.KeyFunction;
import com.github.mrstampy.gameboot.otp.messages.OtpMessage;
import com.github.mrstampy.gameboot.otp.messages.OtpNewKeyAck;
import com.github.mrstampy.gameboot.util.GameBootUtils;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.ssl.SslHandler;
import io.netty.util.concurrent.Future;

/**
 * The Class OtpEncryptedNettyInboundHandler is the last handler added in a
 * pipeline intended to process {@link OtpMessage}s. The connection must be
 * encrypted sending byte arrays as messages. Should these conditions fail the
 * connection will be terminated.<br>
 * <br>
 * 
 * The client connects to the socket containing this handler in the pipeline and
 * sends a message of type {@link OtpKeyRequest}. The
 * {@link OtpKeyRequest#getSystemId()} value will have been set in the client as
 * the value obtained from the clear connection containing the
 * {@link OtpClearNettyHandler} in the pipeline.<br>
 * <br>
 * 
 * If the key generation is successful a {@link Response} object is returned
 * containing the key as the only element of the {@link Response#getResponse()}
 * array. The client then sends a message of type {@link OtpNewKeyAck}. When
 * received the GameBoot server activates the new key for all traffic on the
 * {@link OtpClearNettyHandler} channel and disconnects this connection.<br>
 * <br>
 * 
 * Should any failures occur the old key, should it exist, is considered active.
 * <br>
 * <br>
 * 
 * Do not instantiate directly as this is a prototype Spring managed bean. Use
 * {@link GameBootUtils#getBean(Class)} to obtain a unique instance when
 * constructing the {@link ChannelPipeline}.
 * 
 */
@Component
@Scope("prototype")
@Profile(OtpConfiguration.OTP_PROFILE)
public class OtpEncryptedNettyHandler extends AbstractGameBootNettyMessageHandler {
  private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Autowired
  private GameBootMessageConverter converter;

  @Autowired
  @Qualifier(GameBootConcurrentConfiguration.GAME_BOOT_EXECUTOR)
  private ExecutorService svc;

  @Autowired
  private NettyConnectionRegistry registry;

  @Autowired
  private GameBootUtils utils;

  /**
   * Post construct.
   *
   * @throws Exception
   *           the exception
   */
  @PostConstruct
  public void postConstruct() throws Exception {
    super.postConstruct();
  }

  /*
   * (non-Javadoc)
   * 
   * @see io.netty.channel.ChannelInboundHandlerAdapter#channelActive(io.netty.
   * channel.ChannelHandlerContext)
   */
  @Override
  public void channelActive(ChannelHandlerContext ctx) throws Exception {
    SslHandler handler = ctx.pipeline().get(SslHandler.class);

    if (handler == null) {
      log.error("Unencrypted channels cannot process OTP messages.  Disconnecting {}", ctx.channel());
      ctx.close();
      return;
    }

    handler.handshakeFuture().addListener(f -> validate(f, ctx));
  }

  private void validate(Future<? super Channel> f, ChannelHandlerContext ctx) {
    if (f.isSuccess()) {
      log.debug("Handshake successful with {}", ctx.channel());
      try {
        super.channelActive(ctx);
      } catch (Exception e) {
        log.error("Unexpected exception", e);
      }
    } else {
      log.error("Handshake unsuccessful, disconnecting {}", ctx.channel(), f.cause());
      ctx.close();
    }
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
    super.channelInactive(ctx);

    svc = null;
    converter = null;
    registry = null;
    utils = null;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * com.github.mrstampy.gameboot.netty.AbstractGameBootNettyMessageHandler#
   * channelReadImpl(io.netty.channel.ChannelHandlerContext, byte[])
   */
  @Override
  protected void channelReadImpl(ChannelHandlerContext ctx, byte[] msg) throws Exception {
    svc.execute(() -> {
      try {
        processOtpMessage(ctx, msg);
      } catch (GameBootException | GameBootRuntimeException e) {
        sendError(ctx, e);
      } catch (Exception e) {
        log.error("Unexpected exception", e);
        sendUnexpectedError(ctx);
      }
    });
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * com.github.mrstampy.gameboot.netty.AbstractGameBootNettyMessageHandler#
   * inspect(io.netty.channel.ChannelHandlerContext,
   * com.github.mrstampy.gameboot.messages.AbstractGameBootMessage)
   */
  protected <AGBM extends AbstractGameBootMessage> boolean inspect(ChannelHandlerContext ctx, AGBM agbm) {
    boolean ok = agbm instanceof OtpKeyRequest && KeyFunction.NEW == ((OtpKeyRequest) agbm).getKeyFunction();

    if (!ok) {
      log.error("Unexpected message received, disconnecting: {}", agbm);
      ctx.close();
    }

    return ok;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * com.github.mrstampy.gameboot.netty.AbstractGameBootNettyMessageHandler#
   * channelReadImpl(io.netty.channel.ChannelHandlerContext, java.lang.String)
   */
  protected void channelReadImpl(ChannelHandlerContext ctx, String msg) throws Exception {
    log.error("Received text; binary only.  Closing channel {}", ctx.channel());

    ctx.close();
  }

  private void processOtpMessage(ChannelHandlerContext ctx, byte[] msg) throws Exception {
    OtpMessage message = null;
    try {
      message = converter.fromJson(msg);
    } catch (Exception e) {
      log.error("Message received on {} not an OTP message, disconnecting", ctx.channel());
      ctx.close();
      return;
    }

    if (!validateChannel(ctx, message)) return;

    GameBootMessageController controller = utils.getBean(GameBootMessageController.class);

    Response r = process(ctx, controller, message);

    if (r == null) return;

    String type = message.getType();
    ChannelFuture cf = ctx.channel().writeAndFlush(converter.toJsonArray(r));

    cf.addListener(f -> log(f, ctx, type));
    cf.addListener(f -> ctx.close());
  }

  /**
   * Validates that the clear channel exists. Override to provide additional
   * validation.
   *
   * @param ctx
   *          the ctx
   * @param message
   *          the message
   * @return true, if successful
   */
  protected boolean validateChannel(ChannelHandlerContext ctx, OtpMessage message) {
    Long systemId = message.getSystemId();
    Channel clearChannel = registry.get(systemId);

    if (clearChannel == null || !clearChannel.isActive()) {
      log.error("No clear channel for {}, from encrypted channel {}, disconnecting", systemId, ctx.channel());
      ctx.close();
      return false;
    }

    return true;
  }

  private void log(Future<? super Void> f, ChannelHandlerContext ctx, String type) {
    if (f.isSuccess()) {
      log.debug("Successful send of {} to {}", type, ctx.channel().remoteAddress());
    } else {
      log.error("Unsuccessful send of {} to {}", type, ctx.channel().remoteAddress(), f.cause());
    }
  }

}

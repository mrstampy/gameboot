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
package com.github.mrstampy.gameboot.otp.netty;

import java.lang.invoke.MethodHandles;
import java.net.InetSocketAddress;
import java.util.concurrent.ExecutorService;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.github.mrstampy.gameboot.concurrent.GameBootConcurrentConfiguration;
import com.github.mrstampy.gameboot.controller.GameBootMessageController;
import com.github.mrstampy.gameboot.exception.GameBootException;
import com.github.mrstampy.gameboot.exception.GameBootRuntimeException;
import com.github.mrstampy.gameboot.messages.GameBootMessageConverter;
import com.github.mrstampy.gameboot.messages.Response;
import com.github.mrstampy.gameboot.netty.AbstractGameBootNettyMessageHandler;
import com.github.mrstampy.gameboot.netty.NettyConnectionRegistry;
import com.github.mrstampy.gameboot.otp.messages.OtpMessage;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.ssl.SslHandler;
import io.netty.util.concurrent.Future;

/**
 * The Class OtpEncryptedNettyInboundHandler.
 */
@Component
@Scope("prototype")
public class OtpEncryptedNettyInboundHandler extends AbstractGameBootNettyMessageHandler {
  private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Autowired
  private GameBootMessageConverter converter;

  @Autowired
  private GameBootMessageController controller;

  @Autowired
  @Qualifier(GameBootConcurrentConfiguration.GAME_BOOT_EXECUTOR)
  private ExecutorService svc;

  @Autowired
  private NettyConnectionRegistry registry;

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

    super.channelActive(ctx);
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
        sendError(ctx, e.getMessage());
      } catch (Exception e) {
        log.error("Unexpected exception", e);
        sendError(ctx, "An unexpected error has occurred");
      }
    });
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

    Response r = controller.process(new String(msg), message);
    if (r == null) return;

    String type = message.getType();
    ChannelFuture cf = ctx.channel().writeAndFlush(converter.toJsonArray(r));

    cf.addListener(f -> log(f, ctx, type));
  }

  private boolean validateChannel(ChannelHandlerContext ctx, OtpMessage message) {
    Long systemId = message.getSystemId();
    Channel clearChannel = registry.get(systemId);

    if (clearChannel == null || !clearChannel.isActive()) {
      log.info("No clear channel for {}, from encrypted channel {}", systemId, ctx.channel());
      return true;
    }

    String encryptedHost = getRemote(ctx.channel());
    String clearHost = getRemote(clearChannel);

    if (encryptedHost.equals(clearHost)) return true;

    log.error("OTP request type {} from {} does not match host {} using system id {}, disconnecting.",
        message.getType(),
        ctx.channel(),
        clearChannel,
        systemId);

    ctx.close();

    return false;
  }

  private String getRemote(Channel channel) {
    return ((InetSocketAddress) channel.remoteAddress()).getAddress().getHostAddress();
  }

  private void log(Future<? super Void> f, ChannelHandlerContext ctx, String type) {
    if (f.isSuccess()) {
      log.debug("Successful send of {} to {}", type, ctx.channel().remoteAddress());
    } else {
      log.error("Unsuccessful send of {} to {}", type, ctx.channel().remoteAddress(), f.cause());
    }
  }

}

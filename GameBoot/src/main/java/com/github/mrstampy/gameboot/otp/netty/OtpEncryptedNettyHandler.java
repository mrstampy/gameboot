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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Profile;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.github.mrstampy.gameboot.concurrent.GameBootConcurrentConfiguration;
import com.github.mrstampy.gameboot.exception.GameBootException;
import com.github.mrstampy.gameboot.messages.GameBootMessageConverter;
import com.github.mrstampy.gameboot.messages.Response;
import com.github.mrstampy.gameboot.netty.NettyConnectionRegistry;
import com.github.mrstampy.gameboot.otp.OtpConfiguration;
import com.github.mrstampy.gameboot.otp.messages.OtpKeyRequest;
import com.github.mrstampy.gameboot.otp.messages.OtpMessage;
import com.github.mrstampy.gameboot.otp.messages.OtpNewKeyAck;
import com.github.mrstampy.gameboot.otp.processor.OtpKeyRequestProcessor;
import com.github.mrstampy.gameboot.systemid.SystemIdWrapper;
import com.github.mrstampy.gameboot.util.GameBootUtils;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.SimpleChannelInboundHandler;
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
 * containing the key as the only element of the {@link Response#getPayload()}
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
public class OtpEncryptedNettyHandler extends SimpleChannelInboundHandler<byte[]> {
  private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Autowired
  private GameBootMessageConverter converter;

  @Autowired
  @Qualifier(GameBootConcurrentConfiguration.GAME_BOOT_EXECUTOR)
  private ExecutorService svc;

  @Autowired
  private OtpKeyRequestProcessor processor;

  @Autowired
  private NettyConnectionRegistry registry;

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
      log.error("Unencrypted channels cannot process OTP New Key requests.  Disconnecting {}", ctx.channel());
      ctx.close();
      return;
    }

    handler.handshakeFuture().addListener(f -> validate(f, ctx));
  }

  private void validate(Future<? super Channel> f, ChannelHandlerContext ctx) {
    if (f.isSuccess()) {
      log.debug("Handshake successful with {}", ctx.channel());
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
    svc = null;
    converter = null;
    processor = null;
    registry = null;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * io.netty.channel.SimpleChannelInboundHandler#channelRead0(io.netty.channel.
   * ChannelHandlerContext, java.lang.Object)
   */
  @Override
  protected void channelRead0(ChannelHandlerContext ctx, byte[] msg) throws Exception {
    svc.execute(() -> {
      try {
        processImpl(ctx, msg);
      } catch (Exception e) {
        log.error("Unexpected exception, closing OTP New Key channel {}", ctx.channel(), e);
        ctx.close();
      }
    });
  }

  private void processImpl(ChannelHandlerContext ctx, byte[] msg) throws Exception {
    OtpKeyRequest message = convertAndValidate(ctx, msg);

    if (message == null) return;

    Response r = processor.process(message);

    if (r == null || !r.isSuccess()) {
      log.error("New Key generation for {} failed with {}", message, r);
      ctx.close();
      return;
    }

    sendResponse(ctx, message, r);
  }

  private void sendResponse(ChannelHandlerContext ctx, OtpMessage message, Response r)
      throws JsonProcessingException, GameBootException {
    ChannelFuture cf = ctx.writeAndFlush(converter.toJsonArray(r));

    cf.addListener(f -> log(f, ctx, message.getType()));
  }

  private OtpKeyRequest convertAndValidate(ChannelHandlerContext ctx, byte[] msg) throws Exception {
    OtpKeyRequest message = converter.fromJson(msg);

    switch (message.getKeyFunction()) {
    case NEW:
      break;
    default:
      log.error("Cannot process {}, closing OTP New Key channel {}", message, ctx.channel());
      ctx.close();
      return null;
    }

    Long systemId = message.getOtpSystemId();
    if (systemId == null) {
      log.error("System id missing from {}, disconnecting {}", message, ctx.channel());
      ctx.close();
      return null;
    }

    SystemIdWrapper siw = new SystemIdWrapper(systemId);

    Channel clearChannel = registry.get(siw);
    if (clearChannel == null || !clearChannel.isActive()) {
      log.error("No clear channel for {}, from encrypted channel {}, disconnecting", systemId, ctx.channel());
      ctx.close();
      return null;
    }

    return message;
  }

  private void log(Future<? super Void> f, ChannelHandlerContext ctx, String type) {
    if (f.isSuccess()) {
      log.debug("Successful send of {} to {}, closing channel", type, ctx.channel().remoteAddress());
    } else {
      log.error("Unsuccessful send of {} to {}, closing channel", type, ctx.channel().remoteAddress(), f.cause());
    }

    ctx.close();
  }

}

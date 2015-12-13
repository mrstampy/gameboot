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
import java.util.concurrent.ExecutorService;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.github.mrstampy.gameboot.concurrent.GameBootConcurrentConfiguration;
import com.github.mrstampy.gameboot.exception.GameBootException;
import com.github.mrstampy.gameboot.exception.GameBootRuntimeException;
import com.github.mrstampy.gameboot.messages.AbstractGameBootMessage;
import com.github.mrstampy.gameboot.messages.GameBootMessageConverter;
import com.github.mrstampy.gameboot.messages.Response;
import com.github.mrstampy.gameboot.messages.Response.ResponseCode;
import com.github.mrstampy.gameboot.metrics.MetricsHelper;
import com.github.mrstampy.gameboot.netty.AbstractGameBootNettyMessageHandler;
import com.github.mrstampy.gameboot.otp.KeyRegistry;
import com.github.mrstampy.gameboot.otp.OneTimePad;
import com.github.mrstampy.gameboot.otp.messages.OtpMessage;
import com.github.mrstampy.gameboot.otp.messages.OtpNewKeyAck;
import com.github.mrstampy.gameboot.otp.messages.OtpNewKeyRequest;
import com.github.mrstampy.gameboot.util.GameBootUtils;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.ChannelPromise;

/**
 * The Class OtpClearNettyHandler is intended to provide a transparent means of
 * using the {@link OneTimePad} utility to encrypt outgoing and decrypt incoming
 * messages on unencrypted Netty connections. It is intended that the message is
 * a byte array at the point in which this class is inserted into the pipeline.
 * Inbound messages are later converted to strings, all outbound messages are
 * byte arrays.<br>
 * <br>
 * 
 * By default messages are unencrypted. An INFO message is sent to the client
 * containing the {@link Response#getSystemId()} value upon connection. The
 * client then creates a connection to the socket server containing the
 * {@link OtpEncryptedNettyHandler} in the pipeline and sends a message of type
 * {@link OtpNewKeyRequest} thru it to the server. The
 * {@link OtpNewKeyRequest#getSystemId()} value will have been set in the client
 * as the value obtained from the clear connection's INFO message.<br>
 * <br>
 * 
 * If the key generation is successful a {@link Response} object is returned in
 * the encrypted channel containing the new OTP key as the only element of the
 * {@link Response#getResponse()} array. The client then sends a message of type
 * {@link OtpNewKeyAck} in the encrypted channel. When received the GameBoot
 * server activates the new key for all traffic on the
 * {@link OtpClearNettyHandler} channel and disconnects the encrypted
 * connection.<br>
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
 * @see KeyRegistry
 * @see OneTimePad
 */
@Component
@Scope("prototype")
public class OtpClearNettyHandler extends AbstractGameBootNettyMessageHandler {
  private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private static final String OTP_DECRYPT_COUNTER = "Netty OTP Decrypt Counter";

  private static final String OTP_ENCRYPT_COUNTER = "Netty OTP Encrypt Counter";

  @Autowired
  private OneTimePad oneTimePad;

  @Autowired
  private KeyRegistry keyRegistry;

  @Autowired
  private MetricsHelper helper;

  @Autowired
  @Qualifier(GameBootConcurrentConfiguration.GAME_BOOT_EXECUTOR)
  private ExecutorService svc;

  @Autowired
  private GameBootMessageConverter converter;

  /**
   * Post construct.
   *
   * @throws Exception
   *           the exception
   */
  @PostConstruct
  public void postConstruct() throws Exception {
    super.postConstruct();

    if (!helper.containsCounter(OTP_DECRYPT_COUNTER)) {
      helper.counter(OTP_DECRYPT_COUNTER, getClass(), "otp", "decrypt", "counter");
    }

    if (!helper.containsCounter(OTP_ENCRYPT_COUNTER)) {
      helper.counter(OTP_ENCRYPT_COUNTER, getClass(), "otp", "encrypt", "counter");
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
    super.channelActive(ctx);

    Response r = new Response(ResponseCode.INFO);
    r.setSystemId(getKey());

    ctx.channel().writeAndFlush(converter.toJsonArray(r));
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
    oneTimePad = null;
    keyRegistry = null;
    helper = null;
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
    if (!(msg instanceof byte[])) {
      sendError(ctx, "Message must be a byte array");
      return;
    }

    byte[] key = keyRegistry.get(getKey());

    if (key == null) {
      super.channelRead(ctx, msg);
      return;
    }

    helper.incr(OTP_DECRYPT_COUNTER);

    byte[] converted = oneTimePad.convert(key, (byte[]) msg);

    super.channelRead(ctx, converted);
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * com.github.mrstampy.gameboot.netty.AbstractGameBootNettyMessageHandler#
   * channelReadImpl(io.netty.channel.ChannelHandlerContext, byte[])
   */
  protected void channelReadImpl(ChannelHandlerContext ctx, byte[] msg) throws Exception {
    svc.execute(() -> {
      try {
        process(ctx, new String(msg));
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
   * investigate(io.netty.channel.ChannelHandlerContext,
   * com.github.mrstampy.gameboot.messages.AbstractGameBootMessage)
   */
  protected <AGBM extends AbstractGameBootMessage> boolean investigate(ChannelHandlerContext ctx, AGBM agbm) {
    if (agbm instanceof OtpMessage) {
      sendError(ctx, "OTP messages must be sent on an encrypted connection");
      return false;
    }

    return true;
  }

  /*
   * (non-Javadoc)
   * 
   * @see io.netty.channel.ChannelDuplexHandler#write(io.netty.channel.
   * ChannelHandlerContext, java.lang.Object, io.netty.channel.ChannelPromise)
   */
  @Override
  public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
    if (!(msg instanceof String) && !(msg instanceof byte[])) {
      log.error("Internal error; object is not a string or byte array: {}", msg.getClass());
      return;
    }

    byte[] key = keyRegistry.get(getKey());

    byte[] processed = (msg instanceof byte[]) ? (byte[]) msg : ((String) msg).getBytes();

    if (key == null) {
      ctx.write(processed, promise);
      return;
    }

    helper.incr(OTP_ENCRYPT_COUNTER);

    byte[] converted = oneTimePad.convert(key, processed);

    ctx.write(converted, promise);
  }

}

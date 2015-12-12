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

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.github.mrstampy.gameboot.metrics.MetricsHelper;
import com.github.mrstampy.gameboot.netty.NettyConnectionRegistry;
import com.github.mrstampy.gameboot.otp.KeyRegistry;
import com.github.mrstampy.gameboot.otp.OneTimePad;
import com.github.mrstampy.gameboot.util.GameBootUtils;

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.ChannelPromise;
import io.netty.handler.ssl.SslHandler;

/**
 * The Class OtpHandler is intended to provide a transparent means of using the
 * {@link OneTimePad} utility to encrypt outgoing and decrypt incoming messages
 * on unencrypted Netty connections. It is intended that the message is a byte
 * array at the point in which this class is inserted into the pipeline. Inbound
 * messages are later converted to strings, all outbound messages are byte
 * arrays.<br>
 * <br>
 * 
 * This class uses the {@link Channel#remoteAddress()#toString()} as a key to
 * look up the OTP key. If none exists the message is passed on as is. If an OTP
 * key is returned it is used to encrypt/decrypt the message. <br>
 * <br>
 * 
 * This class registers its channel in the {@link OtpRegistry} as an
 * {@link OtpConnections#getClearChannel()} with the key
 * {@link InetSocketAddress#getHostString()}. The encrypted channel (assumed to
 * be Netty, having a {@link SslHandler} in the pipeline) should have the same
 * remote host and can be added using this clear channel key. <br>
 * <br>
 * 
 * Do not instantiate directly as this is a prototype Spring managed bean. Use
 * {@link GameBootUtils#getBean(Class)} to obtain a unique instance when
 * constructing the {@link ChannelPipeline}.
 * 
 * @see NettyConnectionRegistry
 * @see KeyRegistry
 * @see OneTimePad
 * @see OtpConnections
 */
@Component
@Scope("prototype")
public class OtpHandler extends ChannelDuplexHandler {

  private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private static final String OTP_DECRYPT_COUNTER = "Netty OTP Decrypt Counter";

  private static final String OTP_ENCRYPT_COUNTER = "Netty OTP Encrypt Counter";

  @Autowired
  private OneTimePad oneTimePad;

  @Autowired
  private KeyRegistry keyRegistry;

  @Autowired
  private OtpRegistry otpRegistry;

  @Autowired
  private MetricsHelper helper;

  /**
   * Post construct.
   *
   * @throws Exception
   *           the exception
   */
  @PostConstruct
  public void postConstruct() throws Exception {
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
   * @see
   * io.netty.channel.ChannelInboundHandlerAdapter#channelInactive(io.netty.
   * channel.ChannelHandlerContext)
   */
  @Override
  public void channelActive(ChannelHandlerContext ctx) throws Exception {
    Comparable<?> key = createKey(ctx);

    log.debug("Adding {} keyed by {} as the clear channel to the OtpRegistry", ctx.channel().remoteAddress(), key);

    otpRegistry.setClearChannel(key, ctx.channel());
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
    keyRegistry.remove(getKey(ctx));

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
    byte[] key = keyRegistry.get(getKey(ctx));

    if (key == null) {
      ctx.fireChannelRead(msg);
      return;
    }

    helper.incr(OTP_DECRYPT_COUNTER);

    byte[] converted = oneTimePad.convert(key, (byte[]) msg);

    ctx.fireChannelRead(converted);
  }

  /*
   * (non-Javadoc)
   * 
   * @see io.netty.channel.ChannelDuplexHandler#write(io.netty.channel.
   * ChannelHandlerContext, java.lang.Object, io.netty.channel.ChannelPromise)
   */
  @Override
  public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
    byte[] key = keyRegistry.get(getKey(ctx));

    if (key == null) {
      ctx.write(msg, promise);
      return;
    }

    helper.incr(OTP_ENCRYPT_COUNTER);

    byte[] converted = oneTimePad.convert(key, (byte[]) msg);

    ctx.write(converted, promise);
  }

  private Comparable<?> createKey(ChannelHandlerContext ctx) {
    InetSocketAddress addr = (InetSocketAddress) ctx.channel().remoteAddress();

    return addr.getHostString();
  }

  private String getKey(ChannelHandlerContext ctx) {
    return ctx.channel().remoteAddress().toString();
  }

}

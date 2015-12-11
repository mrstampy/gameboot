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

import static org.apache.commons.lang3.StringUtils.isEmpty;

import javax.annotation.PostConstruct;

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

/**
 * The Class OtpHandler is intended to provide a transparent means of using the
 * {@link OneTimePad} utility to encrypt outgoing and decrypt incoming messages
 * on unencrypted Netty connections. It is intended that the message is a string
 * at the point in which this class is inserted into the pipeline.<br>
 * <br>
 * 
 * This class uses the {@link Channel#remoteAddress()#toString()} as a key to
 * look up the OTP key. If none exists the message is passed on as is. If an OTP
 * key is returned it is used to encrypt/decrypt the message. <br>
 * <br>
 * 
 * Do not instantiate directly as this is a prototype Spring managed bean. Use
 * {@link GameBootUtils#getBean(Class)} to obtain a unique instance when
 * constructing the {@link ChannelPipeline}.
 * 
 * @see NettyConnectionRegistry
 */
@Component
@Scope("prototype")
public class OtpHandler extends ChannelDuplexHandler {

  private static final String OTP_DECRYPT_COUNTER = "Netty OTP Decrypt Counter";

  private static final String OTP_ENCRYPT_COUNTER = "Netty OTP Encrypt Counter";

  @Autowired
  private OneTimePad oneTimePad;

  @Autowired
  private KeyRegistry keyRegistry;

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
    String key = keyRegistry.get(getKey(ctx));
    if (isEmpty(key)) ctx.fireChannelRead(msg);

    helper.incr(OTP_DECRYPT_COUNTER);

    String converted = oneTimePad.convert(key, (String) msg);

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
    String key = keyRegistry.get(getKey(ctx));
    if (isEmpty(key)) ctx.write(msg, promise);

    helper.incr(OTP_ENCRYPT_COUNTER);

    String converted = oneTimePad.convert(key, (String) msg);

    ctx.write(converted, promise);
  }

  private String getKey(ChannelHandlerContext ctx) {
    return ctx.channel().remoteAddress().toString();
  }

}

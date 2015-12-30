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
package com.github.mrstampy.gameboot.netty;

import java.lang.invoke.MethodHandles;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.mrstampy.gameboot.controller.GameBootMessageController;
import com.github.mrstampy.gameboot.messages.AbstractGameBootMessage;
import com.github.mrstampy.gameboot.processor.connection.ConnectionProcessor;
import com.github.mrstampy.gameboot.systemid.SystemId;
import com.github.mrstampy.gameboot.systemid.SystemIdWrapper;

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;

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
 * @param <C>
 *          the generic type
 * @param <CP>
 *          the generic type
 * @see GameBootMessageController
 */
public abstract class AbstractGameBootNettyMessageHandler<C, CP extends ConnectionProcessor<ChannelHandlerContext>>
    extends ChannelDuplexHandler {
  private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private CP connectionProcessor;

  /**
   * Post construct created message counters if necessary. Subclasses will need
   * to invoke this in an annotated {@link PostConstruct} method.
   *
   * @throws Exception
   *           the exception
   */
  protected void postConstruct() throws Exception {
    if (connectionProcessor == null) throw new IllegalStateException("Netty Connection Processor not set");
  }

  /**
   * Subclasses overriding this method should remember to invoke it with a call
   * to 'super.'. Implementation generates a {@link SystemId} and puts the
   * channel in both the {@link NettyConnectionRegistry#ALL} group and against
   * the system id.
   *
   * @param ctx
   *          the ctx
   * @throws Exception
   *           the exception
   * 
   * @see #getSystemId()
   */
  @Override
  public void channelActive(ChannelHandlerContext ctx) throws Exception {
    connectionProcessor.onConnection(ctx);
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
    connectionProcessor.onDisconnection(ctx);

    connectionProcessor = null;
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
    connectionProcessor.onMessage(ctx, msg);
  }

  /**
   * Gets the key set in {@link #channelActive(ChannelHandlerContext)} from
   * {@link SystemId#next()}.
   *
   * @return the key
   */
  public SystemIdWrapper getSystemId() {
    return connectionProcessor.getSystemId(null);
  }

  /**
   * Gets the connection processor.
   *
   * @return the connection processor
   */
  public CP getConnectionProcessor() {
    return connectionProcessor;
  }

  /**
   * Sets the connection processor.
   *
   * @param connectionProcessor
   *          the new connection processor
   */
  public void setConnectionProcessor(CP connectionProcessor) {
    this.connectionProcessor = connectionProcessor;
  }

}

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
package com.github.mrstampy.gameboot.netty.examples;

import java.lang.invoke.MethodHandles;
import java.util.concurrent.ExecutorService;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import com.github.mrstampy.gameboot.concurrent.GameBootConcurrentConfiguration;
import com.github.mrstampy.gameboot.exception.GameBootException;
import com.github.mrstampy.gameboot.exception.GameBootRuntimeException;
import com.github.mrstampy.gameboot.messages.AbstractGameBootMessage;
import com.github.mrstampy.gameboot.netty.AbstractGameBootNettyMessageHandler;
import com.github.mrstampy.gameboot.util.GameBootUtils;
import com.github.mrstampy.gameboot.util.concurrent.MDCRunnable;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;

/**
 * The Class MDCExecutorNettyMessageHandler initializes the Logback {@link MDC}
 * context with the {@link #LOCAL_ADDRESS} (local) and the
 * {@link #REMOTE_ADDRESS} (remote). <br>
 * <br>
 * 
 * While functional these classes are included as examples. As is they will
 * process EVERY {@link AbstractGameBootMessage} type. Subclasses of
 * {@link AbstractGameBootNettyMessageHandler} should implement a message
 * whitelist with aggressive disconnection policies for violations.<br>
 * <br>
 * 
 * Do not instantiate directly as this is a prototype Spring managed bean. Use
 * {@link GameBootUtils#getBean(Class)} to obtain a unique instance when
 * constructing the {@link ChannelPipeline}.
 */
public class MDCExecutorNettyMessageHandler extends AbstractGameBootNettyMessageHandler {
  private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  /** Logback {@link MDC} key for local address (local). */
  public static final String LOCAL_ADDRESS = "local";

  /** Logback {@link MDC} key for remote address (remote). */
  public static final String REMOTE_ADDRESS = "remote";

  @Autowired
  @Qualifier(GameBootConcurrentConfiguration.GAME_BOOT_EXECUTOR)
  private ExecutorService svc;

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
   * channelReadImpl(io.netty.channel.ChannelHandlerContext, java.lang.String)
   */
  @Override
  protected void channelReadImpl(ChannelHandlerContext ctx, String msg) throws Exception {
    initMDC(ctx);

    svc.execute(new MDCRunnable() {

      @Override
      protected void runImpl() {
        try {
          process(ctx, msg);
        } catch (GameBootException | GameBootRuntimeException e) {
          sendError(ctx, e);
        } catch (Exception e) {
          log.error("Unexpected exception", e);
          sendUnexpectedError(ctx);
        }
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
  @Override
  protected void channelReadImpl(ChannelHandlerContext ctx, byte[] msg) throws Exception {
    initMDC(ctx);

    svc.execute(new MDCRunnable() {

      @Override
      protected void runImpl() {
        try {
          process(ctx, new String(msg));
        } catch (GameBootException | GameBootRuntimeException e) {
          sendError(ctx, e);
        } catch (Exception e) {
          log.error("Unexpected exception", e);
          sendUnexpectedError(ctx);
        }
      }
    });
  }

  private void initMDC(ChannelHandlerContext ctx) {
    MDC.put(REMOTE_ADDRESS, ctx.channel().remoteAddress().toString());
    MDC.put(LOCAL_ADDRESS, ctx.channel().localAddress().toString());
  }

}

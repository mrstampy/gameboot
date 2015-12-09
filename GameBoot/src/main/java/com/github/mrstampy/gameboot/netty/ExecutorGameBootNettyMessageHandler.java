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
import java.util.concurrent.ExecutorService;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.github.mrstampy.gameboot.concurrent.GameBootConcurrentConfiguration;
import com.github.mrstampy.gameboot.concurrent.MDCRunnable;
import com.github.mrstampy.gameboot.util.GameBootUtils;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;

/**
 * This class uses the configured {@link ExecutorService} to process incoming
 * messages, preserving the Logback mapped diagnostic context. It is intended to
 * be added last to the {@link ChannelPipeline}.<br>
 * <br>
 * 
 * Do not instantiate directly as this is a prototype Spring managed bean. Use
 * {@link GameBootUtils#getBean(Class)} to obtain a unique instance when
 * constructing the {@link ChannelPipeline}.
 * 
 * @see GameBootConcurrentConfiguration
 */
@Component
@Scope("prototype")
public class ExecutorGameBootNettyMessageHandler extends AbstractGameBootNettyMessageHandler {
  private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  /** Logback {@link MDC} key for local address (nettyLocal). */
  public static final String LOCAL_ADDRESS = "nettyLocal";

  /** Logback {@link MDC} key for remote address (nettyRemote). */
  public static final String REMOTE_ADDRESS = "nettyRemote";

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
        } catch (Exception e) {
          log.error("Unexpected exception", e);
        }
      }
    });
  }

  private void initMDC(ChannelHandlerContext ctx) {
    MDC.put(REMOTE_ADDRESS, ctx.channel().remoteAddress().toString());
    MDC.put(LOCAL_ADDRESS, ctx.channel().localAddress().toString());
  }

}

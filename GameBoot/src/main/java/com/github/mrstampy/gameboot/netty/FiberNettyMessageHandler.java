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

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.github.mrstampy.gameboot.concurrent.GameBootConcurrentConfiguration;
import com.github.mrstampy.gameboot.processor.GameBootProcessor;
import com.github.mrstampy.gameboot.util.GameBootUtils;

import co.paralleluniverse.fibers.Fiber;
import co.paralleluniverse.fibers.FiberExecutorScheduler;
import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.strands.SuspendableCallable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;

/**
 * This class uses the configured {@link FiberExecutorScheduler} to execute
 * incoming messages. It is intended to be added last to the
 * {@link ChannelPipeline}. <br>
 * <br>
 * 
 * Do not instantiate directly as this is a prototype Spring managed bean. Use
 * {@link GameBootUtils#getBean(Class)} to obtain a unique instance when
 * constructing the {@link ChannelPipeline}.<br>
 * <br>
 * 
 * Note that {@link GameBootProcessor}s which define a {@link Transactional}
 * boundary around the
 * {@link GameBootProcessor#process(com.github.mrstampy.gameboot.messages.AbstractGameBootMessage)}
 * method are not suitable for execution within a {@link FiberExecutorScheduler}
 * (unless instrumentation is turned off). See the
 * <a href="http://docs.paralleluniverse.co/quasar/">Quasar documentation</a>
 * for more information about Fibers vs. Threads.
 * 
 * @see GameBootConcurrentConfiguration
 */
@Component
@Scope("prototype")
public class FiberNettyMessageHandler extends AbstractGameBootNettyMessageHandler {
  private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Autowired
  private FiberExecutorScheduler svc;

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
  @SuppressWarnings("serial")
  protected void channelReadImpl(ChannelHandlerContext ctx, String msg) throws Exception {
    Fiber<Void> fiber = svc.newFiber(new SuspendableCallable<Void>() {

      @Override
      public Void run() throws SuspendExecution, InterruptedException {
        process(ctx, msg);

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
   * @throws SuspendExecution
   *           the suspend execution
   * @throws InterruptedException
   *           the interrupted exception
   */
  public void process(ChannelHandlerContext ctx, String msg) throws SuspendExecution, InterruptedException {
    try {
      super.process(ctx, msg);
    } catch (SuspendExecution | InterruptedException e) {
      throw e;
    } catch (Exception e) {
      log.error("Unexpected exception", e);
    }
  }

}

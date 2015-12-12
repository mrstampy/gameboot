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
package com.github.mrstampy.gameboot.websocket;

import java.lang.invoke.MethodHandles;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.socket.WebSocketSession;

import com.github.mrstampy.gameboot.concurrent.GameBootConcurrentConfiguration;

import co.paralleluniverse.fibers.FiberForkJoinScheduler;
import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.strands.SuspendableCallable;

/**
 * The Class FiberForkJoinWebSocketHandler.
 */
public class FiberForkJoinWebSocketHandler extends AbstractGameBootWebSocketHandler {
  private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Autowired
  @Qualifier(GameBootConcurrentConfiguration.GAME_BOOT_EXECUTOR)
  private FiberForkJoinScheduler svc;

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
   * com.github.mrstampy.gameboot.websocket.AbstractGameBootWebSocketHandler#
   * handleTextMessageImpl(org.springframework.web.socket.WebSocketSession,
   * org.springframework.web.socket.TextMessage)
   */
  @SuppressWarnings("serial")
  @Override
  protected void handleTextMessageImpl(WebSocketSession session, String message) throws Exception {
    svc.newFiber(new SuspendableCallable<Void>() {

      @Override
      public Void run() throws SuspendExecution, InterruptedException {
        processForText(session, message);

        return null;
      }
    }).start();
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * com.github.mrstampy.gameboot.websocket.AbstractGameBootWebSocketHandler#
   * processForText(org.springframework.web.socket.WebSocketSession,
   * java.lang.String)
   */
  public void processForText(WebSocketSession session, String message) throws SuspendExecution, InterruptedException {
    try {
      super.processForText(session, message);
    } catch (SuspendExecution | InterruptedException e) {
      throw e;
    } catch (Exception e) {
      log.error("Unexpected exception", e);
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * com.github.mrstampy.gameboot.websocket.AbstractGameBootWebSocketHandler#
   * handleBinaryMessageImpl(org.springframework.web.socket.WebSocketSession,
   * byte[])
   */
  @SuppressWarnings("serial")
  @Override
  protected void handleBinaryMessageImpl(WebSocketSession session, byte[] message) {
    svc.newFiber(new SuspendableCallable<Void>() {

      @Override
      public Void run() throws SuspendExecution, InterruptedException {
        processForBinary(session, message);

        return null;
      }
    }).start();
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * com.github.mrstampy.gameboot.websocket.AbstractGameBootWebSocketHandler#
   * processForBinary(org.springframework.web.socket.WebSocketSession, byte[])
   */
  public void processForBinary(WebSocketSession session, byte[] message) throws SuspendExecution, InterruptedException {
    try {
      super.processForBinary(session, message);
    } catch (SuspendExecution | InterruptedException e) {
      throw e;
    } catch (Exception e) {
      log.error("Unexpected exception", e);
    }
  }

}

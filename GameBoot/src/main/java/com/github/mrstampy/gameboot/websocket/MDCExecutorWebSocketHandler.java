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
import java.util.concurrent.ExecutorService;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.socket.WebSocketSession;

import com.github.mrstampy.gameboot.concurrent.GameBootConcurrentConfiguration;
import com.github.mrstampy.gameboot.util.concurrent.MDCRunnable;

/**
 * The Class MDCExecutorWebSocketHandler.
 */
public class MDCExecutorWebSocketHandler extends ExecutorWebSocketHandler {
  private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  /** Logback {@link MDC} key for local address (local). */
  public static final String LOCAL_ADDRESS = "local";

  /** Logback {@link MDC} key for remote address (remote). */
  public static final String REMOTE_ADDRESS = "remote";

  /** The Constant WEB_SESSION_ID. */
  public static final String WEB_SESSION_ID = "webSessionId";

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
   * @see com.github.mrstampy.gameboot.websocket.ExecutorWebSocketHandler#
   * handleTextMessageImpl(org.springframework.web.socket.WebSocketSession,
   * org.springframework.web.socket.TextMessage)
   */
  /**
   * Handle text message impl.
   *
   * @param session
   *          the session
   * @param message
   *          the message
   * @throws Exception
   *           the exception
   */
  @Override
  protected void handleTextMessageImpl(WebSocketSession session, String message) throws Exception {
    initMDC(session);

    svc.execute(new MDCRunnable() {

      @Override
      protected void runImpl() {
        try {
          process(session, message);
        } catch (Exception e) {
          log.error("Unexpected exception", e);
        }
      }
    });
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * com.github.mrstampy.gameboot.websocket.AbstractGameBootWebSocketHandler#
   * handleBinaryMessageImpl(org.springframework.web.socket.WebSocketSession,
   * byte[])
   */
  @Override
  protected void handleBinaryMessageImpl(WebSocketSession session, byte[] message) {
    initMDC(session);

    svc.execute(new MDCRunnable() {

      @Override
      protected void runImpl() {
        try {
          process(session, new String(message));
        } catch (Exception e) {
          log.error("Unexpected exception", e);
        }
      }
    });
  }

  private void initMDC(WebSocketSession session) {
    MDC.put(REMOTE_ADDRESS, session.getRemoteAddress().toString());
    MDC.put(LOCAL_ADDRESS, session.getLocalAddress().toString());
    MDC.put(WEB_SESSION_ID, session.getId());
  }

}

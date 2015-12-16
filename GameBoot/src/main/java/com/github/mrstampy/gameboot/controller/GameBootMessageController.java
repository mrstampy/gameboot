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
package com.github.mrstampy.gameboot.controller;

import static org.apache.commons.lang3.StringUtils.isEmpty;

import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.github.mrstampy.gameboot.exception.GameBootRuntimeException;
import com.github.mrstampy.gameboot.messages.AbstractGameBootMessage;
import com.github.mrstampy.gameboot.messages.GameBootMessageConverter;
import com.github.mrstampy.gameboot.messages.Response;
import com.github.mrstampy.gameboot.messages.error.ErrorCodes;
import com.github.mrstampy.gameboot.messages.error.ErrorLookup;
import com.github.mrstampy.gameboot.metrics.MetricsHelper;
import com.github.mrstampy.gameboot.processor.GameBootProcessor;

/**
 * This class is intended to be used to process incoming messages when the type
 * of message is not known in advance ie. web sockets. Incoming messages are
 * converted to their {@link AbstractGameBootMessage} counterparts and the
 * {@link GameBootProcessor} for that message is used to return the response.
 * <br>
 * <br>
 * GameBoot enforces one {@link GameBootProcessor} per
 * {@link AbstractGameBootMessage}. Implement a different
 * {@link MessageClassFinder} to process alternative messages.
 */
@Component
public class GameBootMessageController implements ErrorCodes {
  private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private static final String MESSAGE_COUNTER = "Message Controller Counter";

  @Autowired
  private List<GameBootProcessor<? extends AbstractGameBootMessage>> processors;

  @Autowired
  private MetricsHelper helper;

  @Autowired
  private GameBootMessageConverter converter;

  private ErrorLookup lookup;

  /** The map. */
  protected Map<String, GameBootProcessor<?>> map = new ConcurrentHashMap<>();

  /**
   * Sets the error lookup.
   *
   * @param lookup
   *          the new error lookup
   */
  @Autowired
  public void setErrorLookup(ErrorLookup lookup) {
    this.lookup = lookup;
  }

  /**
   * Post construct, invoke directly from subclass {@link PostConstruct}.
   *
   * @throws Exception
   *           the exception
   */
  @PostConstruct
  public void postConstruct() throws Exception {
    processors.forEach(p -> map.put(p.getType(), p));

    helper.counter(MESSAGE_COUNTER, GameBootMessageController.class, "message", "counter");
  }

  /**
   * Process the given JSON message using the {@link GameBootProcessor}
   * specified for its {@link AbstractGameBootMessage#getType()}.
   *
   * @param <AGBM>
   *          the generic type
   * @param request
   *          the message
   * @return the response
   * @throws Exception
   *           the exception
   */
  public <AGBM extends AbstractGameBootMessage> String process(String request) throws Exception {
    helper.incr(MESSAGE_COUNTER);

    if (isEmpty(request)) fail(NO_MESSAGE, "Empty message");

    AGBM msg = converter.fromJson(request);

    Response r = process(request, msg);

    return r == null ? null : converter.toJson(r);
  }

  /**
   * Exposed for {@link AbstractGameBootMessage}-encapsulating protocols
   * (JSON-RPC, Stomp etc).
   *
   * @param <AGBM>
   *          the generic type
   * @param request
   *          the request
   * @param msg
   *          the msg
   * @return the response
   * @throws Exception
   *           the exception
   */
  @SuppressWarnings("unchecked")
  public <AGBM extends AbstractGameBootMessage> Response process(String request, AGBM msg) throws Exception {
    GameBootProcessor<AGBM> processor = (GameBootProcessor<AGBM>) map.get(msg.getType());

    if (processor == null) {
      log.error("No processor for {}", request);
      fail(UNKNOWN_MESSAGE, "Unrecognized message");
    }

    return processor.process(msg);
  }

  /**
   * Fail, throwing a {@link GameBootRuntimeException} with the specified
   * message.
   *
   * @param code
   *          the code
   * @param message
   *          the message
   * @param payload
   *          the payload
   * @throws GameBootRuntimeException
   *           the game boot runtime exception
   */
  protected void fail(int code, String message, Object... payload) throws GameBootRuntimeException {
    throw new GameBootRuntimeException(message, lookup.lookup(code), payload);
  }
}

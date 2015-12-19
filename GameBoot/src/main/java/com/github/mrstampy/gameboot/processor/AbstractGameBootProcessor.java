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
package com.github.mrstampy.gameboot.processor;

import java.lang.invoke.MethodHandles;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.github.mrstampy.gameboot.exception.GameBootException;
import com.github.mrstampy.gameboot.exception.GameBootRuntimeException;
import com.github.mrstampy.gameboot.exception.GameBootThrowable;
import com.github.mrstampy.gameboot.messages.AbstractGameBootMessage;
import com.github.mrstampy.gameboot.messages.Response;
import com.github.mrstampy.gameboot.messages.Response.ResponseCode;
import com.github.mrstampy.gameboot.messages.context.ResponseContext;
import com.github.mrstampy.gameboot.messages.context.ResponseContextCodes;
import com.github.mrstampy.gameboot.messages.context.ResponseContextLookup;

/**
 * Abstract superclass for {@link GameBootProcessor}s.
 *
 * @param <M>
 *          the generic type
 */
public abstract class AbstractGameBootProcessor<M extends AbstractGameBootMessage>
    implements GameBootProcessor<M>, ResponseContextCodes {
  private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private ResponseContextLookup lookup;

  /**
   * Sets the error lookup.
   *
   * @param lookup
   *          the new error lookup
   */
  @Autowired
  public void setErrorLookup(ResponseContextLookup lookup) {
    this.lookup = lookup;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * com.github.mrstampy.gameboot.processor.GameBootProcessor#process(com.github
   * .mrstampy.gameboot.messages.AbstractGameBootMessage)
   */
  @Override
  public Response process(M message) throws Exception {
    log.debug("Processing message {}", message);

    if (message == null) fail(NO_MESSAGE, "Null message");

    try {
      validate(message);

      Response response = processImpl(message);
      response.setId(message.getId());
      if (Response.TYPE.equals(response.getType())) response.setType(message.getType());

      log.debug("Created response {} for {}", response, message);

      return response;
    } catch (GameBootRuntimeException | GameBootException e) {
      return gameBootErrorResponse(message, e);
    } catch (Exception e) {
      log.error("Error in processing {}", message.getType(), e);

      Response r = failure(UNEXPECTED_ERROR, message, "An unexpected error has occurred");
      r.setId(message.getId());

      return r;
    }
  }

  /**
   * Game boot error response.
   *
   * @param message
   *          the message
   * @param e
   *          the e
   * @return the response
   */
  protected Response gameBootErrorResponse(M message, GameBootThrowable e) {
    ResponseContext error = e.getError();

    log.error("Error in processing {} : {}, {}", message.getType(), error, e.getMessage());

    Object[] payload = e.getPayload();

    Response r = new Response(message, ResponseCode.FAILURE, error, payload);
    r.setId(message.getId());
    r.setContext(error);

    return r;
  }

  /**
   * Fail, throwing a {@link GameBootRuntimeException} with the specified
   * message. The exception is caught by the
   * {@link #process(AbstractGameBootMessage)} implementation which after
   * logging returns a failure response.
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

  /**
   * Returns an initialized success {@link Response}.
   *
   * @param message
   *          the message
   * @param response
   *          the response
   * @return the response
   */
  protected Response success(M message, Object... response) {
    return new Response(message, ResponseCode.SUCCESS, response);
  }

  /**
   * Returns an initialized failure {@link Response}.
   *
   * @param code
   *          the code
   * @param message
   *          the message
   * @param response
   *          the response
   * @return the response
   */
  protected Response failure(int code, M message, Object... response) {
    return new Response(message, ResponseCode.FAILURE, lookup.lookup(code), response);
  }

  /**
   * Implement to perform any pre-processing validation.
   *
   * @param message
   *          the message
   * @throws Exception
   *           the exception
   */
  protected abstract void validate(M message) throws Exception;

  /**
   * Implement to process the {@link #validate(AbstractGameBootMessage)}'ed
   * message.
   *
   * @param message
   *          the message
   * @return the response
   * @throws Exception
   *           the exception
   */
  protected abstract Response processImpl(M message) throws Exception;

}

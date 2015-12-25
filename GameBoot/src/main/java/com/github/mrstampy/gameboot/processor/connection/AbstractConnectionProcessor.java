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
package com.github.mrstampy.gameboot.processor.connection;

import org.springframework.beans.factory.annotation.Autowired;

import com.github.mrstampy.gameboot.exception.GameBootThrowable;
import com.github.mrstampy.gameboot.messages.AbstractGameBootMessage;
import com.github.mrstampy.gameboot.messages.Response;
import com.github.mrstampy.gameboot.messages.Response.ResponseCode;
import com.github.mrstampy.gameboot.messages.context.ResponseContext;
import com.github.mrstampy.gameboot.messages.context.ResponseContextLookup;

/**
 * The Class AbstractConnectionProcessor.
 *
 * @param <C>
 *          the generic type
 */
public abstract class AbstractConnectionProcessor<C> implements ConnectionProcessor<C> {

  @Autowired
  private ResponseContextLookup lookup;

  /*
   * (non-Javadoc)
   * 
   * @see
   * com.github.mrstampy.gameboot.processor.connection.ConnectionProcessor#fail(
   * com.github.mrstampy.gameboot.messages.AbstractGameBootMessage,
   * com.github.mrstampy.gameboot.exception.GameBootThrowable)
   */
  @Override
  public Response fail(AbstractGameBootMessage message, GameBootThrowable e) {
    ResponseContext error = e.getError();
    Object[] payload = e.getPayload();

    Response r = new Response(message, ResponseCode.FAILURE, payload);
    r.setContext(error);

    return r;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * com.github.mrstampy.gameboot.processor.connection.ConnectionProcessor#fail(
   * int, com.github.mrstampy.gameboot.messages.AbstractGameBootMessage,
   * java.lang.Object[])
   */
  @Override
  public Response fail(int code, AbstractGameBootMessage message, Object... payload) {
    Response r = new Response(message, ResponseCode.FAILURE, payload);
    r.setContext(lookup.lookup(code));

    return r;
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.github.mrstampy.gameboot.processor.connection.ConnectionProcessor#
   * sendUnexpectedError(java.lang.Object)
   */
  public void sendUnexpectedError(C ctx) {
    sendError(UNEXPECTED_ERROR, ctx, "An unexpected error has occurred");
  }

}

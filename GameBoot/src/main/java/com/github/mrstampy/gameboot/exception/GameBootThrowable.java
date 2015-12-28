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
package com.github.mrstampy.gameboot.exception;

import com.github.mrstampy.gameboot.messages.Response;
import com.github.mrstampy.gameboot.messages.context.ResponseContext;
import com.github.mrstampy.gameboot.messages.context.ResponseContextLookup;

/**
 * The Interface GameBootThrowable.
 */
public interface GameBootThrowable {

  /**
   * Checks for payload.
   *
   * @return true, if successful
   */
  boolean hasPayload();

  /**
   * Gets the payload. If {@link #getError()} is not null the payload is added
   * to the {@link Response#getPayload()}. If {@link #getErrorCode()} is not
   * null it is considered to be parameters for the
   * {@link ResponseContextLookup} of the {@link ResponseContext}.
   *
   * @return the payload
   */
  Object[] getPayload();

  /**
   * Sets the payload.
   *
   * @param payload
   *          the new payload
   */
  void setPayload(Object[] payload);

  /**
   * Gets the error.
   *
   * @return the error
   */
  ResponseContext getError();

  /**
   * Sets the error.
   *
   * @param error
   *          the new error
   */
  void setError(ResponseContext error);

  /**
   * Gets the message.
   *
   * @return the message
   */
  String getMessage();

  /**
   * Gets the error code.
   *
   * @return the error code
   */
  Integer getErrorCode();

  /**
   * Sets the error code.
   *
   * @param code
   *          the new error code
   */
  void setErrorCode(Integer code);

}
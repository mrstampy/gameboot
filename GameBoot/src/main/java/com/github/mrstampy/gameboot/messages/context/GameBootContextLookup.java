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
package com.github.mrstampy.gameboot.messages.context;

import java.lang.invoke.MethodHandles;
import java.util.Locale;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * The Class GameBootErrorLookup.
 */
public class GameBootContextLookup implements ResponseContextLookup {
  private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private ResponseContextLoader loader;

  private Map<Integer, ResponseContext> errors;

  /**
   * Sets the error loader.
   *
   * @param loader
   *          the new error loader
   */
  @Autowired
  public void setErrorLoader(ResponseContextLoader loader) {
    this.loader = loader;
  }

  /**
   * Post construct.
   *
   * @throws Exception
   *           the exception
   */
  @PostConstruct
  public void postConstruct() throws Exception {
    errors = loader.getErrorProperties();
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * com.github.mrstampy.gameboot.messages.error.ErrorLookup#lookup(java.lang.
   * Integer)
   */
  @Override
  public ResponseContext lookup(Integer code) {
    ResponseContext error = errors.get(code);

    if (error == null) log.warn("No error for code {}", code);

    return error;
  }

  /**
   * Default implementation not internationalized.
   */
  @Override
  public ResponseContext lookup(Integer code, Locale locale) {
    return lookup(code);
  }

}

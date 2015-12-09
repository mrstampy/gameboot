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
package com.github.mrstampy.gameboot.concurrent;

import java.util.Map;
import java.util.concurrent.Callable;

import org.slf4j.MDC;

/**
 * The Class MDCCallable preserves the Logback mapped diagnostic context when
 * executing this {@link Callable} in a separate thread.
 *
 * @param <V>
 *          the value type
 */
public abstract class MDCCallable<V> implements Callable<V> {

  private Map<String, String> mdc;

  /**
   * Instantiates a new MDC callable.
   */
  public MDCCallable() {
    this.mdc = MDC.getCopyOfContextMap();
  }

  /*
   * (non-Javadoc)
   * 
   * @see java.util.concurrent.Callable#call()
   */
  @Override
  public V call() throws Exception {
    MDC.setContextMap(mdc);

    try {
      return callImpl();
    } finally {
      MDC.clear();
    }
  }

  /**
   * Implement to perform the task.
   *
   * @return the v
   * @throws Exception
   *           the exception
   */
  protected abstract V callImpl() throws Exception;
}

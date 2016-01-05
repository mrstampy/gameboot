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
 * Copyright (C) 2015, 2016 Burton Alexander
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
package com.github.mrstampy.gameboot.util;

import java.util.Arrays;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * A collection of utility methods.
 */
@Component
public class GameBootUtils implements ApplicationContextAware {

  private ApplicationContext ctx;

  /**
   * Returns a context-initialized Spring managed bean of the specified type,
   * useful for obtaining prototype-{@link Scope}ed bean instances and bean
   * references from outside the {@link ApplicationContext}.
   *
   * @param <T>
   *          the generic type
   * @param clz
   *          the clz
   * @return the bean
   */
  public <T> T getBean(Class<T> clz) {
    return ctx.getBean(clz);
  }

  /**
   * Checks if is power of 2.
   *
   * @param i
   *          the i
   * @return true, if is power of 2
   */
  public boolean isPowerOf2(Integer i) {
    return (i == null || i < 0) ? false : (i & -i) == i;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.springframework.context.ApplicationContextAware#setApplicationContext(
   * org.springframework.context.ApplicationContext)
   */
  @Override
  public void setApplicationContext(ApplicationContext ctx) throws BeansException {
    this.ctx = ctx;
  }

  /**
   * Prepend array.
   *
   * @param <T>
   *          the generic type
   * @param pre
   *          the pre
   * @param post
   *          the post
   * @return the t[]
   */
  @SuppressWarnings("unchecked")
  public <T> T[] prependArray(T pre, T... post) {
    if (pre == null) throw new IllegalArgumentException("Element cannot be null");
    if (post == null) throw new IllegalArgumentException("Array cannot be null");

    T[] array = Arrays.copyOf(post, post.length + 1);

    array[0] = pre;
    if (array.length == 1) return array;

    System.arraycopy(post, 0, array, 1, post.length);

    return array;
  }

  /**
   * Postpend array.
   *
   * @param <T>
   *          the generic type
   * @param post
   *          the post
   * @param pre
   *          the pre
   * @return the t[]
   */
  @SuppressWarnings("unchecked")
  public <T> T[] postpendArray(T post, T... pre) {
    if (post == null) throw new IllegalArgumentException("Element cannot be null");
    if (pre == null) throw new IllegalArgumentException("Array cannot be null");

    T[] array = Arrays.copyOf(pre, pre.length + 1);

    array[pre.length] = post;

    return array;
  }
}

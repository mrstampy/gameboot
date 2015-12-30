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
package com.github.mrstampy.gameboot.util.registry;

import org.apache.commons.lang3.builder.HashCodeBuilder;

/**
 * The Class AbstractNumberKey is to be subclassed for {@link GameBootRegistry}
 * keys extending {@link java.lang.Number} and implementing
 * {@link java.lang.Comparable}.
 *
 * @param <N>
 *          the number type
 */
public abstract class AbstractNumberKey<N extends Number> implements Comparable<AbstractNumberKey<N>> {

  private final N value;

  /**
   * Instantiates a new abstract number key.
   *
   * @param value
   *          the value
   */
  public AbstractNumberKey(N value) {
    if (value == null) throw new NullPointerException("No value");
    this.value = value;
  }

  /**
   * Gets the value.
   *
   * @return the value
   */
  public N getValue() {
    return value;
  }

  /*
   * (non-Javadoc)
   * 
   * @see java.lang.Object#toString()
   */
  public String toString() {
    return value.toString();
  }

  /*
   * (non-Javadoc)
   * 
   * @see java.lang.Object#equals(java.lang.Object)
   */
  public boolean equals(Object o) {
    if (!(o instanceof AbstractNumberKey)) return false;

    AbstractNumberKey<?> ank = (AbstractNumberKey<?>) o;

    return this.value.equals(ank.value);
  }

  /*
   * (non-Javadoc)
   * 
   * @see java.lang.Object#hashCode()
   */
  public int hashCode() {
    return HashCodeBuilder.reflectionHashCode(this);
  }

  /*
   * (non-Javadoc)
   * 
   * @see java.lang.Comparable#compareTo(java.lang.Object)
   */
  @SuppressWarnings("unchecked")
  @Override
  public int compareTo(AbstractNumberKey<N> o) {
    if (!(value instanceof Comparable)) {
      throw new IllegalStateException(value.getClass() + " is not a java.lang.Comparable");
    }

    Comparable<N> left = (Comparable<N>) value;

    return left.compareTo(o.value);
  }

}

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
package com.github.mrstampy.gameboot.util.resource;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.type.AnnotatedTypeMetadata;

/**
 * The Class AbstractFallbackResourceCondition allows a {@link Condition} to be
 * written where a fallback {@link Resource} can be overridden by any of the
 * supplied overrides should they exist.
 */
public abstract class AbstractFallbackResourceCondition implements Condition {

  /** The Constant EXT_CLASSPATH. */
  public static final String EXT_CLASSPATH = "gameboot/";

  private String fallback;
  private String[] overrides;

  /**
   * Instantiates a new abstract fallback resource condition with a fallback
   * {@link Resource} and overrides.
   *
   * @param fallBack
   *          the fall back
   * @param overrides
   *          the overrides
   */
  protected AbstractFallbackResourceCondition(String fallBack, String... overrides) {
    this.fallback = fallBack;
    this.overrides = overrides;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.springframework.context.annotation.Condition#matches(org.
   * springframework.context.annotation.ConditionContext,
   * org.springframework.core.type.AnnotatedTypeMetadata)
   */
  @Override
  public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
    ResourceLoader loader = context.getResourceLoader();

    if (hasOverride(loader)) return false;

    boolean b = loader.getResource(fallback).exists();

    if (b) ResourceLogger.log(fallback);

    return b;
  }

  private boolean hasOverride(ResourceLoader loader) {
    if (overrides == null || overrides.length == 0) return false;

    for (String override : overrides) {
      Resource o = loader.getResource(override);
      if (o.exists()) {
        ResourceLogger.log(override, fallback);
        return true;
      }
    }

    return false;
  }

}

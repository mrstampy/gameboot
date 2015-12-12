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
package com.github.mrstampy.gameboot.metrics;

import java.lang.invoke.MethodHandles;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * The Class GameBootMetricsConfiguration creates the default
 * {@link MetricsHelper} implementation ({@link GameBootMetricsHelper}). Use the
 * {@link NullMetricsHelper} to disable GameBoot metrics acquisition by
 * excluding this configuration and adding a {@link Configuration} which returns
 * it.
 */
@Configuration
public class GameBootMetricsConfiguration {
  private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  /**
   * Helper.
   *
   * @return the metrics helper
   * @throws Exception
   *           the exception
   */
  @Bean
  @ConditionalOnProperty(name = "game.boot.metrics", havingValue = "true")
  public MetricsHelper helper() throws Exception {
    log.info("Acquiring GameBoot metrics");
    return new GameBootMetricsHelper();
  }

  /**
   * Helper.
   *
   * @return the metrics helper
   * @throws Exception
   *           the exception
   */
  @Bean
  @ConditionalOnProperty(name = "game.boot.metrics", havingValue = "false")
  public MetricsHelper nullHelper() throws Exception {
    log.info("Ignoring GameBoot metrics");
    return new NullMetricsHelper();
  }
}

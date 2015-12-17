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
package com.github.mrstampy.gameboot.data;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import com.github.mrstampy.gameboot.data.condition.ClassPathCondition;
import com.github.mrstampy.gameboot.data.condition.ExternalClassPathCondition;
import com.github.mrstampy.gameboot.data.condition.FileCondition;

/**
 * Data configuration for GameBoot. More from the <a href=
 * "http://docs.spring.io/spring-boot/docs/current/reference/html/howto-database-initialization.html#howto-initialize-a-database-using-spring-jdbc">
 * Spring Boot Documentation</a>
 * 
 * @author burton
 *
 */
@Configuration
@EnableJpaAuditing
@EnableTransactionManagement
public class GameBootDataConfiguration implements ApplicationContextAware {

  private ApplicationContext ctx;

  /**
 * File database populator.
 *
 * @return the resource database populator
 * @throws Exception
 *           the exception
 */
  @Bean
  @Conditional(FileCondition.class)
  public ResourceDatabasePopulator fileDatabasePopulator() throws Exception {
    return new ResourceDatabasePopulator(ctx.getResource(FileCondition.GAMEBOOT_SQL));
  }

  /**
 * Ext class path database populator.
 *
 * @return the resource database populator
 * @throws Exception
 *           the exception
 */
  @Bean
  @Conditional(ExternalClassPathCondition.class)
  public ResourceDatabasePopulator extClassPathDatabasePopulator() throws Exception {
    return new ResourceDatabasePopulator(ctx.getResource(ExternalClassPathCondition.GAMEBOOT_SQL));
  }

  /**
 * Classpath database populator.
 *
 * @return the resource database populator
 * @throws Exception
 *           the exception
 */
  @Bean
  @Conditional(ClassPathCondition.class)
  public ResourceDatabasePopulator classpathDatabasePopulator() throws Exception {
    return new ResourceDatabasePopulator(ctx.getResource(ClassPathCondition.GAMEBOOT_SQL));
  }

  /* (non-Javadoc)
   * @see org.springframework.context.ApplicationContextAware#setApplicationContext(org.springframework.context.ApplicationContext)
   */
  @Override
  public void setApplicationContext(ApplicationContext ctx) throws BeansException {
    this.ctx = ctx;
  }
}

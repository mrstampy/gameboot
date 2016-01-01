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
package com.github.mrstampy.gameboot;

import static com.github.mrstampy.gameboot.data.properties.condition.ClassPathCondition.DATABASE_PROPERTIES;
import static com.github.mrstampy.gameboot.otp.properties.condition.ClassPathCondition.OTP_PROPERTIES;
import static com.github.mrstampy.gameboot.properties.condition.ClassPathCondition.GAMEBOOT_PROPERTIES;
import static com.github.mrstampy.gameboot.security.properties.condition.ClassPathCondition.SECURITY_PROPERTIES;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.invoke.MethodHandles;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

/**
 * The Class GameBootDependencyWriter writes the GameBoot configuration to the
 * file system.
 */
@Component
public class GameBootDependencyWriter {
  private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private static final String LOGBACK = "classpath:logback.groovy";
  private static final String GAMEBOOT_SQL_INIT = "classpath:gameboot.sql.init";
  private static final String EHCACHE_XML = "classpath:ehcache.xml";
  private static final String APPLICATION_PROPERTIES = "classpath:application.properties";
  private static final String ERROR_PROPERTIES = "classpath:error.properties";

  /**
   * Write dependencies.
   *
   * @param ctx
   *          the ctx
   * @throws IOException
   *           Signals that an I/O exception has occurred.
   */
  public void writeDependencies(ConfigurableApplicationContext ctx) throws IOException {
    writeResource(ctx.getResource(DATABASE_PROPERTIES));
    writeResource(ctx.getResource(OTP_PROPERTIES));
    writeResource(ctx.getResource(GAMEBOOT_PROPERTIES));
    writeResource(ctx.getResource(SECURITY_PROPERTIES));
    writeResource(ctx.getResource(ERROR_PROPERTIES));
    writeResource(ctx.getResource(APPLICATION_PROPERTIES));
    writeResource(ctx.getResource(EHCACHE_XML));
    writeResource(ctx.getResource(GAMEBOOT_SQL_INIT));
    writeResource(ctx.getResource(LOGBACK));
  }

  private void writeResource(Resource resource) throws IOException {
    String desc = resource.getDescription();
    log.debug("Creating file from {}", desc);

    if (!resource.exists()) {
      log.warn("No resource for {}", desc);
      return;
    }

    int first = desc.indexOf("[");
    int last = desc.indexOf("]");

    desc = desc.substring(first + 1, last);

    File f = new File(".", desc);

    try (BufferedOutputStream bis = new BufferedOutputStream(new FileOutputStream(f))) {
      InputStream in = resource.getInputStream();
      byte[] b = new byte[in.available()];
      in.read(b);

      bis.write(b);
      bis.flush();
    }
  }
}

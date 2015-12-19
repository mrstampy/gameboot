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
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.io.Resource;

import com.github.mrstampy.gameboot.util.resource.AbstractFallbackResourceCondition;

/**
 * The Class GameBootErrorLoader.
 */
public class GameBootContextLoader implements ApplicationContextAware, ResponseContextLoader {
  private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private static final String CODE = ".code";
  private static final String FUNCTION = ".function";
  private static final String DESCRIPTION = ".description";

  private ApplicationContext ctx;

  /*
   * (non-Javadoc)
   * 
   * @see
   * com.github.mrstampy.gameboot.messages.error.ErrorLoader#getErrorProperties(
   * )
   */
  @Override
  @SuppressWarnings("unchecked")
  public Map<Integer, ResponseContext> getErrorProperties() throws Exception {
    Resource r = getOverridableErrorResource();

    if (r == null || !r.exists()) {
      log.error("No error.properties");
      return Collections.EMPTY_MAP;
    }

    Properties p = new Properties();
    p.load(r.getInputStream());

    List<String> codes = getCodes(p);

    Map<Integer, ResponseContext> map = new ConcurrentHashMap<>();

    codes.forEach(c -> createError(c, p, map));

    return Collections.unmodifiableMap(map);
  }

  private Resource getOverridableErrorResource() {
    Resource r = getResource("file:error.properties");
    if (r != null) return r;

    r = getResource("classpath:" + AbstractFallbackResourceCondition.EXT_CLASSPATH + "error.properties");
    if (r != null) return r;

    return getResource("classpath:error.properties");
  }

  private Resource getResource(String resource) {
    Resource r = ctx.getResource(resource);

    return r.exists() ? r : null;
  }

  private void createError(String c, Properties p, Map<Integer, ResponseContext> map) {
    try {
      String keyPart = c.substring(0, c.length() - CODE.length());

      int code = Integer.parseInt(p.getProperty(c));
      String function = p.getProperty(keyPart + FUNCTION);
      String description = p.getProperty(keyPart + DESCRIPTION);

      log.trace("Creating error code {}, function {}, description '{}'", code, function, description);

      map.put(code, new ResponseContext(code, function, description));
    } catch (Exception e) {
      log.error("***********************************");
      log.error("Malformed error properties for code {}", c);
      log.error("***********************************");
      p.entrySet().forEach(f -> log.warn("{} = {}", f.getKey(), f.getValue()));
    }
  }

  private List<String> getCodes(Properties p) {
    return p.stringPropertyNames().stream().filter(s -> s.endsWith(CODE)).collect(Collectors.toList());
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
}

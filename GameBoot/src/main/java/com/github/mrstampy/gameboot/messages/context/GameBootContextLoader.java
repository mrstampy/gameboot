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

import static org.apache.commons.lang3.StringUtils.isNotEmpty;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.io.Resource;

import com.github.mrstampy.gameboot.util.resource.AbstractFallbackResourceCondition;

/**
 * The Class GameBootErrorLoader. {@link Locale} driven, the file
 * 'errors.properties' must exist as the root file. {@link Locale} specific
 * files are named as per property {@link ResourceBundle}s ie.
 * 'errors_en_CA.properties' or 'errors_fr.properties'. Lookups are performed
 * from specific -> general, '_[lang code]_[country code]' first, then '_[lang
 * code]', then the root 'errors.properties'.
 */
public class GameBootContextLoader implements ApplicationContextAware, ResponseContextLoader {
  private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private static final String CODE = ".code";
  private static final String FUNCTION = ".function";
  private static final String DESCRIPTION = ".description";

  private ApplicationContext ctx;

  @Value("#{'${game.boot.additional.locales}'.split(',')}")
  private List<String> additionalLocales;

  /*
   * (non-Javadoc)
   * 
   * @see
   * com.github.mrstampy.gameboot.messages.error.ErrorLoader#getErrorProperties(
   * )
   */
  @Override
  public Map<String, Map<Integer, ResponseContext>> getErrorProperties() throws Exception {
    Map<String, Map<Integer, ResponseContext>> map = new ConcurrentHashMap<>();

    Locale[] locales = Locale.getAvailableLocales();

    for (Locale locale : locales) {
      if (isNotEmpty(locale.getCountry())) {
        String suffix = "_" + locale.getLanguage() + "_" + locale.getCountry();
        addToMap(map, suffix);
      }

      String suffix = "_" + locale.getLanguage();
      addToMap(map, suffix);
    }

    additionalLocales.forEach(suffix -> {
      try {
        addToMap(map, suffix);
      } catch (Exception e) {
        log.error("Unexpected exception", e);
      }
    });

    Map<Integer, ResponseContext> root = getForLocale(GameBootContextLookup.ROOT);
    if (root == null) throw new IllegalStateException("No error.properties file");

    map.put(GameBootContextLookup.ROOT, root);

    return map;
  }

  /**
   * Adds the to map.
   *
   * @param map
   *          the map
   * @param suffix
   *          the suffix
   * @throws IOException
   *           Signals that an I/O exception has occurred.
   */
  public void addToMap(Map<String, Map<Integer, ResponseContext>> map, String suffix) throws IOException {
    if (!map.containsKey(suffix)) {
      Map<Integer, ResponseContext> full = getForLocale(suffix);
      if (full != null) map.put(suffix, full);
    }
  }

  /**
   * Gets the for locale.
   *
   * @param suffix
   *          the suffix
   * @return the for locale
   * @throws IOException
   *           Signals that an I/O exception has occurred.
   */
  public Map<Integer, ResponseContext> getForLocale(String suffix) throws IOException {
    Resource r = getOverridableErrorResource(suffix);

    if (r == null || !r.exists()) {
      log.debug("No error.properties for {}", suffix);
      return null;
    }

    Properties p = new Properties();
    p.load(r.getInputStream());

    List<String> codes = getCodes(p);

    Map<Integer, ResponseContext> map = new ConcurrentHashMap<>();

    codes.forEach(c -> createError(c, p, map));
    return map;
  }

  private Resource getOverridableErrorResource(String suffix) {
    Resource r = getResource("file:error" + suffix + ".properties");
    if (r != null) return r;

    r = getResource("classpath:" + AbstractFallbackResourceCondition.EXT_CLASSPATH + "error" + suffix + ".properties");
    if (r != null) return r;

    return getResource("classpath:error" + suffix + ".properties");
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

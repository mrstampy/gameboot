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
package com.github.mrstampy.gameboot.otp;

import static org.apache.commons.lang3.StringUtils.isEmpty;

import java.lang.invoke.MethodHandles;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * The Class KeyRegistry.
 */
@Component
public class KeyRegistry {
  private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private Map<Object, String> keys = new ConcurrentHashMap<>();

  /**
   * Puts the otp key (secret) into the registry using the lookup key provided.
   *
   * @param <T>
   *          the generic type
   * @param lookup
   *          the lookup
   * @param secret
   *          the otp key
   */
  public <T> void put(T lookup, String secret) {
    check(lookup);
    checkSecret(secret);

    log.debug("Adding otp key with lookup {}", lookup);

    keys.put(lookup, secret);
  }

  /**
   * Gets the otp key (secret) from the registry using the lookup key provided.
   *
   * @param <T>
   *          the generic type
   * @param lookup
   *          the lookup
   * @return the otp key
   */
  public <T> String get(T lookup) {
    check(lookup);

    log.debug("Returning otp key with lookup {}", lookup);

    return keys.get(lookup);
  }

  /**
   * Removes the otp key (secret) from the registry using the lookup key
   * provided.
   *
   * @param <T>
   *          the generic type
   * @param lookup
   *          the lookup
   * @return the otp key
   */
  public <T> String remove(T lookup) {
    check(lookup);

    log.debug("Removing otp key with lookup {}", lookup);

    return keys.remove(lookup);
  }

  /**
   * Contains.
   *
   * @param <T>
   *          the generic type
   * @param lookup
   *          the lookup
   * @return true, if successful
   */
  public <T> boolean contains(T lookup) {
    check(lookup);
    return keys.containsKey(lookup);
  }

  private void checkSecret(String secret) {
    if (isEmpty(secret)) fail("No otp key");
  }

  private <T> void check(T lookup) {
    if (lookup == null) fail("No lookup for otp key");

    if (lookup instanceof String) {
      if (isEmpty((String) lookup)) fail("No lookup for otp key");
    }
  }

  private void fail(String message) {
    throw new IllegalArgumentException(message);
  }

}

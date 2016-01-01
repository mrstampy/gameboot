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
package com.github.mrstampy.gameboot.util.registry;

import java.lang.invoke.MethodHandles;
import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Simple registry superclass backed by a {@link ConcurrentHashMap}. It is
 * recommended when using a {@link Number} as a key to subclass
 * {@link AbstractRegistryKey} in order to be able to type the key and avoid
 * collisions.
 *
 * @param <V>
 *          the value type
 */
public abstract class GameBootRegistry<V> {

  private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  /** The map. */
  protected Map<AbstractRegistryKey<?>, V> map = new ConcurrentHashMap<>();

  /**
   * Put.
   *
   * @param key
   *          the key
   * @param value
   *          the value
   */
  public void put(AbstractRegistryKey<?> key, V value) {
    checkKey(key);
    checkValue(value);

    if (isLogOk()) log.debug("Registering {} = {} in {}", key, value, getClass().getSimpleName());

    map.put(key, value);
  }

  /**
   * Override in subclassed to disable logging (debug level) for registries
   * containing sensitive information. Default true.
   * 
   * @return true if ok to log
   */
  protected boolean isLogOk() {
    return true;
  }

  /**
   * Gets the.
   *
   * @param key
   *          the key
   * @return the v
   */
  public V get(AbstractRegistryKey<?> key) {
    checkKey(key);

    return map.get(key);
  }

  /**
   * Removes the.
   *
   * @param key
   *          the key
   * @return the v
   */
  public V remove(AbstractRegistryKey<?> key) {
    checkKey(key);

    if (isLogOk()) log.debug("Deregistering {} in {}", key, getClass().getSimpleName());

    return map.remove(key);
  }

  /**
   * Contains.
   *
   * @param key
   *          the key
   * @return true, if successful
   */
  public boolean contains(AbstractRegistryKey<?> key) {
    checkKey(key);
    return map.containsKey(key);
  }

  /**
   * Contains value.
   *
   * @param value
   *          the value
   * @return true, if successful
   */
  public boolean containsValue(V value) {
    checkValue(value);
    return map.containsValue(value);
  }

  /**
   * Size.
   *
   * @return the int
   */
  public int size() {
    return map.size();
  }

  /**
   * Gets the keys for value.
   *
   * @param value
   *          the value
   * @return the keys for value
   */
  public Set<Entry<AbstractRegistryKey<?>, V>> getKeysForValue(V value) {
    return Collections
        .unmodifiableSet(map.entrySet().stream().filter(e -> isValue(e, value)).collect(Collectors.toSet()));
  }

  private boolean isValue(Entry<AbstractRegistryKey<?>, V> e, V value) {
    return value == e.getValue() || value.equals(e.getValue());
  }

  /**
   * Check key.
   *
   * @param key
   *          the key
   */
  protected void checkKey(AbstractRegistryKey<?> key) {
    if (key == null) fail("No key");
  }

  /**
   * Check value.
   *
   * @param value
   *          the value
   */
  protected void checkValue(V value) {
    if (value == null) fail("No value");
  }

  /**
   * Fail.
   *
   * @param message
   *          the message
   */
  protected void fail(String message) {
    throw new IllegalArgumentException(message);
  }

}

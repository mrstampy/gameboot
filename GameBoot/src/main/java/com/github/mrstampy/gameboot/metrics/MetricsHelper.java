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

import static com.codahale.metrics.MetricRegistry.name;

import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Gauge;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.codahale.metrics.Timer.Context;
import com.github.mrstampy.gameboot.exception.GameBootRuntimeException;;

/**
 * The {@link MetricRegistry} has an excellent naming standard which defines the
 * class in which the metric is used and qualifiers describing its function.
 * However for programmatic lookups it is a pain to reconstruct. This class
 * manages {@link Timer}s, {@link Counter}s and {@link Gauge}s within GameBoot
 * providing lookups with a simple alternate key. This negates the need to keep
 * a reference to the metric object and provides a single interface to
 * {@link #startTimer(String)}s and {@link #incr(String)} counters. Metrics must
 * first be registered in one of {@link #counter(String, Class, String...)},
 * {@link #timer(String, Class, String...)} or
 * {@link #gauge(Gauge, String, Class, String...)}, usually in a
 * {@link PostConstruct} block.
 */
@Component
public class MetricsHelper {

  @Autowired
  private MetricRegistry registry;

  private Map<String, Timer> timers = new ConcurrentHashMap<>();

  private Map<String, Counter> counters = new ConcurrentHashMap<>();

  private Map<String, Gauge<?>> gauges = new ConcurrentHashMap<>();

  /**
   * Counter register.
   *
   * @param key
   *          the key
   * @param clz
   *          the clz
   * @param qualifiers
   *          the qualifiers
   */
  public void counter(String key, Class<?> clz, String... qualifiers) {
    if (counters.containsKey(key)) throw new GameBootRuntimeException(key + " already exists");
    counters.put(key, registry.counter(name(clz, qualifiers)));
  }

  /**
   * Timer register.
   *
   * @param key
   *          the key
   * @param clz
   *          the clz
   * @param qualifiers
   *          the qualifiers
   */
  public void timer(String key, Class<?> clz, String... qualifiers) {
    if (timers.containsKey(key)) throw new GameBootRuntimeException(key + " already exists");
    timers.put(key, registry.timer(name(clz, qualifiers)));
  }

  /**
   * Gauge register.
   *
   * @param gauge
   *          the gauge
   * @param key
   *          the key
   * @param clz
   *          the clz
   * @param qualifiers
   *          the qualifiers
   */
  public void gauge(Gauge<?> gauge, String key, Class<?> clz, String... qualifiers) {
    if (gauges.containsKey(key)) throw new GameBootRuntimeException(key + " already exists");
    gauges.put(key, registry.register(name(clz, qualifiers), gauge));
  }

  /**
   * Gets the timers.
   *
   * @return the timers
   */
  public Set<Entry<String, Timer>> getTimers() {
    return Collections.unmodifiableSet(timers.entrySet());
  }

  /**
   * Gets the counters.
   *
   * @return the counters
   */
  public Set<Entry<String, Counter>> getCounters() {
    return Collections.unmodifiableSet(counters.entrySet());
  }

  /**
   * Gets the gauges.
   *
   * @return the gauges
   */
  public Set<Entry<String, Gauge<?>>> getGauges() {
    return Collections.unmodifiableSet(gauges.entrySet());
  }

  /**
   * Start timer.
   *
   * @param key
   *          the key
   * @return the context
   */
  public Context startTimer(String key) {
    Timer t = timers.get(key);

    if (t == null) throw new IllegalArgumentException("No timer for key " + key);

    Context ctx = t.time();

    return ctx;
  }

  /**
   * Incr counter.
   *
   * @param key
   *          the key
   */
  public void incr(String key) {
    getCounter(key).inc();
  }

  /**
   * Decr counter.
   *
   * @param key
   *          the key
   */
  public void decr(String key) {
    getCounter(key).dec();
  }

  private Counter getCounter(String key) {
    Counter c = counters.get(key);

    if (c == null) throw new IllegalArgumentException("No counter for key " + key);

    return c;
  }
}

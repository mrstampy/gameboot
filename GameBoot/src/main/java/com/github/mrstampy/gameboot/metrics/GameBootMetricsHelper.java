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
package com.github.mrstampy.gameboot.metrics;

import static com.codahale.metrics.MetricRegistry.name;

import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Gauge;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.codahale.metrics.Timer.Context;;

/**
 * The default implementation of {@link MetricsHelper}.
 * 
 * @see GameBootMetricsConfiguration
 */
public class GameBootMetricsHelper implements MetricsHelper {

  @Autowired
  private MetricRegistry registry;

  private Map<String, Timer> timers = new ConcurrentHashMap<>();

  private Map<String, Counter> counters = new ConcurrentHashMap<>();

  private Map<String, Gauge<?>> gauges = new ConcurrentHashMap<>();

  /*
   * (non-Javadoc)
   * 
   * @see
   * com.github.mrstampy.gameboot.metrics.MetHelper#counter(java.lang.String,
   * java.lang.Class, java.lang.String)
   */
  @Override
  public void counter(String key, Class<?> clz, String... qualifiers) {
    check(key);
    if (counters.containsKey(key)) throw new IllegalArgumentException(key + " already exists");
    counters.put(key, registry.counter(name(clz, qualifiers)));
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.github.mrstampy.gameboot.metrics.MetHelper#timer(java.lang.String,
   * java.lang.Class, java.lang.String)
   */
  @Override
  public void timer(String key, Class<?> clz, String... qualifiers) {
    check(key);
    if (timers.containsKey(key)) throw new IllegalArgumentException(key + " already exists");
    timers.put(key, registry.timer(name(clz, qualifiers)));
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * com.github.mrstampy.gameboot.metrics.MetHelper#gauge(com.codahale.metrics.
   * Gauge, java.lang.String, java.lang.Class, java.lang.String)
   */
  @Override
  public void gauge(Gauge<?> gauge, String key, Class<?> clz, String... qualifiers) {
    check(key);
    if (gauges.containsKey(key)) throw new IllegalArgumentException(key + " already exists");
    gauges.put(key, registry.register(name(clz, qualifiers), gauge));
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.github.mrstampy.gameboot.metrics.MetHelper#getTimers()
   */
  @Override
  public Set<Entry<String, Timer>> getTimers() {
    return Collections.unmodifiableSet(timers.entrySet());
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.github.mrstampy.gameboot.metrics.MetHelper#getCounters()
   */
  @Override
  public Set<Entry<String, Counter>> getCounters() {
    return Collections.unmodifiableSet(counters.entrySet());
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.github.mrstampy.gameboot.metrics.MetHelper#getGauges()
   */
  @Override
  public Set<Entry<String, Gauge<?>>> getGauges() {
    return Collections.unmodifiableSet(gauges.entrySet());
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * com.github.mrstampy.gameboot.metrics.MetHelper#containsCounter(java.lang.
   * String)
   */
  @Override
  public boolean containsCounter(String key) {
    check(key);
    return counters.containsKey(key);
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * com.github.mrstampy.gameboot.metrics.MetHelper#containsGauge(java.lang.
   * String)
   */
  @Override
  public boolean containsGauge(String key) {
    check(key);
    return gauges.containsKey(key);
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * com.github.mrstampy.gameboot.metrics.MetHelper#containsTimer(java.lang.
   * String)
   */
  @Override
  public boolean containsTimer(String key) {
    check(key);
    return timers.containsKey(key);
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * com.github.mrstampy.gameboot.metrics.MetHelper#startTimer(java.lang.String)
   */
  @Override
  public Optional<Context> startTimer(String key) {
    check(key);
    Timer t = timers.get(key);

    if (t == null) throw new IllegalArgumentException("No timer for key " + key);

    Context ctx = t.time();

    return Optional.of(ctx);
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * com.github.mrstampy.gameboot.metrics.MetricsHelper#stopTimer(java.util.
   * Optional)
   */
  public void stopTimer(Optional<Context> ctx) {
    if (ctx.isPresent()) ctx.get().stop();
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.github.mrstampy.gameboot.metrics.MetHelper#incr(java.lang.String)
   */
  @Override
  public void incr(String key) {
    check(key);
    getCounter(key).inc();
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.github.mrstampy.gameboot.metrics.MetHelper#decr(java.lang.String)
   */
  @Override
  public void decr(String key) {
    check(key);
    getCounter(key).dec();
  }

  private void check(String key) {
    if (StringUtils.isEmpty(key)) throw new IllegalArgumentException("No key specified");
  }

  private Counter getCounter(String key) {
    Counter c = counters.get(key);

    if (c == null) throw new IllegalArgumentException("No counter for key " + key);

    return c;
  }
}

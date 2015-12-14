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

import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;

import javax.annotation.PostConstruct;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Gauge;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.codahale.metrics.Timer.Context;

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
public interface MetricsHelper {

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
  void counter(String key, Class<?> clz, String... qualifiers);

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
  void timer(String key, Class<?> clz, String... qualifiers);

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
  void gauge(Gauge<?> gauge, String key, Class<?> clz, String... qualifiers);

  /**
   * Gets the timers.
   *
   * @return the timers
   */
  Set<Entry<String, Timer>> getTimers();

  /**
   * Gets the counters.
   *
   * @return the counters
   */
  Set<Entry<String, Counter>> getCounters();

  /**
   * Gets the gauges.
   *
   * @return the gauges
   */
  Set<Entry<String, Gauge<?>>> getGauges();

  /**
   * Contains counter.
   *
   * @param key
   *          the key
   * @return true, if successful
   */
  boolean containsCounter(String key);

  /**
   * Contains gauge.
   *
   * @param key
   *          the key
   * @return true, if successful
   */
  boolean containsGauge(String key);

  /**
   * Contains timer.
   *
   * @param key
   *          the key
   * @return true, if successful
   */
  boolean containsTimer(String key);

  /**
   * Start timer.
   *
   * @param key
   *          the key
   * @return the optional
   */
  Optional<Context> startTimer(String key);

  /**
   * Stop timer.
   *
   * @param ctx
   *          the ctx
   */
  void stopTimer(Optional<Context> ctx);

  /**
   * Incr counter.
   *
   * @param key
   *          the key
   */
  void incr(String key);

  /**
   * Decr counter.
   *
   * @param key
   *          the key
   */
  void decr(String key);

}
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

import java.util.Collections;
import java.util.Map.Entry;
import java.util.Set;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Gauge;
import com.codahale.metrics.Timer;

/**
 * The Class NullMetricsHelper allows the disabling of GameBoot metrics
 * acquisition.
 * 
 * @see GameBootMetricsConfiguration
 */
public class NullMetricsHelper implements MetricsHelper {

  /*
   * (non-Javadoc)
   * 
   * @see com.github.mrstampy.gameboot.metrics.MetricsHelper#counter(java.lang.
   * String, java.lang.Class, java.lang.String[])
   */
  @Override
  public void counter(String key, Class<?> clz, String... qualifiers) {
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * com.github.mrstampy.gameboot.metrics.MetricsHelper#timer(java.lang.String,
   * java.lang.Class, java.lang.String[])
   */
  @Override
  public void timer(String key, Class<?> clz, String... qualifiers) {
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.github.mrstampy.gameboot.metrics.MetricsHelper#gauge(com.codahale.
   * metrics.Gauge, java.lang.String, java.lang.Class, java.lang.String[])
   */
  @Override
  public void gauge(Gauge<?> gauge, String key, Class<?> clz, String... qualifiers) {
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.github.mrstampy.gameboot.metrics.MetricsHelper#getTimers()
   */
  @SuppressWarnings("unchecked")
  @Override
  public Set<Entry<String, Timer>> getTimers() {
    return Collections.EMPTY_SET;
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.github.mrstampy.gameboot.metrics.MetricsHelper#getCounters()
   */
  @SuppressWarnings("unchecked")
  @Override
  public Set<Entry<String, Counter>> getCounters() {
    return Collections.EMPTY_SET;
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.github.mrstampy.gameboot.metrics.MetricsHelper#getGauges()
   */
  @SuppressWarnings("unchecked")
  @Override
  public Set<Entry<String, Gauge<?>>> getGauges() {
    return Collections.EMPTY_SET;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * com.github.mrstampy.gameboot.metrics.MetricsHelper#containsCounter(java.
   * lang.String)
   */
  @Override
  public boolean containsCounter(String key) {
    return true;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * com.github.mrstampy.gameboot.metrics.MetricsHelper#containsGauge(java.lang.
   * String)
   */
  @Override
  public boolean containsGauge(String key) {
    return true;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * com.github.mrstampy.gameboot.metrics.MetricsHelper#containsTimer(java.lang.
   * String)
   */
  @Override
  public boolean containsTimer(String key) {
    return true;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * com.github.mrstampy.gameboot.metrics.MetricsHelper#startTimer(java.lang.
   * String)
   */
  @Override
  public void startTimer(String key) {
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * com.github.mrstampy.gameboot.metrics.MetricsHelper#stopTimer(java.lang.
   * String)
   */
  @Override
  public void stopTimer(String key) {
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * com.github.mrstampy.gameboot.metrics.MetricsHelper#incr(java.lang.String)
   */
  @Override
  public void incr(String key) {
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * com.github.mrstampy.gameboot.metrics.MetricsHelper#decr(java.lang.String)
   */
  @Override
  public void decr(String key) {
  }

}

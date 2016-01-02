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
package com.github.mrstampy.gameboot.web;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.github.mrstampy.gameboot.metrics.MetricsHelper;
import com.github.mrstampy.gameboot.systemid.SystemId;
import com.github.mrstampy.gameboot.util.registry.AbstractRegistryKey;
import com.github.mrstampy.gameboot.util.registry.GameBootRegistry;
import com.github.mrstampy.gameboot.util.registry.RegistryCleaner;

/**
 * The Class HttpSessionRegistry.
 */
@Component
public class HttpSessionRegistry extends GameBootRegistry<HttpSession> {

  private static final String WEB_CONNECTIONS = "Web Connections";

  @Autowired
  private ScheduledExecutorService svc;

  @Autowired
  private MetricsHelper helper;

  @Value("${http.session.expiry.seconds}")
  private int expiry;

  @Autowired
  private RegistryCleaner cleaner;

  private Map<Comparable<?>, ScheduledFuture<?>> futures = new ConcurrentHashMap<>();

  @PostConstruct
  public void postConstruct() throws Exception {
    helper.gauge(() -> size(), WEB_CONNECTIONS, getClass(), "web", "connections");
  }

  /**
   * Puts the newly generated key paired against the {@link SystemId#next()} id
   * value of the http session.
   *
   * @param key
   *          the key
   * @param value
   *          the value
   */
  @Override
  public void put(AbstractRegistryKey<?> key, HttpSession value) {
    if (contains(key)) return;

    ScheduledFuture<?> sf = futures.remove(key);
    if (sf != null) sf.cancel(true);

    super.put(key, value);

    sf = svc.schedule(() -> cleanup(key, value), expiry, TimeUnit.SECONDS);
    futures.put(key, sf);
  }

  /**
   * Removes the HttpSession.
   *
   * @param key
   *          the key
   * @return the byte[]
   */
  @Override
  public HttpSession remove(AbstractRegistryKey<?> key) {
    HttpSession session = super.remove(key);

    ScheduledFuture<?> sf = futures.remove(key);
    if (sf != null) sf.cancel(true);

    return session;
  }

  public void restartExpiry(AbstractRegistryKey<?> key) {
    HttpSession session = remove(key);

    put(key, session);
  }

  private void cleanup(AbstractRegistryKey<?> key, HttpSession value) {
    super.remove(key);
    futures.remove(key);
    cleaner.cleanup(key);
  }

}

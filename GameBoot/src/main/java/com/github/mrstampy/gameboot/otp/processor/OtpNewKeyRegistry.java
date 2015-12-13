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
package com.github.mrstampy.gameboot.otp.processor;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.github.mrstampy.gameboot.util.GameBootRegistry;

/**
 * The Class OtpNewKeyRegistry.
 */
@Component
public class OtpNewKeyRegistry extends GameBootRegistry<byte[]> {

  @Autowired
  private ScheduledExecutorService svc;

  private Map<Comparable<?>, ScheduledFuture<?>> futures = new ConcurrentHashMap<>();

  /*
   * (non-Javadoc)
   * 
   * @see com.github.mrstampy.gameboot.util.GameBootRegistry#put(java.lang.
   * Comparable, java.lang.Object)
   */
  public void put(Comparable<?> key, byte[] value) {
    ScheduledFuture<?> sf = futures.remove(key);
    if (sf != null) sf.cancel(true);

    super.put(key, value);

    sf = svc.schedule(() -> cleanup(key), 30, TimeUnit.SECONDS);
    futures.put(key, sf);
  }

  private void cleanup(Comparable<?> key) {
    remove(key);
    futures.remove(key);
  }

}

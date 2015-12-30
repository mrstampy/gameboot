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
package com.github.mrstampy.gameboot.systemid;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Configuration;

import com.github.mrstampy.gameboot.security.SecurityConfiguration;
import com.github.mrstampy.gameboot.util.RegistryCleanerListener;

/**
 * Default implementation of the {@link SystemId} interface, using the
 * {@link SecurityConfiguration#secureRandom()} to generate greater-than-zero
 * system unique ids. GameBoot implementations using persistent storage will
 * want to add their own implementation in their {@link Configuration}.
 */
public class GameBootSystemId implements SystemId, RegistryCleanerListener {

  @Autowired
  @Qualifier(SecurityConfiguration.GAME_BOOT_SECURE_RANDOM)
  private SecureRandom random;

  private List<SystemIdWrapper> activeIds = new ArrayList<>();

  private Lock lock = new ReentrantLock();

  /*
   * (non-Javadoc)
   * 
   * @see com.github.mrstampy.gameboot.SystemId#next()
   */
  @Override
  public SystemIdWrapper next() {
    lock.lock();
    try {
      SystemIdWrapper siw = new SystemIdWrapper(random.nextLong());

      while (siw.getId().longValue() <= 0 || activeIds.contains(siw)) {
        siw = new SystemIdWrapper(random.nextLong());
      }

      activeIds.add(siw);

      return siw;
    } finally {
      lock.unlock();
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * com.github.mrstampy.gameboot.util.RegistryCleanerListener#cleanup(java.lang
   * .Comparable)
   */
  @Override
  public void cleanup(Comparable<?> key) {
    if (!(key instanceof SystemIdWrapper)) return;

    lock.lock();
    try {
      activeIds.remove(key);
    } finally {
      lock.unlock();
    }
  }
}

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
package com.github.mrstampy.gameboot.otp.processor;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.github.mrstampy.gameboot.SystemId;
import com.github.mrstampy.gameboot.otp.KeyRegistry;
import com.github.mrstampy.gameboot.otp.OtpConfiguration;
import com.github.mrstampy.gameboot.otp.netty.OtpClearNettyHandler;
import com.github.mrstampy.gameboot.otp.netty.OtpEncryptedNettyHandler;
import com.github.mrstampy.gameboot.otp.websocket.OtpClearWebSocketHandler;
import com.github.mrstampy.gameboot.otp.websocket.OtpEncryptedWebSocketHandler;
import com.github.mrstampy.gameboot.util.GameBootRegistry;

/**
 * The Class OtpNewKeyRegistry acts as a temporary in-memory storage of newly
 * generated OTP keys intended for clear channel encryption.
 * 
 * @see OtpClearNettyHandler
 * @see OtpEncryptedNettyHandler
 * @see OtpClearWebSocketHandler
 * @see OtpEncryptedWebSocketHandler
 * @see OtpNewKeyAckProcessor
 * @see OtpKeyRequestProcessor
 * @see KeyRegistry
 */
@Component
@Profile(OtpConfiguration.OTP_PROFILE)
public class OtpNewKeyRegistry extends GameBootRegistry<byte[]> {

  @Autowired
  private ScheduledExecutorService svc;

  @Value("otp.new.key.expiry.seconds")
  private int newKeyExpiry;

  @PostConstruct
  public void postConstruct() throws Exception {
    if (newKeyExpiry <= 0) throw new IllegalStateException("otp.new.key.expiry.seconds must be > 0");
  }

  private Map<Comparable<?>, ScheduledFuture<?>> futures = new ConcurrentHashMap<>();

  /**
   * Puts the newly generated key paired against the {@link SystemId#next()} id
   * value of the clear connection.
   *
   * @param key
   *          the key
   * @param value
   *          the value
   */
  @Override
  public void put(Comparable<?> key, byte[] value) {
    ScheduledFuture<?> sf = futures.remove(key);
    if (sf != null) sf.cancel(true);

    super.put(key, value);

    sf = svc.schedule(() -> cleanup(key), newKeyExpiry, TimeUnit.SECONDS);
    futures.put(key, sf);
  }

  /**
   * Removes the newly generated key for activation.
   *
   * @param key
   *          the key
   * @return the byte[]
   */
  @Override
  public byte[] remove(Comparable<?> key) {
    byte[] b = super.remove(key);

    ScheduledFuture<?> sf = futures.remove(key);
    if (sf != null) sf.cancel(true);

    return b;
  }

  private void cleanup(Comparable<?> key) {
    super.remove(key);
    futures.remove(key);
  }

}

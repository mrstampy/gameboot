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
package com.github.mrstampy.gameboot.otp;

import java.security.SecureRandom;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.context.annotation.Scope;

import com.github.mrstampy.gameboot.otp.netty.OtpClearNettyHandler;
import com.github.mrstampy.gameboot.otp.websocket.OtpClearWebSocketHandler;

/**
 * The Class OtpConfiguration.
 */
@Configuration
@Profile(OtpConfiguration.OTP_PROFILE)
public class OtpConfiguration {

  public static final String OTP_SECURE_RANDOM = "OTP Secure Random";

  public static final String OTP_PROFILE = "otp";

  @Value("${otp.secure.random.seed.size}")
  private int seedSize;

  /**
   * Clear netty handler.
   *
   * @return the otp clear netty handler
   */
  @Bean
  @ConditionalOnMissingBean(OtpClearNettyHandler.class)
  @Scope("prototype")
  public OtpClearNettyHandler clearNettyHandler() {
    return new OtpClearNettyHandler();
  }

  /**
   * Clear web socket handler.
   *
   * @return the otp clear web socket handler
   */
  @Bean
  @ConditionalOnMissingBean(OtpClearWebSocketHandler.class)
  public OtpClearWebSocketHandler clearWebSocketHandler() {
    return new OtpClearWebSocketHandler();
  }

  /**
   * Returns a strong instance of the secure random seeded with a byte array the
   * size of which is specified by the gameboot property
   * 'otp.secure.random.seed.size'.
   *
   * @return the secure random
   * @throws Exception
   *           the exception
   */
  @Bean(name = OTP_SECURE_RANDOM)
  public SecureRandom secureRandom() throws Exception {
    SecureRandom random = SecureRandom.getInstanceStrong();

    byte[] seed = new byte[seedSize];
    random.nextBytes(seed);

    return random;
  }

}

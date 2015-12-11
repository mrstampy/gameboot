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
package com.github.mrstampy.gameboot.otp;

import static org.apache.commons.lang3.StringUtils.isEmpty;

import java.security.SecureRandom;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * The Class OneTimePad.
 */
@Component
public class OneTimePad {

  @Autowired
  private SecureRandom random;

  /**
   * Generate key.
   *
   * @param size
   *          the size
   * @return the string
   * @throws Exception
   *           the exception
   */
  public String generateKey(int size) throws Exception {
    check(size);

    byte[] key = new byte[size];

    random.nextBytes(key);

    return new String(key);
  }

  /**
   * Encrypt.
   *
   * @param key
   *          the key
   * @param decoded
   *          the decoded
   * @return the string
   * @throws Exception
   *           the exception
   */
  public String encrypt(String key, String decoded) throws Exception {
    return otp(key, decoded);
  }

  /**
   * Decrypt.
   *
   * @param key
   *          the key
   * @param encoded
   *          the encoded
   * @return the string
   * @throws Exception
   *           the exception
   */
  public String decrypt(String key, String encoded) throws Exception {
    return otp(key, encoded);
  }

  private String otp(String key, String message) {
    check(key, message);

    byte[] kb = key.getBytes();
    byte[] mb = message.getBytes();
    byte[] ed = new byte[mb.length];

    for (int i = 0; i < mb.length; i++) {
      ed[i] = (byte) (mb[i] ^ kb[i]);
    }

    return new String(ed);
  }

  private void check(int size) {
    if (size <= 0) fail("Size must be > 0");
  }

  private void check(String key, String message) {
    if (isEmpty(key)) fail("No key");
    if (isEmpty(message)) fail("No message");

    if (key.length() < message.length()) fail("Key length too short for message");
  }

  private void fail(String message) {
    throw new IllegalArgumentException(message);
  }
}

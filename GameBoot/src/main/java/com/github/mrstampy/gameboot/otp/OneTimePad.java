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
 * The Class OneTimePad is an implementation of the <a href="">One Time Pad</a>
 * algorithm. The intended use is to generate a secret key which is passed
 * securely to the client (https, wss etc). All messages are then encrypted and
 * decrypted with this key and the encrypted messages can be exchanged clear
 * text (http, ws etc), negating the overhead of secure connections.<br>
 * <br>
 * 
 * Key length must be >= message length.
 * 
 * @see KeyRegistry
 */
@Component
public class OneTimePad {

  @Autowired
  private SecureRandom random;

  /**
   * Generate key to share securely (https/wss) with the client.
   *
   * @param size
   *          the size of the key string
   * @return the string
   * @throws Exception
   *           the exception
   * @see KeyRegistry
   */
  public byte[] generateKey(int size) throws Exception {
    check(size);

    byte[] key = new byte[size];

    random.nextBytes(key);

    return key;
  }

  /**
   * Will encode the message if decoded, decode the message if encoded.
   *
   * @param key
   *          the key
   * @param message
   *          the message
   * @return the converted string
   * @throws Exception
   *           the exception
   */
  public String convert(byte[] key, String message) throws Exception {
    check(key, message);

    return new String(convertImpl(key, message.getBytes()));
  }

  /**
   * Will encode the message if decoded, decode the message if encoded.
   *
   * @param key
   *          the key
   * @param message
   *          the message byte array
   * @return the converted byte array
   * @throws Exception
   *           the exception
   */
  public byte[] convert(byte[] key, byte[] message) throws Exception {
    check(key, message);

    return convertImpl(key, message);
  }

  private byte[] convertImpl(byte[] key, byte[] message) {
    byte[] converted = new byte[message.length];

    for (int i = 0; i < message.length; i++) {
      converted[i] = (byte) (message[i] ^ key[i]);
    }

    return converted;
  }

  private void check(int size) {
    if (size <= 0) fail("Size must be > 0");
  }

  private void check(byte[] key, String message) {
    keyCheck(key);
    if (isEmpty(message)) fail("No message");

    if (key.length < message.length()) fail("Key length too short for message");
  }

  private void check(byte[] key, byte[] message) {
    keyCheck(key);
    if (mtArray(message)) fail("No message");

    if (key.length < message.length) fail("Key length too short for message");
  }

  private void keyCheck(byte[] key) {
    if (mtArray(key)) fail("No key");
  }

  protected boolean mtArray(byte[] key) {
    return key == null || key.length == 0;
  }

  private void fail(String message) {
    throw new IllegalArgumentException(message);
  }
}

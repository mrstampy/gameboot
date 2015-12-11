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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.fail;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.github.mrstampy.gameboot.TestConfiguration;

/**
 * The Class OneTimePadTest.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(TestConfiguration.class)
public class OneTimePadTest {

  @Autowired
  private OneTimePad pad;

  /**
   * Test one time pad.
   *
   * @throws Exception
   *           the exception
   */
  @Test
  public void testOneTimePad() throws Exception {
    byte[] shush = pad.generateKey(5);
    byte[] msg = "Hello".getBytes();

    byte[] converted = pad.convert(shush, msg);
    assertNotEquals(new String(msg), new String(converted));

    converted = pad.convert(shush, converted);

    assertEquals(new String(msg), new String(converted));
  }

  /**
   * Test illegal args.
   *
   * @throws Exception
   *           the exception
   */
  @Test
  public void testIllegalArgs() throws Exception {
    byte[] key = null;
    byte[] msg = "Hello".getBytes();

    illegalArgumentExpected(key, msg, "null key");

    key = new byte[0];

    illegalArgumentExpected(key, msg, "mt key");

    key = new byte[1];

    illegalArgumentExpected(key, msg, "key too short");

    msg = new byte[0];

    illegalArgumentExpected(key, msg, "mt byte[] message");

    key = new byte[5];
    msg = null;

    illegalArgumentExpected(key, msg, "null byte[] message");
  }

  private void illegalArgumentExpected(byte[] key, byte[] message, String failMsg) {
    illegalArgumentRunner(() -> {
      try {
        pad.convert(key, message);
      } catch (IllegalArgumentException expected) {
        throw expected;
      } catch (Exception e) {
        e.printStackTrace();
        fail(e.getMessage());
      }
    } , failMsg);
  }

  private void illegalArgumentRunner(Runnable r, String failMsg) {
    try {
      r.run();
      fail(failMsg);
    } catch (IllegalArgumentException expected) {
    }
  }

}

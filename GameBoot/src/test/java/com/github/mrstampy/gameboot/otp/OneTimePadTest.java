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

import java.lang.invoke.MethodHandles;
import java.util.Map.Entry;
import java.util.Set;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.codahale.metrics.Timer;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.mrstampy.gameboot.TestConfiguration;
import com.github.mrstampy.gameboot.metrics.MetricsHelper;

/**
 * The Class OneTimePadTest.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(TestConfiguration.class)
public class OneTimePadTest {
  private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Autowired
  private OneTimePad pad;

  @Autowired
  private MetricsHelper helper;

  @Autowired
  private ObjectMapper mapper;

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

    byte[] badkey = "This is not the key you are looking for".getBytes();
    byte[] perverted = pad.convert(badkey, converted);
    assertNotEquals(new String(msg), new String(perverted));

    converted = pad.convert(shush, converted);

    assertEquals(new String(msg), new String(converted));
  }

  /**
   * Test32 kilo byte messages for metrics.
   *
   * @throws Exception
   *           the exception
   */
  @Test
  public void test32KiloByteMessagesForMetrics() throws Exception {
    int k32 = 1024 * 32;

    for (int i = 0; i < 5; i++) {
      byte[] shush = pad.generateKey(k32);
      byte[] msg = pad.generateKey(k32);
      byte[] converted = pad.convert(shush, msg);
      assertNotEquals(new String(msg), new String(converted));

      converted = pad.convert(shush, converted);

      assertEquals(new String(msg), new String(converted));
    }

    metrics();
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

  private void metrics() throws Exception {
    Set<Entry<String, Timer>> timers = helper.getTimers();

    timers.stream().filter(e -> isMetric(e.getKey())).forEach(e -> display(e));
  }

  private boolean isMetric(String key) {
    return OneTimePad.OTP_KEY_GENERATION.equals(key) || OneTimePad.OTP_CONVERSION.equals(key);
  }

  private void display(Entry<String, ?> t) {
    try {
      log.debug(mapper.writeValueAsString(t));
    } catch (JsonProcessingException e) {
      log.error("Unexpected exception", e);
    }
  }

}

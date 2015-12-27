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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.github.mrstampy.gameboot.TestConfiguration;
import com.github.mrstampy.gameboot.exception.GameBootException;
import com.github.mrstampy.gameboot.exception.GameBootRuntimeException;
import com.github.mrstampy.gameboot.messages.Response;
import com.github.mrstampy.gameboot.messages.Response.ResponseCode;
import com.github.mrstampy.gameboot.otp.OtpConfiguration;
import com.github.mrstampy.gameboot.otp.messages.OtpKeyRequest;
import com.github.mrstampy.gameboot.otp.messages.OtpKeyRequest.KeyFunction;

/**
 * The Class OtpNewKeyRequestProcessorTest.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(TestConfiguration.class)
@ActiveProfiles(OtpConfiguration.OTP_PROFILE)
public class OtpKeyRequestProcessorTest {

  /** The Constant CLEAR_CHANNEL_ID. */
  static final Long CLEAR_CHANNEL_ID = 1234l;

  /** The Constant KEY_SIZE. */
  static final Integer KEY_SIZE = 64;

  @Autowired
  private OtpKeyRequestProcessor processor;

  @Value("${otp.maximum.key.size}")
  private Integer maxKeySize;

  /**
   * Test processor.
   *
   * @throws Exception
   *           the exception
   */
  @Test
  public void testNewKey() throws Exception {
    failExpected(null, "Null message");

    OtpKeyRequest r = new OtpKeyRequest();
    failExpected(r, "mt message");

    r.setSystemId(CLEAR_CHANNEL_ID);
    r.setKeyFunction(KeyFunction.NEW);

    r.setKeySize(-32);
    failExpected(r, "negative size");

    r.setKeySize(KEY_SIZE);
    r.setKeyFunction(null);
    failExpected(r, "No key function");

    r.setKeyFunction(KeyFunction.NEW);
    r.setKeySize(maxKeySize + 1);
    failExpected(r, "> max key size");

    r.setKeySize(KEY_SIZE);

    Response rep = processor.process(r);

    assertEquals(ResponseCode.SUCCESS, rep.getResponseCode());
    assertNotNull(rep.getResponse());
    assertEquals(1, rep.getResponse().length);
    assertTrue(rep.getResponse()[0] instanceof byte[]);

    byte[] b = (byte[]) rep.getResponse()[0];

    assertEquals(KEY_SIZE.intValue(), b.length);
  }

  private void failExpected(OtpKeyRequest m, String failMsg) {
    try {
      Response r = processor.process(m);
      switch (r.getResponseCode()) {
      case FAILURE:
        break;
      default:
        fail(failMsg);
        break;
      }
    } catch (GameBootRuntimeException | GameBootException expected) {
    } catch (Exception e) {
      fail(e.getMessage());
    }
  }
}

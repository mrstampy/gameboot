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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.github.mrstampy.gameboot.TestConfiguration;
import com.github.mrstampy.gameboot.exception.GameBootException;
import com.github.mrstampy.gameboot.exception.GameBootRuntimeException;
import com.github.mrstampy.gameboot.messages.Response;
import com.github.mrstampy.gameboot.messages.Response.ResponseCode;
import com.github.mrstampy.gameboot.otp.messages.OtpNewKeyRequest;

/**
 * The Class OtpNewKeyRequestProcessorTest.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(TestConfiguration.class)
public class OtpNewKeyRequestProcessorTest {

  @Autowired
  private OtpNewKeyRequestProcessor processor;

  /**
   * Test processor.
   *
   * @throws Exception
   *           the exception
   */
  @Test
  public void testProcessor() throws Exception {
    failExpected(null, "Null message");

    OtpNewKeyRequest r = new OtpNewKeyRequest();
    failExpected(r, "mt message");

    r.setSystemId(1234l);

    r.setSize(-32);
    failExpected(r, "negative size");

    r.setSize(7);
    failExpected(r, "bad size");

    r.setSize(64);
    Response rep = processor.process(r);

    assertEquals(ResponseCode.SUCCESS, rep.getResponseCode());
    assertNotNull(rep.getResponse());
    assertEquals(1, rep.getResponse().length);
    assertTrue(rep.getResponse()[0] instanceof byte[]);

    byte[] b = (byte[]) rep.getResponse()[0];

    assertEquals(64, b.length);
  }

  private void failExpected(OtpNewKeyRequest m, String failMsg) {
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

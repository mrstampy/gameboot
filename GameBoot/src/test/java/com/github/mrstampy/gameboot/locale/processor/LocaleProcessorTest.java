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
package com.github.mrstampy.gameboot.locale.processor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.github.mrstampy.gameboot.TestConfiguration;
import com.github.mrstampy.gameboot.exception.GameBootRuntimeException;
import com.github.mrstampy.gameboot.locale.messages.LocaleMessage;
import com.github.mrstampy.gameboot.locale.messages.LocaleRegistry;
import com.github.mrstampy.gameboot.messages.Response;
import com.github.mrstampy.gameboot.messages.Response.ResponseCode;

/**
 * The Class LocaleProcessorTest.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(TestConfiguration.class)
@ActiveProfiles(LocaleProcessor.PROFILE)
public class LocaleProcessorTest {

  @Autowired
  private LocaleProcessor processor;

  @Autowired
  private LocaleRegistry registry;

  /**
   * Test validation.
   *
   * @throws Exception
   *           the exception
   */
  @Test
  public void testValidation() throws Exception {
    validationFailExpected(null, "Null message");

    LocaleMessage msg = new LocaleMessage();
    msg.setSystemId(1l); // set by the system on the way to processing

    validationFailExpected(msg, "No lang or country codes");

    msg.setCountryCode("FR");

    validationFailExpected(msg, "No lang code");

    msg.setLanguageCode("fr");

    processor.validate(msg);
  }

  /**
   * Test process.
   *
   * @throws Exception
   *           the exception
   */
  @Test
  public void testProcess() throws Exception {
    assertEquals(0, registry.size());

    LocaleMessage msg = new LocaleMessage();
    msg.setSystemId(1l);
    msg.setLanguageCode("fr");

    Response r = processor.process(msg);

    assertNotNull(r);
    assertEquals(ResponseCode.SUCCESS, r.getResponseCode());
    assertEquals(1, registry.size());
  }

  private void validationFailExpected(LocaleMessage msg, String desc) {
    try {
      processor.validate(msg);
      fail(desc);
    } catch (GameBootRuntimeException expected) {
    } catch (Exception unexpected) {
      unexpected.printStackTrace();
      fail(unexpected.getMessage());
    }
  }
}

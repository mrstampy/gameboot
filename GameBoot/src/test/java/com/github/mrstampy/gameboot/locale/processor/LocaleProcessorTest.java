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
 * Copyright (C) 2015, 2016 Burton Alexander
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
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Locale;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.github.mrstampy.gameboot.TestConfiguration;
import com.github.mrstampy.gameboot.exception.GameBootRuntimeException;
import com.github.mrstampy.gameboot.locale.messages.CurrentLocaleMessage;
import com.github.mrstampy.gameboot.locale.messages.LocaleMessage;
import com.github.mrstampy.gameboot.locale.processor.CurrentLocaleProcessor.LocaleBean;
import com.github.mrstampy.gameboot.messages.Response;
import com.github.mrstampy.gameboot.messages.Response.ResponseCode;
import com.github.mrstampy.gameboot.systemid.SystemIdKey;

/**
 * The Class LocaleProcessorTest.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(TestConfiguration.class)
@ActiveProfiles(LocaleProcessor.PROFILE)
public class LocaleProcessorTest {

  private static final SystemIdKey SYSTEM_ID_KEY = new SystemIdKey(1l);

  private static final String FRANCE_CODE = "FR";

  private static final String FRENCH_CODE = "fr";

  @Autowired
  private LocaleProcessor processor;

  @Autowired
  private CurrentLocaleProcessor current;

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
    msg.setSystemId(SYSTEM_ID_KEY); // set by the system on the way to
                                    // processing

    validationFailExpected(msg, "No lang or country codes");

    msg.setCountryCode(FRANCE_CODE);

    validationFailExpected(msg, "No lang code");

    msg.setLanguageCode(FRENCH_CODE);

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

    testCurrent(Locale.getDefault());

    LocaleMessage msg = new LocaleMessage();
    msg.setSystemId(SYSTEM_ID_KEY);
    msg.setLanguageCode(FRENCH_CODE);

    Response r = processor.process(msg);

    assertNotNull(r);
    assertEquals(ResponseCode.SUCCESS, r.getResponseCode());
    assertEquals(1, registry.size());

    Locale fr = new Locale(FRENCH_CODE);
    testCurrent(fr);

    Locale locale = registry.get(SYSTEM_ID_KEY);
    assertNotNull(locale);
    assertEquals(FRENCH_CODE, locale.getLanguage());
  }

  private void testCurrent(Locale expected) throws Exception {
    CurrentLocaleMessage cur = new CurrentLocaleMessage();
    cur.setSystemId(SYSTEM_ID_KEY);

    Response r = current.process(cur);

    assertEquals(ResponseCode.SUCCESS, r.getResponseCode());
    assertNotNull(r.getPayload());
    assertEquals(1, r.getPayload().length);
    assertTrue(r.getPayload()[0] instanceof LocaleBean);

    LocaleBean lb = (LocaleBean) r.getPayload()[0];
    assertEquals(expected.getCountry(), lb.getCountryCode());
    assertEquals(expected.getLanguage(), lb.getLanguageCode());
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

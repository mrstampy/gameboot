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
package com.github.mrstampy.gameboot.messages.context;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.Locale;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.github.mrstampy.gameboot.TestConfiguration;

/**
 * The Class GameBootContextTest.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(TestConfiguration.class)
public class GameBootContextTest implements ResponseContextCodes {

  private static final int TEST_CODE = -9999;

  private static final String MARY = "Mary had a little lamb, its fleece was white as snow.";
  private static final String DEFAULT = "An unexpected error has occurred.";
  private static final String FRENCH = "Avec! Un skunk de pew!";

  @Autowired
  private GameBootContextLookup lookup;

  /**
   * Test intl.
   *
   * @throws Exception
   *           the exception
   */
  @Test
  public void testIntl() throws Exception {
    ResponseContext rc = lookup.lookup(UNEXPECTED_ERROR, Locale.getDefault());
    assertRC(rc, UNEXPECTED_ERROR, DEFAULT);

    rc = lookup.lookup(UNEXPECTED_ERROR, Locale.FRENCH);
    assertRC(rc, UNEXPECTED_ERROR, FRENCH);

    rc = lookup.lookup(UNEXPECTED_ERROR, Locale.UK);
    assertRC(rc, UNEXPECTED_ERROR, DEFAULT);

    rc = lookup.lookup(UNEXPECTED_ERROR, new Locale("r2", "D2"));
    assertRC(rc, UNEXPECTED_ERROR, DEFAULT);
  }

  /**
   * Test parameters.
   *
   * @throws Exception
   *           the exception
   */
  @Test
  public void testParameters() throws Exception {
    ResponseContext rc = lookup.lookup(TEST_CODE, Locale.FRENCH, "Mary", "lamb", "fleece", "white as snow.");
    assertRC(rc, TEST_CODE, MARY);
  }

  private void assertRC(ResponseContext rc, int expectedCode, String expectedDescription) {
    assertNotNull(rc);
    assertEquals(expectedCode, rc.getCode());
    assertEquals(expectedDescription, rc.getDescription());
  }
}

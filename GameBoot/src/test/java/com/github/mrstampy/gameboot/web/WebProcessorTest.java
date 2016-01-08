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
package com.github.mrstampy.gameboot.web;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.github.mrstampy.gameboot.TestConfiguration;
import com.github.mrstampy.gameboot.messages.GameBootMessageConverter;
import com.github.mrstampy.gameboot.messages.Response;
import com.github.mrstampy.gameboot.messages.Response.ResponseCode;
import com.github.mrstampy.gameboot.usersession.UserSessionConfiguration;
import com.github.mrstampy.gameboot.usersession.data.entity.User;
import com.github.mrstampy.gameboot.usersession.data.repository.UserRepository;
import com.github.mrstampy.gameboot.usersession.messages.UserMessage;
import com.github.mrstampy.gameboot.usersession.messages.UserMessage.Function;

/**
 * The Class WebProcessorTest.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(TestConfiguration.class)
@ActiveProfiles(UserSessionConfiguration.USER_SESSION_PROFILE)
public class WebProcessorTest {

  private static final String PASSWORD = "password";

  private static final String TEST_USER = "testuser";

  @Autowired
  private WebProcessor processor;

  @Autowired
  private GameBootMessageConverter converter;

  @Autowired
  private UserRepository userRepo;

  private MockHttpSession httpSession = new MockHttpSession(null, "ID");

  private Long userId;

  /**
   * After.
   *
   * @throws Exception
   *           the exception
   */
  @After
  public void after() throws Exception {
    userRepo.delete(userId);
  }

  /**
   * Test create user.
   *
   * @throws Exception
   *           the exception
   */
  @Test
  public void testCreateUser() throws Exception {
    UserMessage m = createUserMessage();

    processor.onConnection(httpSession);

    Response r = processor.process(httpSession, converter.toJson(m));

    assertResponse(m, r);
  }

  /**
   * Test create user binary.
   *
   * @throws Exception
   *           the exception
   */
  @Test
  public void testCreateUserBinary() throws Exception {
    UserMessage m = createUserMessage();

    processor.onConnection(httpSession);

    Response r = processor.process(httpSession, converter.toJsonArray(m));

    assertResponse(m, r);
  }

  private UserMessage createUserMessage() {
    UserMessage m = new UserMessage();

    m.setId(1);
    m.setFunction(Function.CREATE);
    m.setUserName(TEST_USER);
    m.setNewPassword(PASSWORD);

    return m;
  }

  private void assertResponse(UserMessage m, Response r) {
    assertEquals(m.getId(), r.getId());
    assertEquals(ResponseCode.SUCCESS, r.getResponseCode());
    assertNotNull(r.getPayload());
    assertEquals(1, r.getPayload().length);
    assertTrue(r.getPayload()[0] instanceof User);

    User user = (User) r.getPayload()[0];
    userId = user.getId();
  }
}

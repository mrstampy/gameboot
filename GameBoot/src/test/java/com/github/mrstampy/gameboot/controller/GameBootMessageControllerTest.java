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
package com.github.mrstampy.gameboot.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.util.Map;

import javax.transaction.Transactional;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.mrstampy.gameboot.TestConfiguration;
import com.github.mrstampy.gameboot.exception.GameBootException;
import com.github.mrstampy.gameboot.exception.GameBootRuntimeException;
import com.github.mrstampy.gameboot.messages.Response;
import com.github.mrstampy.gameboot.messages.Response.ResponseCode;
import com.github.mrstampy.gameboot.usersession.UserSessionConfiguration;
import com.github.mrstampy.gameboot.usersession.data.repository.UserRepository;
import com.github.mrstampy.gameboot.usersession.messages.UserMessage;
import com.github.mrstampy.gameboot.usersession.messages.UserMessage.Function;

/**
 * The Class GameBootMessageControllerTest.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(TestConfiguration.class)
@ActiveProfiles(UserSessionConfiguration.USER_SESSION_PROFILE)
public class GameBootMessageControllerTest {

  @Autowired
  private GameBootMessageController controller;

  @Autowired
  private ObjectMapper mapper;

  @Autowired
  private UserRepository userRepo;

  private Long userId;

  /**
   * After.
   *
   * @throws Exception
   *           the exception
   */
  @After
  @Transactional
  public void after() throws Exception {
    if (userId != null) userRepo.delete(userId);

    userId = null;
  }

  /**
   * Test create user.
   *
   * @throws Exception
   *           the exception
   */
  @Test
  public void testCreateUser() throws Exception {
    UserMessage msg = new UserMessage();
    msg.setUserName("test user");
    msg.setFunction(Function.CREATE);
    msg.setNewPassword("password");

    String s = controller.process(mapper.writeValueAsString(msg));
    assertNotNull(s);

    Response r = mapper.readValue(s, Response.class);
    Object[] response = r.getResponse();

    assertEquals(ResponseCode.SUCCESS, r.getResponseCode());
    assertNotNull(response);
    assertEquals(1, response.length);

    Map<?, ?> blah = (Map<?, ?>) response[0];

    Object id = blah.get("id");

    assertNotNull(id);

    this.userId = Long.parseLong(id.toString());
  }

  /**
   * Test error conditions.
   *
   * @throws Exception
   *           the exception
   */
  @Test
  public void testErrorConditions() throws Exception {
    process(null, "null message");
    process("", "empty message");
    process("hello", "not a message");
    process(mapper.writeValueAsString(new Response()), "no processor");
  }

  private void process(String message, String failMsg) {
    gameBootExpected(() -> {
      try {
        controller.process(message);
        fail(failMsg);
      } catch (GameBootException | GameBootRuntimeException expected) {
      } catch (JsonParseException expected) {
      } catch (Exception unexpected) {
        unexpected.printStackTrace();
        fail(unexpected.getMessage());
      }
    });
  }

  private void gameBootExpected(Runnable r) {
    r.run();
  }
}

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
package com.github.mrstampy.gameboot.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import javax.transaction.Transactional;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.mrstampy.gameboot.TestConfiguration;
import com.github.mrstampy.gameboot.data.entity.User;
import com.github.mrstampy.gameboot.data.repository.UserRepository;
import com.github.mrstampy.gameboot.exception.GameBootRuntimeException;
import com.github.mrstampy.gameboot.messages.Response;
import com.github.mrstampy.gameboot.messages.Response.ResponseCode;
import com.github.mrstampy.gameboot.messages.UserMessage;
import com.github.mrstampy.gameboot.messages.UserMessage.Function;

/**
 * The Class GameBootMessageControllerTest.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(TestConfiguration.class)
public class GameBootMessageControllerTest {

  @Autowired
  private GameBootMessageController controller;

  @Autowired
  private ObjectMapper mapper;

  @Autowired
  private UserRepository userRepo;

  private User user;

  /**
   * After.
   *
   * @throws Exception
   *           the exception
   */
  @After
  @Transactional
  public void after() throws Exception {
    if (user != null) userRepo.delete(user);

    user = null;
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

    Response r = controller.process(mapper.writeValueAsString(msg));
    Object[] response = r.getResponse();

    assertEquals(ResponseCode.SUCCESS, r.getResponseCode());
    assertNotNull(response);
    assertEquals(1, response.length);
    assertTrue(response[0] instanceof User);

    this.user = (User) response[0];
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
      } catch (GameBootRuntimeException expected) {
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

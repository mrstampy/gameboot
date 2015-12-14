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
package com.github.mrstampy.gameboot.usersession.processor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.lang.invoke.MethodHandles;
import java.util.Date;
import java.util.Map.Entry;
import java.util.Set;

import javax.transaction.Transactional;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Timer;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.mrstampy.gameboot.TestConfiguration;
import com.github.mrstampy.gameboot.messages.Response;
import com.github.mrstampy.gameboot.messages.Response.ResponseCode;
import com.github.mrstampy.gameboot.metrics.MetricsHelper;
import com.github.mrstampy.gameboot.usersession.ActiveSessions;
import com.github.mrstampy.gameboot.usersession.UserSessionAssist;
import com.github.mrstampy.gameboot.usersession.UserSessionConfiguration;
import com.github.mrstampy.gameboot.usersession.data.entity.User;
import com.github.mrstampy.gameboot.usersession.data.entity.User.UserState;
import com.github.mrstampy.gameboot.usersession.data.entity.UserSession;
import com.github.mrstampy.gameboot.usersession.data.repository.UserRepository;
import com.github.mrstampy.gameboot.usersession.data.repository.UserSessionRepository;
import com.github.mrstampy.gameboot.usersession.messages.UserMessage;
import com.github.mrstampy.gameboot.usersession.messages.UserMessage.Function;

/**
 * The Class UserMessageProcessorTest.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(TestConfiguration.class)
@ActiveProfiles(UserSessionConfiguration.USER_SESSION_PROFILE)
public class UserMessageProcessorTest {
  private static final UserState NEW_STATE = UserState.INACTIVE;

  private static final String NEW_LAST = "last";

  private static final String NEW_FIRST = "first";

  private static final Date NEW_DOB = new Date();

  private static final String NEW_EMAIL = "bling.blah@yada.com";

  private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private static final String PASSWORD = "password";

  private static final String BAD_PASSWORD = "BADpassword";

  private static final String TEST_USER = "testuser";

  private static final String BAD_USER = "baduser";

  @Autowired
  private UserMessageProcessor processor;

  @Autowired
  private UserSessionAssist assist;

  @Autowired
  private ActiveSessions activeSessions;

  @Autowired
  private UserRepository userRepo;

  @Autowired
  private UserSessionRepository userSessionRepo;

  @Autowired
  private MetricsHelper helper;

  @Autowired
  private ObjectMapper mapper;

  private Long userId;

  private Long sessionId;

  /**
   * Before.
   *
   * @throws Exception
   *           the exception
   */
  @Before
  public void before() throws Exception {
    failExpected(null, "Null message");

    UserMessage m = new UserMessage();
    m.setId(1);

    failExpected(m, "Empty message");

    m.setFunction(Function.CREATE);
    failExpected(m, "No username/password");

    m.setUserName(TEST_USER);
    failExpected(m, "No password");

    m.setNewPassword(PASSWORD);

    Response r = processor.process(m);

    assertEquals(m.getId(), r.getId());
    assertEquals(ResponseCode.SUCCESS, r.getResponseCode());
    assertNotNull(r.getResponse());
    assertEquals(1, r.getResponse().length);
    assertTrue(r.getResponse()[0] instanceof User);

    User user = (User) r.getResponse()[0];
    userId = user.getId();
  }

  /**
   * After.
   *
   * @throws Exception
   *           the exception
   */
  @After
  public void after() throws Exception {
    if (sessionId != null) {
      if (assist.hasSession(sessionId)) assist.logout(sessionId);

      userSessionRepo.delete(sessionId);
    }

    userRepo.delete(userId);

    metrics();
  }

  /**
   * Test login.
   *
   * @throws Exception
   *           the exception
   */
  @Test
  @Transactional
  public void testLogin() throws Exception {
    UserMessage m = new UserMessage();
    m.setId(1);

    m.setFunction(Function.LOGIN);
    m.setUserName(TEST_USER);
    m.setOldPassword(BAD_PASSWORD);

    failExpected(m, "Bad password");

    m.setUserName(BAD_USER);
    m.setOldPassword(PASSWORD);

    failExpected(m, "Bad user");

    m.setUserName(TEST_USER);

    Response r = processor.process(m);

    assertEquals(m.getId(), r.getId());
    assertEquals(ResponseCode.SUCCESS, r.getResponseCode());

    assertNotNull(r.getResponse());
    assertEquals(1, r.getResponse().length);

    assertTrue(r.getResponse()[0] instanceof UserSession);

    UserSession session = (UserSession) r.getResponse()[0];
    sessionId = session.getId();

    assertEquals(2, r.getMappingKeys().length);
    assertEquals(TEST_USER, r.getMappingKeys()[0]);
    assertEquals(sessionId, r.getMappingKeys()[1]);

    failExpected(m, "User logged in");
  }

  /**
   * Test logout.
   *
   * @throws Exception
   *           the exception
   */
  @Test
  @Transactional
  public void testLogout() throws Exception {
    UserMessage m = new UserMessage();
    m.setId(1);

    m.setFunction(Function.LOGOUT);
    m.setUserName(TEST_USER);

    failExpected(m, "Not logged in");

    testLogin();

    Response r = processor.process(m);

    assertEquals(m.getId(), r.getId());
    assertEquals(ResponseCode.SUCCESS, r.getResponseCode());

    assertNotNull(r.getResponse());
    assertEquals(1, r.getResponse().length);

    assertTrue(r.getResponse()[0] instanceof User);
  }

  /**
   * Test delete.
   *
   * @throws Exception
   *           the exception
   */
  @Test
  @Transactional
  public void testDelete() throws Exception {
    UserMessage m = new UserMessage();
    m.setId(1);

    m.setFunction(Function.DELETE);
    m.setUserName(TEST_USER);

    Response r = processor.process(m);

    assertEquals(m.getId(), r.getId());
    assertEquals(ResponseCode.SUCCESS, r.getResponseCode());

    assertNotNull(r.getResponse());
    assertEquals(1, r.getResponse().length);

    assertTrue(r.getResponse()[0] instanceof User);

    User user = (User) r.getResponse()[0];
    assertEquals(UserState.DELETED, user.getState());
  }

  /**
   * Test delete logged in.
   *
   * @throws Exception
   *           the exception
   */
  @Test
  @Transactional
  public void testDeleteLoggedIn() throws Exception {
    testLogin();
    assertEquals(1, activeSessions.size());

    testDelete();
    assertEquals(0, activeSessions.size());
  }

  /**
   * Test update.
   *
   * @throws Exception
   *           the exception
   */
  @Test
  @Transactional
  public void testUpdate() throws Exception {
    User user = userRepo.findOne(userId);

    UserMessage m = new UserMessage();
    m.setId(1);

    m.setFunction(Function.UPDATE);
    m.setUserName(TEST_USER);

    failExpected(m, "No user data changed");

    user = emailTest(user, m);

    user = dobTest(user, m);
    assertEquals(NEW_EMAIL, user.getEmail());
    assertEquals(NEW_DOB, user.getDob());
    assertNotEquals(NEW_FIRST, user.getFirstName());
    assertNotEquals(NEW_LAST, user.getLastName());
    assertNotEquals(NEW_STATE, user.getState());

    user = firstNameTest(user, m);
    assertEquals(NEW_EMAIL, user.getEmail());
    assertEquals(NEW_DOB, user.getDob());
    assertEquals(NEW_FIRST, user.getFirstName());
    assertNotEquals(NEW_LAST, user.getLastName());
    assertNotEquals(NEW_STATE, user.getState());

    user = lastNameTest(user, m);
    assertEquals(NEW_EMAIL, user.getEmail());
    assertEquals(NEW_DOB, user.getDob());
    assertEquals(NEW_FIRST, user.getFirstName());
    assertEquals(NEW_LAST, user.getLastName());
    assertNotEquals(NEW_STATE, user.getState());

    user = userStateTest(user, m);
    assertEquals(NEW_EMAIL, user.getEmail());
    assertEquals(NEW_DOB, user.getDob());
    assertEquals(NEW_FIRST, user.getFirstName());
    assertEquals(NEW_LAST, user.getLastName());
    assertEquals(NEW_STATE, user.getState());

    passwordTest(user, m);
  }

  private void passwordTest(User user, UserMessage m) throws Exception {
    m.setOldPassword(BAD_PASSWORD);
    m.setNewPassword(PASSWORD);

    failExpected(m, "Wrong password for password change");

    m.setOldPassword(PASSWORD);
    m.setNewPassword(BAD_PASSWORD);

    String oldHash = user.getPasswordHash();

    user = updateCheck(processor.process(m));
    assertNotEquals(oldHash, user.getPasswordHash());
  }

  private User userStateTest(User user, UserMessage m) throws Exception {
    UserState state = NEW_STATE;
    assertNotEquals(state, user.getState());
    m.setState(state);

    user = updateCheck(processor.process(m));
    assertEquals(state, user.getState());
    m.setState(null);
    return user;
  }

  private User lastNameTest(User user, UserMessage m) throws Exception {
    String lastName = NEW_LAST;
    assertNotEquals(lastName, user.getLastName());
    m.setLastName(lastName);

    user = updateCheck(processor.process(m));
    assertEquals(lastName, user.getLastName());
    m.setLastName(null);
    return user;
  }

  private User firstNameTest(User user, UserMessage m) throws Exception {
    String firstName = NEW_FIRST;
    assertNotEquals(firstName, user.getFirstName());
    m.setFirstName(firstName);

    user = updateCheck(processor.process(m));
    assertEquals(firstName, user.getFirstName());
    m.setFirstName(null);
    return user;
  }

  private User dobTest(User user, UserMessage m) throws Exception {
    Date dob = NEW_DOB;
    assertNotEquals(dob, user.getDob());
    m.setDob(dob);

    user = updateCheck(processor.process(m));
    assertEquals(dob, user.getDob());
    m.setDob(null);
    return user;
  }

  private User emailTest(User user, UserMessage m) throws Exception {
    String email = NEW_EMAIL;
    assertNotEquals(email, user.getEmail());
    m.setEmail(email);

    user = updateCheck(processor.process(m));
    assertEquals(email, user.getEmail());
    m.setEmail(null);
    return user;
  }

  private User updateCheck(Response r) {
    assertEquals(ResponseCode.SUCCESS, r.getResponseCode());

    assertNotNull(r.getResponse());
    assertEquals(1, r.getResponse().length);

    assertTrue(r.getResponse()[0] instanceof User);

    return (User) r.getResponse()[0];
  }

  private void metrics() throws Exception {
    Set<Entry<String, Timer>> timers = helper.getTimers();

    timers.forEach(e -> display(e));

    Set<Entry<String, Counter>> counters = helper.getCounters();

    counters.forEach(e -> display(e));
  }

  private void display(Entry<String, ?> t) {
    try {
      log.debug(mapper.writeValueAsString(t));
    } catch (JsonProcessingException e) {
      log.error("Unexpected exception", e);
    }
  }

  private void failExpected(UserMessage m, String failMsg) {
    try {
      Response r = processor.process(m);
      switch (r.getResponseCode()) {
      case FAILURE:
        assertEquals(m.getId(), r.getId());
        break;
      default:
        fail(failMsg);
        break;
      }
    } catch (RuntimeException expected) {
    } catch (Exception e) {
      fail(e.getMessage());
    }
  }
}

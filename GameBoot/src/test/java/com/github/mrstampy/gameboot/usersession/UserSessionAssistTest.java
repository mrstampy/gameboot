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
package com.github.mrstampy.gameboot.usersession;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.List;

import javax.transaction.Transactional;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.github.mrstampy.gameboot.TestConfiguration;
import com.github.mrstampy.gameboot.exception.GameBootRuntimeException;
import com.github.mrstampy.gameboot.usersession.data.entity.User;
import com.github.mrstampy.gameboot.usersession.data.entity.User.UserState;
import com.github.mrstampy.gameboot.usersession.data.entity.UserSession;
import com.github.mrstampy.gameboot.usersession.data.repository.UserRepository;
import com.github.mrstampy.gameboot.usersession.data.repository.UserSessionRepository;

/**
 * The Class UserSessionAssistTest.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(TestConfiguration.class)
@ActiveProfiles(UserSessionConfiguration.USER_SESSION_PROFILE)
public class UserSessionAssistTest {

  private static final String SESSIONS_CACHE_NAME = UserSessionAssist.SESSIONS_CACHE;

  private static final String NON_EXISTENT = "usertest";

  private static final String USER_NAME = "testuser";

  @Autowired
  private UserSessionAssist assist;

  @Autowired
  private UserRepository userRepo;

  @Autowired
  private UserSessionRepository userSessionRepo;

  @Autowired
  private CacheManager cacheManager;

  private Long userId;

  private Long sessionId;

  private Cache cache;

  /**
   * Before.
   *
   * @throws Exception
   *           the exception
   */
  @Before
  public void before() throws Exception {
    User user = createUser();

    user = userRepo.save(user);

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

    sessionId = null;

    userRepo.delete(userId);
  }

  /**
   * Test expected user.
   *
   * @throws Exception
   *           the exception
   */
  @Test
  @Transactional
  public void testExpectedUser() throws Exception {
    gameBootExpected(() -> assist.expectedUser(null), "Null user name");
    gameBootExpected(() -> assist.expectedUser(" "), "Blank user name");
    gameBootExpected(() -> assist.expectedUser(NON_EXISTENT), "non existent user name");

    User user = assist.expectedUser(USER_NAME);

    assertEquals(userId, user.getId());
  }

  /**
   * Test expected session.
   *
   * @throws Exception
   *           the exception
   */
  @Test
  @Transactional
  public void testExpectedSession() throws Exception {
    gameBootExpected(() -> assist.create(null), "Null user");
    gameBootExpected(() -> assist.expected((String) null), "Null user name");
    gameBootExpected(() -> assist.expected(" "), "Blank user name");
    gameBootExpected(() -> assist.expected(0l), "Zero id");
    gameBootExpected(() -> assist.expected(-1l), "Negative id");

    assertFalse(assist.hasSession(Long.MAX_VALUE));
    assertFalse(assist.hasSession(NON_EXISTENT));

    User user = assist.expectedUser(USER_NAME);

    createSession(user);

    assertTrue(assist.hasSession(sessionId));
    assertTrue(assist.hasSession(USER_NAME));

    gameBootExpected(() -> assist.create(user), "Session exists");

    UserSession same = assist.expected(sessionId);
    assertEquals(sessionId, same.getId());

    same = assist.expected(USER_NAME);
    assertEquals(sessionId, same.getId());

    same = assist.expected(user);
    assertEquals(sessionId, same.getId());
  }

  /**
   * Test logout user name.
   *
   * @throws Exception
   *           the exception
   */
  @Test
  @Transactional
  public void testLogoutUserName() throws Exception {
    testLogout(() -> assist.logout(USER_NAME));
  }

  /**
   * Test logout id.
   *
   * @throws Exception
   *           the exception
   */
  @Test
  @Transactional
  public void testLogoutId() throws Exception {
    testLogout(() -> assist.logout(sessionId));
  }

  /**
   * Test active session caching.
   *
   * @throws Exception
   *           the exception
   */
  @Test
  @Transactional
  public void testActiveSessionCaching() throws Exception {
    cache = cacheManager.getCache(SESSIONS_CACHE_NAME);

    cache.clear();

    User user = assist.expectedUser(USER_NAME);

    UserSession session = createSession(user);

    List<UserSession> sessions = assist.activeSessions();

    assertEquals(1, sessions.size());
    assertEquals(session.getId(), sessions.get(0).getId());
    assertNotNull(getCached());

    // enable to test cache expiry, assumes 5 seconds
    // @see src/main/resources/ehcache.xml
    //
    // Thread.sleep(6000);
    //
    // assertNull(getCached());
  }

  @SuppressWarnings("unchecked")
  private List<UserSession> getCached() {
    return cache.get(UserSessionAssist.SESSIONS_KEY, List.class);
  }

  private void testLogout(Runnable r) throws Exception {
    User user = assist.expectedUser(USER_NAME);

    UserSession session = createSession(user);

    assertNull(session.getEnded());

    assertTrue(assist.hasSession(sessionId));

    r.run();

    assertFalse(assist.hasSession(sessionId));

    assertNotNull(session.getEnded());
  }

  private UserSession createSession(User user) {
    UserSession session = assist.create(user);
    sessionId = session.getId();
    return session;
  }

  private void gameBootExpected(Runnable r, String failMsg) {
    try {
      r.run();
      fail(failMsg);
    } catch (GameBootRuntimeException expected) {
    }
  }

  private User createUser() {
    User user = new User();

    user.setUserName(USER_NAME);
    user.setState(UserState.ACTIVE);
    user.setPasswordHash("unimportant");

    return user;
  }
}

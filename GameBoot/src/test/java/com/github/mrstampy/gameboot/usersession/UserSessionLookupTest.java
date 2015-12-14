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
package com.github.mrstampy.gameboot.usersession;

import static org.junit.Assert.fail;

import java.lang.invoke.MethodHandles;
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
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.codahale.metrics.Timer;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.mrstampy.gameboot.TestConfiguration;
import com.github.mrstampy.gameboot.exception.GameBootRuntimeException;
import com.github.mrstampy.gameboot.metrics.MetricsHelper;
import com.github.mrstampy.gameboot.usersession.data.entity.User;
import com.github.mrstampy.gameboot.usersession.data.entity.User.UserState;
import com.github.mrstampy.gameboot.usersession.data.entity.UserSession;
import com.github.mrstampy.gameboot.usersession.data.repository.UserRepository;
import com.github.mrstampy.gameboot.usersession.data.repository.UserSessionRepository;

/**
 * The Class UserSessionLookupTest.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(TestConfiguration.class)
@ActiveProfiles(UserSessionConfiguration.USER_SESSION_PROFILE)
public class UserSessionLookupTest {
  private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private static final String NON_EXISTENT = "usertest";

  private static final String USER_NAME = "testuser";

  private static final int METRICS_ITR = 100;

  @Autowired
  private UserSessionLookup lookup;

  @Autowired
  private UserSessionAssist assist;

  @Autowired
  private UserRepository userRepo;

  @Autowired
  private UserSessionRepository userSessionRepo;

  @Autowired
  private MetricsHelper helper;

  @Autowired
  private ObjectMapper mapper;

  private User user;

  private Long sessionId;

  @Autowired
  private CacheManager cacheManager;

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

    this.user = userRepo.save(user);

    createSession(user);

    cache = cacheManager.getCache(UserSessionAssist.SESSIONS_CACHE);
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

    userRepo.delete(user);
  }

  /**
   * Test exceptions.
   *
   * @throws Exception
   *           the exception
   */
  @Test
  @Transactional
  public void testExceptions() throws Exception {
    gameBootExcpected(() -> lookup.expected(NON_EXISTENT), "No session for username");
    gameBootExcpected(() -> lookup.expected((String) null), "Null username");
    gameBootExcpected(() -> lookup.expected((Long) null), "Null id");
    gameBootExcpected(() -> lookup.expected(Long.MAX_VALUE), "No session for id");
  }

  /**
   * Metrics with session id.
   *
   * @throws Exception
   *           the exception
   */
  @Test
  @Transactional
  public void metricsWithSessionId() throws Exception {
    generateUncachedStats();

    for (int i = 0; i < METRICS_ITR; i++) {
      lookup.expected(sessionId);
    }

    metrics();
  }

  /**
   * Metrics with user name.
   *
   * @throws Exception
   *           the exception
   */
  @Test
  @Transactional
  public void metricsWithUserName() throws Exception {
    generateUncachedStats();

    for (int i = 0; i < METRICS_ITR; i++) {
      lookup.expected(USER_NAME);
    }

    metrics();
  }

  /**
   * Generate uncached stats.
   */
  protected void generateUncachedStats() {
    for (int i = 0; i < METRICS_ITR; i++) {
      cache.clear();
      assist.activeSessions();
    }
  }

  private void gameBootExcpected(Runnable r, String failMsg) {
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

  private UserSession createSession(User user) {
    UserSession session = assist.create(user);
    sessionId = session.getId();
    return session;
  }

  private void metrics() throws Exception {
    Set<Entry<String, Timer>> timers = helper.getTimers();

    timers.stream().filter(e -> isMetric(e.getKey())).forEach(e -> display(e));
  }

  private boolean isMetric(String key) {
    return "UncachedSessionTimer".equals(key) || "CachedSessionTimer".equals(key);
  }

  private void display(Entry<String, ?> t) {
    try {
      log.debug(mapper.writeValueAsString(t));
    } catch (JsonProcessingException e) {
      log.error("Unexpected exception", e);
    }
  }
}

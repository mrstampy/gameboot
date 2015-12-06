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
package com.github.mrstampy.gameboot.processor.impl;

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
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Timer;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.mrstampy.gameboot.GameBoot;
import com.github.mrstampy.gameboot.data.assist.ActiveSessions;
import com.github.mrstampy.gameboot.data.assist.UserSessionAssist;
import com.github.mrstampy.gameboot.data.entity.User;
import com.github.mrstampy.gameboot.data.entity.User.UserState;
import com.github.mrstampy.gameboot.data.entity.UserSession;
import com.github.mrstampy.gameboot.data.entity.repository.UserRepository;
import com.github.mrstampy.gameboot.data.entity.repository.UserSessionRepository;
import com.github.mrstampy.gameboot.messages.Response;
import com.github.mrstampy.gameboot.messages.Response.ResponseCode;
import com.github.mrstampy.gameboot.messages.UserMessage;
import com.github.mrstampy.gameboot.messages.UserMessage.Function;
import com.github.mrstampy.gameboot.metrics.MetricsHelper;

// TODO: Auto-generated Javadoc
/**
 * The Class UserMessageProcessorTest.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(GameBoot.class)
public class UserMessageProcessorTest {
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

		failExpected(m, "Empty message");

		m.setFunction(Function.CREATE);
		failExpected(m, "No username/password");

		m.setUserName(TEST_USER);
		failExpected(m, "No password");

		m.setNewPassword(PASSWORD);

		Response r = processor.process(m);

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

		m.setFunction(Function.LOGIN);
		m.setUserName(TEST_USER);
		m.setNewPassword(BAD_PASSWORD);

		failExpected(m, "Bad password");

		m.setUserName(BAD_USER);
		m.setNewPassword(PASSWORD);

		failExpected(m, "Bad user");

		m.setUserName(TEST_USER);

		Response r = processor.process(m);

		assertEquals(ResponseCode.SUCCESS, r.getResponseCode());

		assertNotNull(r.getResponse());
		assertEquals(2, r.getResponse().length);

		assertTrue(r.getResponse()[0] instanceof User);
		assertTrue(r.getResponse()[1] instanceof UserSession);

		UserSession session = (UserSession) r.getResponse()[1];
		sessionId = session.getId();

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

		m.setFunction(Function.LOGOUT);
		m.setUserName(TEST_USER);

		failExpected(m, "Not logged in");

		testLogin();

		Response r = processor.process(m);

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

		m.setFunction(Function.DELETE);
		m.setUserName(TEST_USER);

		Response r = processor.process(m);

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

		m.setFunction(Function.UPDATE);
		m.setUserName(TEST_USER);

		failExpected(m, "No user data changed");

		user = emailTest(user, m);

		user = dobTest(user, m);

		user = firstNameTest(user, m);

		user = lastNameTest(user, m);

		user = userStateTest(user, m);

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
		UserState state = UserState.INACTIVE;
		assertNotEquals(state, user.getState());
		m.setState(state);

		user = updateCheck(processor.process(m));
		assertEquals(state, user.getState());
		m.setState(null);
		return user;
	}

	private User lastNameTest(User user, UserMessage m) throws Exception {
		String lastName = "last";
		assertNotEquals(lastName, user.getLastName());
		m.setLastName(lastName);

		user = updateCheck(processor.process(m));
		assertEquals(lastName, user.getLastName());
		m.setLastName(null);
		return user;
	}

	private User firstNameTest(User user, UserMessage m) throws Exception {
		String firstName = "first";
		assertNotEquals(firstName, user.getFirstName());
		m.setFirstName(firstName);

		user = updateCheck(processor.process(m));
		assertEquals(firstName, user.getFirstName());
		m.setFirstName(null);
		return user;
	}

	private User dobTest(User user, UserMessage m) throws Exception {
		Date dob = new Date();
		assertNotEquals(dob, user.getDob());
		m.setDob(dob);

		user = updateCheck(processor.process(m));
		assertEquals(dob, user.getDob());
		m.setDob(null);
		return user;
	}

	private User emailTest(User user, UserMessage m) throws Exception {
		String email = "bling.blah@yada.com";
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

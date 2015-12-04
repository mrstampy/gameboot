package com.github.mrstampy.gameboot.data.assist;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import javax.transaction.Transactional;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.github.mrstampy.gameboot.GameBoot;
import com.github.mrstampy.gameboot.data.entity.User;
import com.github.mrstampy.gameboot.data.entity.User.UserState;
import com.github.mrstampy.gameboot.data.entity.UserSession;
import com.github.mrstampy.gameboot.data.entity.repository.UserRepository;
import com.github.mrstampy.gameboot.data.entity.repository.UserSessionRepository;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(GameBoot.class)
public class UserSessionAssistTest {

	private static final String NON_EXISTENT = "usertest";

	public static final String USER_NAME = "testuser";

	@Autowired
	private UserSessionAssist assist;

	@Autowired
	private UserRepository userRepo;
	
	@Autowired
	private UserSessionRepository userSessionRepo;

	private Long userId;
	
	private Long sessionId;

	@Before
	public void before() throws Exception {
		User user = createUser();

		user = userRepo.save(user);

		userId = user.getId();
	}

	@After
	public void after() throws Exception {
		if(sessionId != null) userSessionRepo.delete(sessionId);
		
		sessionId = null;
		
		userRepo.delete(userId);
	}

	@Test
	public void testExpectedUser() throws Exception {
		illegalStateExpected(() -> assist.expectedUser(null), "Null user name");
		illegalStateExpected(() -> assist.expectedUser(" "), "Blank user name");
		illegalStateExpected(() -> assist.expectedUser(NON_EXISTENT), "non existent user name");

		User user = assist.expectedUser(USER_NAME);

		assertEquals(userId, user.getId());
	}
	
	@Test
	public void testExpectedSession() throws Exception {
		illegalStateExpected(() -> assist.create(null), "Null user");
		illegalStateExpected(() -> assist.expected((String)null), "Null user name");
		illegalStateExpected(() -> assist.expected(" "), "Blank user name");
		illegalStateExpected(() -> assist.expected(0), "Zero id");
		illegalStateExpected(() -> assist.expected(-1), "Negative id");
		
		assertFalse(assist.hasSession(Long.MAX_VALUE));
		assertFalse(assist.hasSession(NON_EXISTENT));
		
		User user = assist.expectedUser(USER_NAME);
		
		UserSession session = assist.create(user);
		
		sessionId = session.getId();
		
		assertTrue(assist.hasSession(sessionId));
		assertTrue(assist.hasSession(USER_NAME));
		
		illegalStateExpected(() -> assist.create(user), "Session exists");
		
		UserSession same = assist.expected(sessionId);
		assertEquals(sessionId, same.getId());
		
		same = assist.expected(USER_NAME);
		assertEquals(sessionId, same.getId());
		
		same = assist.expected(user);
		assertEquals(sessionId, same.getId());
	}
	
	@Test
	@Transactional
	public void testLogoutUserName() throws Exception {
		testLogout(() -> assist.logout(USER_NAME));
	}
	
	@Test
	@Transactional
	public void testLogoutId() throws Exception {
		testLogout(() -> assist.logout(sessionId));
	}
	
	private void testLogout(Runnable r) throws Exception {
		User user = assist.expectedUser(USER_NAME);
		
		UserSession session = assist.create(user);
		
		sessionId = session.getId();
		
		assertNull(session.getEnded());
		
		assertTrue(assist.hasSession(sessionId));
		
		r.run();
		
		assertFalse(assist.hasSession(sessionId));
		
		UserSession same = userSessionRepo.findOne(sessionId);
		
		assertNotNull(same.getEnded());
	}

	private void illegalStateExpected(Runnable r, String failMsg) {
		try {
			r.run();
			fail(failMsg);
		} catch (IllegalStateException expected) {
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

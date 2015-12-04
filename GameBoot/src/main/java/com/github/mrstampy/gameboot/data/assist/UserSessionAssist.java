package com.github.mrstampy.gameboot.data.assist;

import static org.apache.commons.lang3.StringUtils.isEmpty;

import java.lang.invoke.MethodHandles;
import java.util.Date;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import com.github.mrstampy.gameboot.data.entity.User;
import com.github.mrstampy.gameboot.data.entity.UserSession;
import com.github.mrstampy.gameboot.data.entity.repository.UserRepository;
import com.github.mrstampy.gameboot.data.entity.repository.UserSessionRepository;

@Component
public class UserSessionAssist {
	private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

	@Autowired
	private UserSessionRepository userSessionRepo;

	@Autowired
	private UserRepository userRepo;

	@Autowired
	private ActiveSessions activeSessions;

	public UserSession create(User user) throws IllegalStateException {
		userCheck(user);

		String userName = user.getUserName();
		check(activeSessions.hasSession(userName), "Session already exists for " + userName);

		UserSession session = new UserSession();
		session.setUser(user);

		userSessionRepo.save(session);

		activeSessions.addSession(session);

		return session;
	}

	public User expectedUser(String userName) throws IllegalStateException {
		userNameCheck(userName);

		User user = userRepo.findByUserName(userName);

		check(user == null, "No user for " + userName);

		return user;
	}

	public UserSession expected(String userName) throws IllegalStateException {
		userNameCheck(userName);

		return expected(expectedUser(userName));
	}

	public UserSession expected(User user) throws IllegalStateException {
		userCheck(user);

		String userName = user.getUserName();
		check(!activeSessions.hasSession(userName), "No session for " + userName);

		return userSessionRepo.findByUserAndEndedIsNull(user);
	}

	public UserSession expected(long id) throws IllegalStateException {
		if (id <= 0) throw new IllegalStateException("Id must be > 0: " + id);

		check(!activeSessions.hasSession(id), "No session for id " + id);

		return userSessionRepo.findByIdAndEndedIsNull(id);
	}

	public boolean hasSession(String userName) {
		return activeSessions.hasSession(userName);
	}

	public boolean hasSession(long id) {
		return activeSessions.hasSession(id);
	}

	public User logout(String userName) throws IllegalStateException {
		UserSession session = expected(userName);

		closeSession(session);

		return session.getUser();
	}

	public User logout(Long id) throws IllegalStateException {
		UserSession session = expected(id);

		closeSession(session);

		return session.getUser();
	}

	@Cacheable(value = "sessions")
	public List<UserSession> activeSessions() {
		return userSessionRepo.findByEndedIsNull();
	}

	protected void closeSession(UserSession session) {
		session.setEnded(new Date());

		userSessionRepo.save(session);

		activeSessions.removeSession(session);

		log.info("User {} logged out", session.getUser().getUserName());
	}

	protected void userCheck(User user) {
		check(user == null, "null user");
	}

	protected void userNameCheck(String userName) throws IllegalStateException {
		check(isEmpty(userName), "null username");
	}

	protected void check(boolean condition, String msg) {
		if (condition) throw new IllegalStateException(msg);
	}
}

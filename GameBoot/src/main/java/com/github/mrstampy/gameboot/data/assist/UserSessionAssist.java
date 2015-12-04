package com.github.mrstampy.gameboot.data.assist;

import java.lang.invoke.MethodHandles;
import java.util.Date;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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

	public UserSession create(User user) throws IllegalStateException {
		userCheck(user);

		UserSession session = userSessionRepo.findByUserAndEndedIsNull(user);

		check(session != null, "Session already exists for " + user.getUserName());

		session = new UserSession();
		session.setUser(user);

		userSessionRepo.save(session);

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

		UserSession session = userSessionRepo.findByUserAndEndedIsNull(user);

		check(session == null, "No session for " + user.getUserName());

		return session;
	}

	public UserSession expected(long id) throws IllegalStateException {
		if (id <= 0) throw new IllegalStateException("Id must be > 0: " + id);

		UserSession session = userSessionRepo.findByIdAndEndedIsNull(id);

		check(session == null, "No session for id " + id);

		return session;
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

	protected void closeSession(UserSession session) {
		session.setEnded(new Date());

		log.info("User {} logged out", session.getUser().getUserName());

		userSessionRepo.save(session);
	}

	protected void userCheck(User user) {
		check(user == null, "null user");
	}

	protected void userNameCheck(String userName) throws IllegalStateException {
		check(StringUtils.isEmpty(userName), "null username");
	}

	protected void check(boolean condition, String msg) {
		if (condition) throw new IllegalStateException(msg);
	}
}

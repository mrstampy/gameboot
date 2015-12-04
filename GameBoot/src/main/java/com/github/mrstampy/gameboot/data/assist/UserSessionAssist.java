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

// TODO: Auto-generated Javadoc
/**
 * The Class UserSessionAssist.
 */
@Component
public class UserSessionAssist {
	private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

	@Autowired
	private UserSessionRepository userSessionRepo;

	@Autowired
	private UserRepository userRepo;

	@Autowired
	private ActiveSessions activeSessions;

	/**
	 * Creates the.
	 *
	 * @param user
	 *          the user
	 * @return the user session
	 * @throws IllegalStateException
	 *           the illegal state exception
	 */
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

	/**
	 * Expected user.
	 *
	 * @param userName
	 *          the user name
	 * @return the user
	 * @throws IllegalStateException
	 *           the illegal state exception
	 */
	public User expectedUser(String userName) throws IllegalStateException {
		userNameCheck(userName);

		User user = userRepo.findByUserName(userName);

		check(user == null, "No user for " + userName);

		return user;
	}

	/**
	 * Expected.
	 *
	 * @param userName
	 *          the user name
	 * @return the user session
	 * @throws IllegalStateException
	 *           the illegal state exception
	 */
	public UserSession expected(String userName) throws IllegalStateException {
		userNameCheck(userName);

		return expected(expectedUser(userName));
	}

	/**
	 * Expected.
	 *
	 * @param user
	 *          the user
	 * @return the user session
	 * @throws IllegalStateException
	 *           the illegal state exception
	 */
	public UserSession expected(User user) throws IllegalStateException {
		userCheck(user);

		String userName = user.getUserName();
		check(!activeSessions.hasSession(userName), "No session for " + userName);

		return userSessionRepo.findByUserAndEndedIsNull(user);
	}

	/**
	 * Expected.
	 *
	 * @param id
	 *          the id
	 * @return the user session
	 * @throws IllegalStateException
	 *           the illegal state exception
	 */
	public UserSession expected(long id) throws IllegalStateException {
		if (id <= 0) throw new IllegalStateException("Id must be > 0: " + id);

		check(!activeSessions.hasSession(id), "No session for id " + id);

		return userSessionRepo.findByIdAndEndedIsNull(id);
	}

	/**
	 * Checks for session.
	 *
	 * @param userName
	 *          the user name
	 * @return true, if successful
	 */
	public boolean hasSession(String userName) {
		return activeSessions.hasSession(userName);
	}

	/**
	 * Checks for session.
	 *
	 * @param id
	 *          the id
	 * @return true, if successful
	 */
	public boolean hasSession(long id) {
		return activeSessions.hasSession(id);
	}

	/**
	 * Logout.
	 *
	 * @param userName
	 *          the user name
	 * @return the user
	 * @throws IllegalStateException
	 *           the illegal state exception
	 */
	public User logout(String userName) throws IllegalStateException {
		UserSession session = expected(userName);

		closeSession(session);

		return session.getUser();
	}

	/**
	 * Logout.
	 *
	 * @param id
	 *          the id
	 * @return the user
	 * @throws IllegalStateException
	 *           the illegal state exception
	 */
	public User logout(Long id) throws IllegalStateException {
		UserSession session = expected(id);

		closeSession(session);

		return session.getUser();
	}

	/**
	 * Active sessions.
	 *
	 * @return the list
	 */
	@Cacheable(value = "sessions")
	public List<UserSession> activeSessions() {
		return userSessionRepo.findByEndedIsNull();
	}

	/**
	 * Close session.
	 *
	 * @param session
	 *          the session
	 */
	protected void closeSession(UserSession session) {
		session.setEnded(new Date());

		userSessionRepo.save(session);

		activeSessions.removeSession(session);

		log.info("User {} logged out", session.getUser().getUserName());
	}

	/**
	 * User check.
	 *
	 * @param user
	 *          the user
	 */
	protected void userCheck(User user) {
		check(user == null, "null user");
	}

	/**
	 * User name check.
	 *
	 * @param userName
	 *          the user name
	 * @throws IllegalStateException
	 *           the illegal state exception
	 */
	protected void userNameCheck(String userName) throws IllegalStateException {
		check(isEmpty(userName), "null username");
	}

	/**
	 * Check.
	 *
	 * @param condition
	 *          the condition
	 * @param msg
	 *          the msg
	 */
	protected void check(boolean condition, String msg) {
		if (condition) throw new IllegalStateException(msg);
	}
}

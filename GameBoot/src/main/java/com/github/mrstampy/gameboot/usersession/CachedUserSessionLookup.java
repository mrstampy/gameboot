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
package com.github.mrstampy.gameboot.usersession;

import static org.apache.commons.lang3.StringUtils.isEmpty;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.codahale.metrics.Timer.Context;
import com.github.mrstampy.gameboot.data.entity.UserSession;
import com.github.mrstampy.gameboot.data.repository.UserSessionRepository;
import com.github.mrstampy.gameboot.metrics.MetricsHelper;

/**
 * This class ensures {@link UserSession} lookups use the cacheable
 * {@link UserSessionAssist#activeSessions()} method.
 * CachedUserSessionLookupTest shows 3 - 7X faster lookups vs. database access,
 * for a single session.
 */
@Component
public class CachedUserSessionLookup {

	private static final String CACHED_SESSION_TIMER = "CachedSessionTimer";

	@Autowired
	private UserSessionAssist assist;

	@Autowired
	private ActiveSessions activeSessions;

	@Autowired
	private MetricsHelper helper;

	@Autowired
	private UserSessionRepository repository;

	/**
	 * Post construct.
	 *
	 * @throws Exception
	 *           the exception
	 */
	@PostConstruct
	public void postConstruct() throws Exception {
		helper.timer(CACHED_SESSION_TIMER, CachedUserSessionLookup.class, "cached", "session", "timer");
	}

	/**
	 * Return an expected session for the user, falling back to database retrieval
	 * should the session not yet exist in cache.
	 *
	 * @param userName
	 *          the user name
	 * @return the user session
	 * @throws IllegalStateException
	 *           the illegal state exception
	 */
	public UserSession expected(String userName) throws IllegalStateException {
		Context ctx = helper.startTimer(CACHED_SESSION_TIMER);

		try {
			String noSession = "No session for " + userName;

			check(isEmpty(userName), "No username specified");

			check(!activeSessions.hasSession(userName), noSession);

			List<UserSession> sessions = assist.activeSessions();

			Optional<UserSession> o = find(sessions, us -> us.getUser().getUserName().equals(userName));

			// may not yet be in the cached list
			return o.isPresent() ? o.get() : sessionCheck(repository.findOpenSession(userName));
		} finally {
			ctx.stop();
		}
	}

	/**
	 * Return an expected session for the given id, falling back to database
	 * retrieval should the session not yet exist in cache.
	 *
	 * @param id
	 *          the id
	 * @return the user session
	 * @throws IllegalStateException
	 *           the illegal state exception
	 */
	public UserSession expected(Long id) throws IllegalStateException {
		Context ctx = helper.startTimer(CACHED_SESSION_TIMER);

		try {
			String noSession = "No session for " + id;

			check(id == null, "No session id specified");

			check(!activeSessions.hasSession(id), noSession);

			List<UserSession> sessions = assist.activeSessions();

			Optional<UserSession> o = find(sessions, us -> us.getId().equals(id));

			// may not yet be in the cached list
			return o.isPresent() ? o.get() : sessionCheck(repository.findOpenSession(id));
		} finally {
			ctx.stop();
		}
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

	private UserSession sessionCheck(UserSession session) {
		check(session == null, "No session");

		return session;
	}

	private Optional<UserSession> find(List<UserSession> sessions, Predicate<UserSession> p) {
		return sessions.stream().filter(p).findFirst();
	}

	private void check(boolean condition, String msg) {
		if (condition) throw new IllegalStateException(msg);
	}
}

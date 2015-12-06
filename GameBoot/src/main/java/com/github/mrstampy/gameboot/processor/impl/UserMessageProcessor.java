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

import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;

import java.lang.invoke.MethodHandles;
import java.util.Date;

import javax.annotation.PostConstruct;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.stereotype.Component;

import com.codahale.metrics.Timer.Context;
import com.github.mrstampy.gameboot.data.assist.UserSessionAssist;
import com.github.mrstampy.gameboot.data.entity.User;
import com.github.mrstampy.gameboot.data.entity.User.UserState;
import com.github.mrstampy.gameboot.data.entity.UserSession;
import com.github.mrstampy.gameboot.data.entity.repository.UserRepository;
import com.github.mrstampy.gameboot.messages.Response;
import com.github.mrstampy.gameboot.messages.UserMessage;
import com.github.mrstampy.gameboot.metrics.MetricsHelper;
import com.github.mrstampy.gameboot.processor.AbstractGameBootProcessor;

// TODO: Auto-generated Javadoc
/**
 * The Class UserMessageProcessor.
 */
@Component
public class UserMessageProcessor extends AbstractGameBootProcessor<UserMessage> {
	private static final String USER_UPDATE_COUNTER = "UserUpdateCounter";

	private static final String USER_DELETE_COUNTER = "UserDeleteCounter";

	private static final String USER_CREATE_COUNTER = "UserCreateCounter";

	private static final String USER_LOGOUT_COUNTER = "UserLogoutCounter";

	private static final String USER_LOGIN_COUNTER = "UserLoginCounter";

	private static final String USER_TIMER = "UserTImer";

	private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

	@Autowired
	private UserRepository userRepo;

	@Autowired
	private UserSessionAssist assist;

	@Autowired
	private MetricsHelper helper;

	/**
	 * Post construct.
	 *
	 * @throws Exception
	 *           the exception
	 */
	@PostConstruct
	public void postConstruct() throws Exception {
		helper.timer(USER_TIMER, UserMessageProcessor.class, "user", "process", "timer");
		helper.counter(USER_LOGIN_COUNTER, UserMessageProcessor.class, "login", "counter");
		helper.counter(USER_LOGOUT_COUNTER, UserMessageProcessor.class, "logout", "counter");
		helper.counter(USER_CREATE_COUNTER, UserMessageProcessor.class, "create", "counter");
		helper.counter(USER_DELETE_COUNTER, UserMessageProcessor.class, "delete", "counter");
		helper.counter(USER_UPDATE_COUNTER, UserMessageProcessor.class, "update", "counter");
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.github.mrstampy.gameboot.processor.AbstractGameBootProcessor#validate(
	 * com.github.mrstampy.gameboot.messages.AbstractGameBootMessage)
	 */
	@Override
	protected void validate(UserMessage message) throws Exception {
		if (message.getFunction() == null) fail("function must be one of CREATE, DELETE, UPDATE, LOGIN, LOGOUT");

		if (isEmpty(message.getUserName())) fail("userName must be supplied");

		switch (message.getFunction()) {
		case LOGIN:
		case CREATE:
			if (isEmpty(message.getNewPassword())) fail("new password must be supplied");
			break;
		case DELETE:
			break;
		case UPDATE:
			if (noData(message)) fail("No user data to update");
			break;
		case LOGOUT:
			break;
		}

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.github.mrstampy.gameboot.processor.AbstractGameBootProcessor#
	 * processImpl(com.github.mrstampy.gameboot.messages.AbstractGameBootMessage)
	 */
	@Override
	protected Response processImpl(UserMessage message) throws Exception {
		Context ctx = helper.startTimer(USER_TIMER);
		try {
			switch (message.getFunction()) {
			case CREATE:
				helper.incr(USER_CREATE_COUNTER);
				return createNewUser(message);
			case DELETE:
				helper.incr(USER_DELETE_COUNTER);
				return deleteUser(message);
			case UPDATE:
				helper.incr(USER_UPDATE_COUNTER);
				return updateUser(message);
			case LOGIN:
				helper.incr(USER_LOGIN_COUNTER);
				return loginUser(message);
			case LOGOUT:
				helper.incr(USER_LOGOUT_COUNTER);
				return logoutUser(message);
			default:
				log.error("Inaccessible: UserMessage.function is broken for {}", message);
				return failure("Should never reach here");
			}
		} finally {
			ctx.stop();
		}
	}

	private boolean noData(UserMessage message) {
		//@formatter:off
		return isEmpty(message.getNewPassword()) && 
				isEmpty(message.getEmail()) && 
				isEmpty(message.getFirstName()) && 
				isEmpty(message.getLastName()) && 
				message.getState() == null &&
				message.getDob() == null;
		//@formatter:on
	}

	private Response logoutUser(UserMessage message) {
		String userName = message.getUserName();

		return success(assist.logout(userName));
	}

	private Response loginUser(UserMessage message) {
		String userName = message.getUserName();
		User user = assist.expectedUser(userName);

		switch (user.getState()) {
		case ACTIVE:
			break;
		default:
			fail(userName + " is in state " + user.getState());
		}

		boolean ok = BCrypt.checkpw(message.getNewPassword(), user.getPasswordHash());

		log.info("Login for {} is {}", userName, ok);

		return ok ? createSession(user) : failure("Password is invalid");
	}

	private Response createSession(User user) {
		UserSession session = assist.create(user);

		return success(user, session);
	}

	private Response updateUser(UserMessage message) {
		String userName = message.getUserName();
		User user = assist.expectedUser(userName);

		boolean changed = populateForUpdate(message, user);

		if (changed) user = userRepo.save(user);

		log.info("Updated user {}? {}", user, changed);

		return changed ? success(user) : failure(user);
	}

	private Response deleteUser(UserMessage message) {
		String userName = message.getUserName();
		User user = assist.expectedUser(userName);

		if (assist.hasSession(userName)) assist.logout(userName);

		user.setState(UserState.DELETED);

		userRepo.save(user);

		log.info("Set user status for {} to DELETED", user.getUserName());

		return success(user);
	}

	private Response createNewUser(UserMessage message) {
		User user = createUser(message);

		user = userRepo.save(user);

		log.info("Created user {}", user);

		return success(user);
	}

	private boolean populateForUpdate(UserMessage message, User user) {
		boolean changed = false;

		String userName = message.getUserName();

		String email = message.getEmail();
		if (changed(email, user.getEmail())) {
			log.trace("Changing email from {} to {} for {}", user.getEmail(), email, userName);

			user.setEmail(email);
			changed = true;
		}

		String firstName = message.getFirstName();
		if (changed(firstName, user.getFirstName())) {
			log.trace("Changing first name from {} to {} for {}", user.getFirstName(), firstName, userName);

			user.setFirstName(firstName);
			changed = true;
		}

		String lastName = message.getLastName();
		if (changed(lastName, user.getLastName())) {
			log.trace("Changing last name from {} to {} for {}", user.getLastName(), lastName, userName);

			user.setLastName(lastName);
			changed = true;
		}

		if (isNotEmpty(message.getNewPassword())) {
			log.trace("Changing password for {}", userName);

			if (!BCrypt.checkpw(message.getOldPassword(), user.getPasswordHash())) fail("Old Password is invalid");

			setPasswordHash(message, user);
			changed = true;
		}

		Date dob = message.getDob();
		if (changed(dob, user.getDob())) {
			log.trace("Changing dob from {} to {} for {}", user.getDob(), dob, userName);

			changed = true;
			user.setDob(dob);
		}

		UserState state = message.getState();
		if (changed(state, user.getState())) {
			log.trace("Changing state from {} to {} for {}", user.getState(), state, userName);

			changed = true;
			user.setState(state);
		}

		return changed;
	}

	private boolean changed(Object in, Object exist) {
		return in != null && !EqualsBuilder.reflectionEquals(in, exist);
	}

	private User createUser(UserMessage message) {
		User user = new User();

		user.setDob(message.getDob());
		user.setEmail(message.getEmail());
		user.setFirstName(message.getFirstName());
		user.setLastName(message.getLastName());
		user.setUserName(message.getUserName());
		user.setState(UserState.ACTIVE);

		setPasswordHash(message, user);

		return user;
	}

	private void setPasswordHash(UserMessage message, User user) {
		String salt = BCrypt.gensalt();
		user.setPasswordHash(BCrypt.hashpw(message.getNewPassword(), salt));
	}

}

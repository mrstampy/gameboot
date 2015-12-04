package com.github.mrstampy.gameboot.processor.impl;

import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;

import java.lang.invoke.MethodHandles;
import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.stereotype.Component;

import com.github.mrstampy.gameboot.data.entity.User;
import com.github.mrstampy.gameboot.data.entity.repository.UserRepository;
import com.github.mrstampy.gameboot.messages.AbstractGameBootMessage;
import com.github.mrstampy.gameboot.messages.MessageType;
import com.github.mrstampy.gameboot.messages.UserMessage;
import com.github.mrstampy.gameboot.processor.AbstractGameBootProcessor;

@Component
public class UserMessageProcessor extends AbstractGameBootProcessor<UserMessage> {
	private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

	@Autowired
	private UserRepository repository;

	@Override
	public MessageType getType() {
		return MessageType.CREATE_USER;
	}

	@Override
	protected void validate(UserMessage message) throws Exception {
		if (message.getFunction() == null) fail("function must be one of CREATE, DELETE, UPDATE, LOGIN");

		if (isEmpty(message.getUserName())) fail("userName must be supplied");

		switch (message.getFunction()) {
		case LOGIN:
			if (isEmpty(message.getOldPassword())) fail("old password must be supplied");
		case CREATE:
			if (isEmpty(message.getNewPassword())) fail("new password must be supplied");
			break;
		case DELETE:
			break;
		case UPDATE:
			//@formatter:off
			if (isEmpty(message.getNewPassword()) && 
					isEmpty(message.getEmail()) && 
					isEmpty(message.getFirstName()) && 
					isEmpty(message.getLastName()) && 
					message.getDob() == null) {
				fail("No user data to update");
			}
			//@formatter:on
			break;
		}

	}

	@Override
	protected AbstractGameBootMessage processImpl(UserMessage message) throws Exception {
		switch (message.getFunction()) {
		case CREATE:
			return createNewUser(message);
		case DELETE:
			return deleteUser(message);
		case UPDATE:
			return updateUser(message);
		case LOGIN:
			return loginUser(message);
		default:
			log.error("Inaccessible: UserMessage.function is broken for {}", message);
			return failure("Should never reach here");
		}
	}

	private AbstractGameBootMessage loginUser(UserMessage message) {
		String userName = message.getUserName();
		User user = getUser(userName);

		boolean ok = BCrypt.checkpw(message.getNewPassword(), user.getPasswordHash());

		log.debug("Login for {} is {}", userName, ok);

		return ok ? success() : failure("Password is invalid");
	}

	private AbstractGameBootMessage updateUser(UserMessage message) {
		String userName = message.getUserName();
		User user = getUser(userName);

		boolean changed = populateForUpdate(message, user);

		if (changed) user = repository.save(user);

		log.debug("Updated user {}? {}", user, changed);

		return changed ? success("User " + userName + " updated") : failure("User " + userName + " unchanged");
	}

	private AbstractGameBootMessage deleteUser(UserMessage message) {
		String userName = message.getUserName();
		User user = getUser(userName);

		repository.delete(user.getId());

		log.debug("Deleted user {}", user);

		return success("User " + userName + " deleted");
	}

	private AbstractGameBootMessage createNewUser(UserMessage message) {
		User user = createUser(message);

		user = repository.save(user);

		log.debug("Created user {}", user);

		return success("User " + user.getUserName() + " created");
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

			if (!BCrypt.checkpw(message.getOldPassword(), user.getPasswordHash())) fail("Password is invalid");

			setPasswordHash(message, user);
			changed = true;
		}

		Date dob = message.getDob();
		if (changed(dob, user.getDob())) {
			log.trace("Changing dob from {} to {} for {}", user.getDob(), dob, userName);

			changed = true;
			user.setDob(dob);
		}

		return changed;
	}

	private boolean changed(String in, String exist) {
		return (isEmpty(in) && isNotEmpty(exist)) || (isNotEmpty(in) && !in.equals(exist));
	}

	private boolean changed(Date in, Date existing) {
		return (in == null && existing != null) || (in != null && !in.equals(existing));
	}

	private User getUser(String userName) {
		User user = repository.findByUserName(userName);
		if (user == null) fail("No user for " + userName);

		return user;
	}

	private User createUser(UserMessage message) {
		User user = new User();

		user.setDob(message.getDob());
		user.setEmail(message.getEmail());
		user.setFirstName(message.getFirstName());
		user.setLastName(message.getLastName());
		user.setUserName(message.getUserName());

		setPasswordHash(message, user);

		return user;
	}

	private void setPasswordHash(UserMessage message, User user) {
		String salt = BCrypt.gensalt();
		user.setPasswordHash(BCrypt.hashpw(message.getNewPassword(), salt));
	}

}

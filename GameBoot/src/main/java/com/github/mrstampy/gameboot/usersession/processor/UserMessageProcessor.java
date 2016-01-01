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
package com.github.mrstampy.gameboot.usersession.processor;

import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;

import java.lang.invoke.MethodHandles;
import java.util.Date;
import java.util.Locale;
import java.util.Optional;

import javax.annotation.PostConstruct;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCrypt;

import com.codahale.metrics.Timer.Context;
import com.github.mrstampy.gameboot.locale.processor.LocaleRegistry;
import com.github.mrstampy.gameboot.messages.Response;
import com.github.mrstampy.gameboot.metrics.MetricsHelper;
import com.github.mrstampy.gameboot.processor.AbstractTransactionalGameBootProcessor;
import com.github.mrstampy.gameboot.processor.GameBootProcessor;
import com.github.mrstampy.gameboot.systemid.SystemIdKey;
import com.github.mrstampy.gameboot.usersession.UserSessionAssist;
import com.github.mrstampy.gameboot.usersession.UserSessionLookup;
import com.github.mrstampy.gameboot.usersession.data.entity.User;
import com.github.mrstampy.gameboot.usersession.data.entity.User.UserState;
import com.github.mrstampy.gameboot.usersession.data.entity.UserSession;
import com.github.mrstampy.gameboot.usersession.data.repository.UserRepository;
import com.github.mrstampy.gameboot.usersession.messages.UserMessage;

/**
 * This implementation of a {@link GameBootProcessor} processes
 * {@link UserMessage}s for user creation, update, delete, login and logout.
 */
public class UserMessageProcessor extends AbstractTransactionalGameBootProcessor<UserMessage> {
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

  @Autowired
  private UserSessionLookup lookup;

  @Autowired
  private LocaleRegistry localeRegistry;

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
   * @see com.github.mrstampy.gameboot.processor.GameBootProcessor#getType()
   */
  @Override
  public String getType() {
    return UserMessage.TYPE;
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
    SystemIdKey id = message.getSystemId();
    if (message.getFunction() == null) fail(getResponseContext(INVALID_USER_FUNCTION, id), "Invalid function");

    if (isEmpty(message.getUserName())) fail(getResponseContext(NO_USERNAME, id), "userName must be supplied");

    checkLocale(message.getLanguageCode(), "languageCode");
    checkLocale(message.getCountryCode(), "countryCode");

    switch (message.getFunction()) {
    case LOGIN:
      if (isEmpty(message.getOldPassword())) {
        fail(getResponseContext(OLD_PASSWORD_MISSING, id), "old password must be supplied");
      }
      break;
    case CREATE:
      if (isEmpty(message.getNewPassword())) {
        fail(getResponseContext(NEW_PASSWORD_MISSING, id), "new password must be supplied");
      }
      break;
    case DELETE:
      break;
    case UPDATE:
      if (noData(message)) fail(getResponseContext(NO_USER_DATA, id), "No user data to update");
      break;
    case LOGOUT:
      break;
    }

  }

  private void checkLocale(String code, String name) {
    if (isNotEmpty(code) && code.length() != 2) {
      fail(getResponseContext(MUST_BE, name, "2 characters."), name + " not 2 characters: " + code);
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
    Optional<Context> ctx = helper.startTimer(USER_TIMER);
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
        return failure(getResponseContext(UNEXPECTED_ERROR, message.getSystemId()), message, "Should never reach here");
      }
    } finally {
      helper.stopTimer(ctx);
    }
  }

  /**
   * No data.
   *
   * @param message
   *          the message
   * @return true, if successful
   */
  protected boolean noData(UserMessage message) {
    //@formatter:off
    return isEmpty(message.getNewPassword()) && 
        isEmpty(message.getEmail()) && 
        isEmpty(message.getFirstName()) && 
        isEmpty(message.getLastName()) && 
        isEmpty(message.getLanguageCode()) &&
        isEmpty(message.getCountryCode()) &&
        message.getState() == null && 
        message.getDob() == null;
    //@formatter:on
  }

  /**
   * Logout user.
   *
   * @param message
   *          the message
   * @return the response
   */
  protected Response logoutUser(UserMessage message) {
    String userName = message.getUserName();

    return success(message, assist.logout(userName));
  }

  /**
   * Login user.
   *
   * @param message
   *          the message
   * @return the response
   */
  protected Response loginUser(UserMessage message) {
    String userName = message.getUserName();
    User user = assist.expectedUser(userName);

    SystemIdKey id = message.getSystemId();
    switch (user.getState()) {
    case ACTIVE:
      break;
    default:
      fail(getResponseContext(CANNOT_DELETE_USER, id), userName + " is in state " + user.getState());
    }

    boolean ok = BCrypt.checkpw(message.getOldPassword(), user.getPasswordHash());

    log.info("Login for {} is {}", userName, ok);

    setLocale(message);

    //@formatter:off
    return ok ? 
        createSession(message, user) : 
        failure(getResponseContext(INVALID_PASSWORD, id), message, "Password is invalid");
    //@formatter:on
  }

  private void setLocale(UserMessage message) {
    if (isEmpty(message.getCountryCode()) && isEmpty(message.getLanguageCode())) return;

    Locale locale = null;
    if (isEmpty(message.getCountryCode())) {
      locale = new Locale(message.getLanguageCode());
    } else {
      locale = new Locale(message.getLanguageCode(), message.getCountryCode());
    }

    localeRegistry.put(message.getSystemId(), locale);
  }

  /**
   * Creates the session.
   *
   * @param message
   *          the message
   * @param user
   *          the user
   * @return the response
   */
  protected Response createSession(UserMessage message, User user) {
    UserSession session = assist.create(user);

    Response r = success(message, session);

    r.setMappingKeys(new UsernameKey(user.getUserName()), new UserSessionKey(session));

    return r;
  }

  /**
   * Update user.
   *
   * @param message
   *          the message
   * @return the response
   */
  protected Response updateUser(UserMessage message) {
    String userName = message.getUserName();
    User user = assist.expectedUser(userName);

    boolean changed = populateForUpdate(message, user);

    if (changed) user = userRepo.save(user);

    log.info("Updated user {}? {}", user, changed);

    localeCheck(user, message);

    //@formatter:off
    return changed ? 
        success(message, user) : 
        failure(getResponseContext(USER_UNCHANGED, message.getSystemId()), message, user);
    //@formatter:on
  }

  private void localeCheck(User user, UserMessage message) {
    if (isEmpty(message.getCountryCode()) && isEmpty(message.getLanguageCode())) return;

    Locale locale = null;
    if (isEmpty(user.getCountryCode())) {
      locale = new Locale(user.getLanguageCode());
    } else {
      locale = new Locale(user.getLanguageCode(), user.getCountryCode());
    }

    localeRegistry.put(message.getSystemId(), locale);
  }

  /**
   * Delete user.
   *
   * @param message
   *          the message
   * @return the response
   */
  protected Response deleteUser(UserMessage message) {
    String userName = message.getUserName();
    User user = assist.expectedUser(userName);

    if (assist.hasSession(userName)) assist.logout(userName);

    user.setState(UserState.DELETED);

    userRepo.save(user);

    log.info("Set user status for {} to DELETED", user.getUserName());

    lookup.clearMDC();

    return success(message, user);
  }

  /**
   * Creates the new user.
   *
   * @param message
   *          the message
   * @return the response
   */
  protected Response createNewUser(UserMessage message) {
    User user = createUser(message);

    user = userRepo.save(user);

    log.info("Created user {}", user);

    return success(message, user);
  }

  /**
   * Populate for update.
   *
   * @param message
   *          the message
   * @param user
   *          the user
   * @return true, if successful
   */
  protected boolean populateForUpdate(UserMessage message, User user) {
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

      if (!BCrypt.checkpw(message.getOldPassword(), user.getPasswordHash())) {
        fail(getResponseContext(INVALID_PASSWORD, message.getSystemId()), "Old Password is invalid");
      }

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

    String languageCode = message.getLanguageCode();
    if (changed(languageCode, user.getLanguageCode())) {
      log.trace("Changing language code from {} to {} for {}",
          user.getLanguageCode(),
          languageCode,
          user.getUserName());

      changed = true;
      user.setLanguageCode(languageCode);
    }

    String countryCode = message.getCountryCode();
    if (changed(countryCode, user.getCountryCode())) {
      log.trace("Changing country code from {} to {} for {}", user.getCountryCode(), countryCode, user.getUserName());

      changed = true;
      user.setCountryCode(countryCode);
    }

    return changed;
  }

  private boolean changed(Object in, Object exist) {
    return in != null && !EqualsBuilder.reflectionEquals(in, exist);
  }

  /**
   * Creates the user.
   *
   * @param message
   *          the message
   * @return the user
   */
  protected User createUser(UserMessage message) {
    User user = new User();

    user.setDob(message.getDob());
    user.setEmail(message.getEmail());
    user.setFirstName(message.getFirstName());
    user.setLastName(message.getLastName());
    user.setUserName(message.getUserName());
    user.setState(UserState.ACTIVE);
    user.setLanguageCode(message.getLanguageCode());
    user.setCountryCode(message.getCountryCode());

    setPasswordHash(message, user);

    return user;
  }

  /**
   * Sets the password hash.
   *
   * @param message
   *          the message
   * @param user
   *          the user
   */
  protected void setPasswordHash(UserMessage message, User user) {
    String salt = BCrypt.gensalt();
    user.setPasswordHash(BCrypt.hashpw(message.getNewPassword(), salt));
  }

}

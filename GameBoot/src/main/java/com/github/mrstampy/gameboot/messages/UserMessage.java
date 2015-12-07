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
package com.github.mrstampy.gameboot.messages;

import java.util.Date;

import org.apache.commons.lang3.builder.ToStringBuilder;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonFormat.Shape;
import com.github.mrstampy.gameboot.data.entity.User;
import com.github.mrstampy.gameboot.data.entity.User.UserState;

/**
 * Message class to perform operations on/for a {@link User}.
 */
public class UserMessage extends AbstractGameBootMessage {

  private String userName;

  private String newPassword;

  private String oldPassword;

  private String firstName;

  private String lastName;

  private String email;

  private UserState state;

  @JsonFormat(shape = Shape.STRING, pattern = "yyyy/MM/dd")
  private Date dob;

  /**
   * The Enum Function.
   */
  public enum Function {

    /** The create. */
    CREATE,
    /** The update. */
    UPDATE,
    /** The delete. */
    DELETE,
    /** The login. */
    LOGIN,
    /** The logout. */
    LOGOUT
  }

  private Function function;

  /**
   * Gets the user name.
   *
   * @return the user name
   */
  public String getUserName() {
    return userName;
  }

  /**
   * Sets the user name.
   *
   * @param userName
   *          the new user name
   */
  public void setUserName(String userName) {
    this.userName = userName;
  }

  /**
   * Gets the new password.
   *
   * @return the new password
   */
  public String getNewPassword() {
    return newPassword;
  }

  /**
   * Sets the new password.
   *
   * @param password
   *          the new new password
   */
  public void setNewPassword(String password) {
    this.newPassword = password;
  }

  /**
   * Gets the first name.
   *
   * @return the first name
   */
  public String getFirstName() {
    return firstName;
  }

  /**
   * Sets the first name.
   *
   * @param firstName
   *          the new first name
   */
  public void setFirstName(String firstName) {
    this.firstName = firstName;
  }

  /**
   * Gets the last name.
   *
   * @return the last name
   */
  public String getLastName() {
    return lastName;
  }

  /**
   * Sets the last name.
   *
   * @param lastName
   *          the new last name
   */
  public void setLastName(String lastName) {
    this.lastName = lastName;
  }

  /**
   * Gets the dob.
   *
   * @return the dob
   */
  public Date getDob() {
    return dob;
  }

  /**
   * Sets the dob.
   *
   * @param dob
   *          the new dob
   */
  public void setDob(Date dob) {
    this.dob = dob;
  }

  /**
   * Gets the email.
   *
   * @return the email
   */
  public String getEmail() {
    return email;
  }

  /**
   * Sets the email.
   *
   * @param email
   *          the new email
   */
  public void setEmail(String email) {
    this.email = email;
  }

  /**
   * Gets the function.
   *
   * @return the function
   */
  public Function getFunction() {
    return function;
  }

  /**
   * Sets the function.
   *
   * @param function
   *          the new function
   */
  public void setFunction(Function function) {
    this.function = function;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * com.github.mrstampy.gameboot.messages.AbstractGameBootMessage#toString()
   */
  @Override
  public String toString() {
    // no passwords in teh logz
    ToStringBuilder tsb = new ToStringBuilder(this);

    //@formatter:off
    tsb
      .append(getUserName())
      .append(getFunction())
      .append(getFirstName())
      .append(getLastName())
      .append(getEmail())
      .append(getDob())
      .append(getState());
    //@formatter:on

    return tsb.toString();
  }

  /**
   * Gets the old password.
   *
   * @return the old password
   */
  public String getOldPassword() {
    return oldPassword;
  }

  /**
   * Sets the old password.
   *
   * @param oldPassword
   *          the new old password
   */
  public void setOldPassword(String oldPassword) {
    this.oldPassword = oldPassword;
  }

  /**
   * Gets the state.
   *
   * @return the state
   */
  public UserState getState() {
    return state;
  }

  /**
   * Sets the state.
   *
   * @param state
   *          the new state
   */
  public void setState(UserState state) {
    this.state = state;
  }
}

package com.github.mrstampy.gameboot.messages;

import java.util.Date;

import org.apache.commons.lang3.builder.ToStringBuilder;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonFormat.Shape;
import com.github.mrstampy.gameboot.data.entity.User.UserState;

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

	public enum Function {
		CREATE, UPDATE, DELETE, LOGIN, LOGOUT
	}

	private Function function;

	public UserMessage() {
		super(MessageType.USER);
	}

	public String getUserName() {
		return userName;
	}

	public void setUserName(String userName) {
		this.userName = userName;
	}

	public String getNewPassword() {
		return newPassword;
	}

	public void setNewPassword(String password) {
		this.newPassword = password;
	}

	public String getFirstName() {
		return firstName;
	}

	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	public String getLastName() {
		return lastName;
	}

	public void setLastName(String lastName) {
		this.lastName = lastName;
	}

	public Date getDob() {
		return dob;
	}

	public void setDob(Date dob) {
		this.dob = dob;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public Function getFunction() {
		return function;
	}

	public void setFunction(Function function) {
		this.function = function;
	}

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

	public String getOldPassword() {
		return oldPassword;
	}

	public void setOldPassword(String oldPassword) {
		this.oldPassword = oldPassword;
	}

	public UserState getState() {
		return state;
	}

	public void setState(UserState state) {
		this.state = state;
	}
}

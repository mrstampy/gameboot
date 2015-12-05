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

import com.fasterxml.jackson.annotation.JsonIgnore;

// TODO: Auto-generated Javadoc
/**
 * The Class Response.
 */
public class Response extends AbstractGameBootMessage {

	/**
	 * The Enum ResponseCode.
	 */
	public enum ResponseCode {

		/** The success. */
		SUCCESS,
		/** The failure. */
		FAILURE,
		/** The warning. */
		WARNING,
		/** The info. */
		INFO,
		/** The alert. */
		ALERT
	}

	private ResponseCode responseCode;

	private Object[] response;

	/**
	 * Instantiates a new response.
	 */
	public Response() {
	}

	/**
	 * Instantiates a new response.
	 *
	 * @param responseCode
	 *          the response code
	 * @param response
	 *          the response
	 */
	public Response(ResponseCode responseCode, Object... response) {
		this();
		setResponseCode(responseCode);
		setResponse(response);
	}

	/**
	 * Gets the response code.
	 *
	 * @return the response code
	 */
	public ResponseCode getResponseCode() {
		return responseCode;
	}

	/**
	 * Sets the response code.
	 *
	 * @param responseCode
	 *          the new response code
	 */
	public void setResponseCode(ResponseCode responseCode) {
		this.responseCode = responseCode;
	}

	/**
	 * Gets the response.
	 *
	 * @return the response
	 */
	public Object[] getResponse() {
		return response;
	}

	/**
	 * Sets the response.
	 *
	 * @param response
	 *          the new response
	 */
	public void setResponse(Object... response) {
		this.response = response;
	}

	/**
	 * Checks if is success.
	 *
	 * @return true, if is success
	 */
	@JsonIgnore
	public boolean isSuccess() {
		return isResponseCode(ResponseCode.SUCCESS);
	}

	private boolean isResponseCode(ResponseCode rc) {
		return rc == getResponseCode();
	}

}

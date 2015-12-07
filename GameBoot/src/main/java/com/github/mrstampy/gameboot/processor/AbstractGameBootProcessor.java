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
package com.github.mrstampy.gameboot.processor;

import java.lang.invoke.MethodHandles;

import javax.transaction.Transactional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.mrstampy.gameboot.messages.AbstractGameBootMessage;
import com.github.mrstampy.gameboot.messages.Response;
import com.github.mrstampy.gameboot.messages.Response.ResponseCode;

/**
 * Abstract superclass for {@link GameBootProcessor}s ensuring processing occurs
 * within a transaction.
 *
 * @param <M>
 *          the generic type
 */
public abstract class AbstractGameBootProcessor<M extends AbstractGameBootMessage> implements GameBootProcessor<M> {
	private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.github.mrstampy.gameboot.processor.GameBootProcessor#process(com.github
	 * .mrstampy.gameboot.messages.AbstractGameBootMessage)
	 */
	@Transactional
	@Override
	public final Response process(M message) throws Exception {
		log.debug("Processing message {}", message);

		if (message == null) fail("Null message");

		try {
			validate(message);

			Response response = processImpl(message);
			response.setId(message.getId());

			log.debug("Created response {} for {}", response, message);

			return response;
		} catch (Exception e) {
			log.error("Error in processing {}", message, e);

			return new Response(ResponseCode.FAILURE, e.getMessage());
		}
	}

	/**
	 * Fail, throwing a {@link RuntimeException} with the specified message. The
	 * exception is caught by the {@link #process(AbstractGameBootMessage)}
	 * implementation which after logging returns a failure response.
	 *
	 * @param message
	 *          the message
	 */
	protected void fail(String message) throws RuntimeException {
		throw new RuntimeException(message);
	}

	/**
	 * Returns an initialized success {@link Response}.
	 *
	 * @param message
	 *          the message
	 * @return the response
	 */
	protected Response success(Object... message) {
		return new Response(ResponseCode.SUCCESS, message);
	}

	/**
	 * Returns an initialized failure {@link Response}.
	 *
	 * @param message
	 *          the message
	 * @return the response
	 */
	protected Response failure(Object... message) {
		return new Response(ResponseCode.FAILURE, message);
	}

	/**
	 * Implement to perform any pre-processing validation.
	 *
	 * @param message
	 *          the message
	 * @throws Exception
	 *           the exception
	 */
	protected abstract void validate(M message) throws Exception;

	/**
	 * Implement to process the {@link #validate(AbstractGameBootMessage)}'ed
	 * message.
	 *
	 * @param message
	 *          the message
	 * @return the response
	 * @throws Exception
	 *           the exception
	 */
	protected abstract Response processImpl(M message) throws Exception;

}

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
import com.github.mrstampy.gameboot.messages.context.ResponseContext;
import com.github.mrstampy.gameboot.netty.AbstractNettyProcessor;
import com.github.mrstampy.gameboot.processor.GameBootProcessor;
import com.github.mrstampy.gameboot.util.registry.AbstractRegistryKey;
import com.github.mrstampy.gameboot.util.registry.GameBootRegistry;
import com.github.mrstampy.gameboot.web.WebProcessor;
import com.github.mrstampy.gameboot.websocket.AbstractWebSocketProcessor;

/**
 * The response to (intended) all GameBoot messages. {@link #getResponseCode()}
 * defines the type of message while the response can be null, strings, JSON, or
 * a mix of strings and JSON.
 */
public class Response extends AbstractGameBootMessage {

  /** The Constant TYPE. */
  public static final String TYPE = "Response";

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
    ALERT,
    /** The critical. */
    CRITICAL
  }

  private ResponseCode responseCode;

  private ResponseContext context;

  private Object[] payload;

  private AbstractRegistryKey<?>[] mappingKeys;

  /**
   * Instantiates a new response.
   */
  public Response() {
    super(TYPE);
  }

  /**
   * Instantiates a new response.
   *
   * @param message
   *          the message
   * @param responseCode
   *          the response code
   * @param payload
   *          the payload
   */
  public Response(AbstractGameBootMessage message, ResponseCode responseCode, Object... payload) {
    this(responseCode, payload);
    if (message != null) {
      setType(message.getType());
      setId(message.getId());
    }
  }

  /**
   * Instantiates a new response.
   *
   * @param responseCode
   *          the response code
   * @param payload
   *          the payload
   */
  public Response(ResponseCode responseCode, Object... payload) {
    this();
    setResponseCode(responseCode);
    setPayload(payload);
  }

  /**
   * Instantiates a new response.
   *
   * @param message
   *          the message
   * @param responseCode
   *          the response code
   * @param error
   *          the error
   * @param payload
   *          the payload
   */
  public Response(AbstractGameBootMessage message, ResponseCode responseCode, ResponseContext error,
      Object... payload) {
    this(message, responseCode, payload);
    setContext(error);
  }

  /**
   * Instantiates a new response.
   *
   * @param responseCode
   *          the response code
   * @param error
   *          the error
   * @param payload
   *          the payload
   */
  public Response(ResponseCode responseCode, ResponseContext error, Object... payload) {
    this(responseCode, payload);
    setContext(error);
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
   * Gets the payload.
   *
   * @return the payload
   */
  public Object[] getPayload() {
    return payload;
  }

  /**
   * Sets the payload.
   *
   * @param payload
   *          the new payload
   */
  public void setPayload(Object... payload) {
    if (payload != null && payload.length == 0) payload = null;
    this.payload = payload;
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

  /**
   * Gets the mapping keys.
   *
   * @return the mapping keys
   */
  @JsonIgnore
  public AbstractRegistryKey<?>[] getMappingKeys() {
    return mappingKeys;
  }

  /**
   * Sets the mapping keys. {@link GameBootProcessor} implementations can use
   * this method to pass any mapping keys (userName, sessionId) to the
   * infrastructure for ease of lookups in the various {@link GameBootRegistry}
   * s.
   *
   * @param mappingKeys
   *          the new mapping keys
   * @see AbstractNettyProcessor#process(io.netty.channel.ChannelHandlerContext,
   *      com.github.mrstampy.gameboot.controller.GameBootMessageController,
   *      AbstractGameBootMessage)
   * @see AbstractWebSocketProcessor#process(org.springframework.web.socket.WebSocketSession,
   *      com.github.mrstampy.gameboot.controller.GameBootMessageController,
   *      AbstractGameBootMessage)
   * @see WebProcessor#process(javax.servlet.http.HttpSession,
   *      com.github.mrstampy.gameboot.controller.GameBootMessageController,
   *      AbstractGameBootMessage)
   */
  public void setMappingKeys(AbstractRegistryKey<?>... mappingKeys) {
    this.mappingKeys = mappingKeys;
  }

  /**
   * Gets the error.
   *
   * @return the error
   */
  public ResponseContext getContext() {
    return context;
  }

  /**
   * Sets the error.
   *
   * @param error
   *          the new error
   */
  public void setContext(ResponseContext error) {
    this.context = error;
  }

}

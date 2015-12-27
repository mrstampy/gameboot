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
package com.github.mrstampy.gameboot.otp.processor;

import java.lang.invoke.MethodHandles;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.github.mrstampy.gameboot.messages.Response;
import com.github.mrstampy.gameboot.messages.Response.ResponseCode;
import com.github.mrstampy.gameboot.otp.KeyRegistry;
import com.github.mrstampy.gameboot.otp.OneTimePad;
import com.github.mrstampy.gameboot.otp.OtpConfiguration;
import com.github.mrstampy.gameboot.otp.messages.OtpKeyRequest;
import com.github.mrstampy.gameboot.otp.messages.OtpMessage;
import com.github.mrstampy.gameboot.otp.netty.OtpClearNettyHandler;
import com.github.mrstampy.gameboot.otp.netty.OtpEncryptedNettyHandler;
import com.github.mrstampy.gameboot.otp.websocket.OtpClearWebSocketHandler;
import com.github.mrstampy.gameboot.otp.websocket.OtpEncryptedWebSocketHandler;
import com.github.mrstampy.gameboot.processor.AbstractGameBootProcessor;

/**
 * The Class OtpNewKeyRequestProcessor generates a key from a request sent on an
 * encrypted channel for encrypting data sent on a related clear channel. If the
 * {@link OtpKeyRequest#getKeySize()} has not been set a default size specified
 * by the GameBoot property 'otp.default.key.size' will be used. If set the
 * value must be greater than 0 and must be a power of 2. Keys size must also be
 * a power of 2 and sizes must be greater than or equal to all message sizes
 * sent in the unencrypted channel. The {@link OtpKeyRequest#getSystemId()}
 * value will be the value obtained from the clear channel.
 * 
 * @see OtpClearNettyHandler
 * @see OtpEncryptedNettyHandler
 * @see OtpClearWebSocketHandler
 * @see OtpEncryptedWebSocketHandler
 */
@Component
@Profile(OtpConfiguration.OTP_PROFILE)
public class OtpKeyRequestProcessor extends AbstractGameBootProcessor<OtpKeyRequest> {
  private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Autowired
  private OtpNewKeyRegistry newKeyRegistry;

  @Autowired
  private KeyRegistry registry;

  @Autowired
  private OneTimePad pad;

  @Value("${otp.default.key.size}")
  private Integer defaultKeySize;

  @Value("${otp.maximum.key.size}")
  private Integer maxKeySize;

  /**
   * Gets the type.
   *
   * @return the type
   */
  @Override
  public String getType() {
    return OtpKeyRequest.TYPE;
  }

  /**
   * Validate.
   *
   * @param message
   *          the message
   * @throws Exception
   *           the exception
   */
  @Override
  protected void validate(OtpKeyRequest message) throws Exception {
    Long id = message.getOtpSystemId();
    if (message.getKeyFunction() == null) {
      fail(getResponseContext(INVALID_KEY_FUNCTION, id), "keyFunction one of NEW, DELETE");
    }

    if (id == null) fail(getResponseContext(NO_SYSTEM_ID), "No systemId");

    switch (message.getKeyFunction()) {
    case DELETE:
      if (!id.equals(message.getProcessorKey())) {
        fail(getResponseContext(SYSTEM_ID_MISMATCH, id), "systemId does not match processor id");
      }
      break;
    default:
      break;
    }

    Integer size = message.getKeySize();

    if (size != null && size > maxKeySize) {
      fail(getResponseContext(INVALID_KEY_SIZE, id), "Size " + size + " is greater than the maximum " + maxKeySize);
    }
  }

  /**
   * Process impl.
   *
   * @param message
   *          the message
   * @return the response
   * @throws Exception
   *           the exception
   */
  @Override
  protected Response processImpl(OtpKeyRequest message) throws Exception {
    switch (message.getKeyFunction()) {
    case DELETE:
      return deleteKey(message);
    case NEW:
      return newKey(message);
    default:
      return failure(getResponseContext(UNEXPECTED_ERROR, message.getOtpSystemId()),
          message,
          "Implementation error: " + message.getKeyFunction());
    }
  }

  private Response deleteKey(OtpMessage message) throws Exception {
    Long systemId = message.getOtpSystemId();
    log.debug("Deleting key for {}", systemId);

    registry.remove(systemId);

    return new Response(message, ResponseCode.SUCCESS);
  }

  private Response newKey(OtpKeyRequest message) throws Exception {
    Integer size = message.getKeySize() == null ? defaultKeySize : message.getKeySize();
    Long systemId = message.getOtpSystemId();

    log.debug("Creating new OTP key of size {} for {}", size, systemId);

    byte[] newKey = pad.generateKey(size);

    newKeyRegistry.put(systemId, newKey);

    return new Response(message, ResponseCode.SUCCESS, newKey);
  }

}

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
package com.github.mrstampy.gameboot.otp.processor;

import java.lang.invoke.MethodHandles;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.github.mrstampy.gameboot.messages.Response;
import com.github.mrstampy.gameboot.messages.Response.ResponseCode;
import com.github.mrstampy.gameboot.otp.OneTimePad;
import com.github.mrstampy.gameboot.otp.messages.OtpNewKeyRequest;
import com.github.mrstampy.gameboot.otp.netty.OtpClearNettyHandler;
import com.github.mrstampy.gameboot.otp.netty.OtpEncryptedNettyHandler;
import com.github.mrstampy.gameboot.otp.websocket.OtpClearWebSocketHandler;
import com.github.mrstampy.gameboot.otp.websocket.OtpEncryptedWebSocketHandler;
import com.github.mrstampy.gameboot.processor.AbstractGameBootProcessor;

/**
 * The Class OtpNewKeyRequestProcessor generates a key from a request sent on an
 * encrypted channel for encrypting data sent on a related clear channel. If the
 * {@link OtpNewKeyRequest#getSize()} has not been set a default size specified
 * by the GameBoot property 'otp.default.key.size' will be used. If set the
 * value must be > 0 and must be a multiple of 2. Key sizes must be >= all
 * message sizes sent in the unencrypted channel. The
 * {@link OtpNewKeyRequest#getSystemId()} value will be the value obtained from
 * the clear channel.
 * 
 * @see OtpClearNettyHandler
 * @see OtpEncryptedNettyHandler
 * @see OtpClearWebSocketHandler
 * @see OtpEncryptedWebSocketHandler
 */
@Component
public class OtpNewKeyRequestProcessor extends AbstractGameBootProcessor<OtpNewKeyRequest> {
  private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Autowired
  private OtpNewKeyRegistry newKeyRegistry;

  @Autowired
  private OneTimePad pad;

  @Value("${otp.default.key.size}")
  private Integer defaultKeySize;

  /*
   * (non-Javadoc)
   * 
   * @see com.github.mrstampy.gameboot.processor.GameBootProcessor#getType()
   */
  @Override
  public String getType() {
    return OtpNewKeyRequest.TYPE;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * com.github.mrstampy.gameboot.processor.AbstractGameBootProcessor#validate(
   * com.github.mrstampy.gameboot.messages.AbstractGameBootMessage)
   */
  @Override
  protected void validate(OtpNewKeyRequest message) throws Exception {
    Long systemId = message.getSystemId();
    if (systemId == null || systemId <= 0) fail("No systemId");

    Integer size = message.getSize();
    if (size != null) {
      if (size <= 0 || size % 2 != 0) fail("Invalid key size, expecting > 0 and multiples of 2");
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.github.mrstampy.gameboot.processor.AbstractGameBootProcessor#
   * processImpl(com.github.mrstampy.gameboot.messages.AbstractGameBootMessage)
   */
  @Override
  protected Response processImpl(OtpNewKeyRequest message) throws Exception {
    Integer size = message.getSize() == null ? defaultKeySize : message.getSize();
    Long systemId = message.getSystemId();

    log.debug("Creating new OTP key of size {} for {}", size, systemId);

    byte[] newKey = pad.generateKey(size);

    newKeyRegistry.put(systemId, newKey);

    return new Response(ResponseCode.SUCCESS, newKey);
  }

}

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
package com.github.mrstampy.gameboot.locale.processor;

import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;

import java.lang.invoke.MethodHandles;
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.github.mrstampy.gameboot.locale.messages.LocaleMessage;
import com.github.mrstampy.gameboot.messages.Response;
import com.github.mrstampy.gameboot.messages.Response.ResponseCode;
import com.github.mrstampy.gameboot.processor.AbstractGameBootProcessor;
import com.github.mrstampy.gameboot.systemid.SystemIdKey;

/**
 * The Class LocaleProcessor.
 */
@Component
@Profile(LocaleProcessor.PROFILE)
public class LocaleProcessor extends AbstractGameBootProcessor<LocaleMessage> {
  private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  /** The Constant PROFILE. */
  public static final String PROFILE = "locale";

  @Autowired
  private LocaleRegistry registry;

  /*
   * (non-Javadoc)
   * 
   * @see com.github.mrstampy.gameboot.processor.GameBootProcessor#getType()
   */
  @Override
  public String getType() {
    return LocaleMessage.TYPE;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * com.github.mrstampy.gameboot.processor.AbstractGameBootProcessor#validate(
   * com.github.mrstampy.gameboot.messages.AbstractGameBootMessage)
   */
  @Override
  protected void validate(LocaleMessage message) throws Exception {
    if (message == null) fail(getResponseContext(NO_MESSAGE), "No message");

    if (isEmpty(message.getLanguageCode())) {
      fail(getResponseContext(LANG_CODE_MISSING, message.getSystemId()), "Missing lang code");
    }

    if (message.getSystemId() == null) {
      fail(getResponseContext(NO_SYSTEM_ID, message.getSystemId()), "Missing system id");
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.github.mrstampy.gameboot.processor.AbstractGameBootProcessor#
   * processImpl(com.github.mrstampy.gameboot.messages.AbstractGameBootMessage)
   */
  @Override
  protected Response processImpl(LocaleMessage message) throws Exception {
    SystemIdKey systemId = message.getSystemId();

    Locale locale = null;
    if (isNotEmpty(message.getCountryCode())) {
      locale = new Locale(message.getLanguageCode(), message.getCountryCode());
    } else {
      locale = new Locale(message.getLanguageCode());
    }

    log.debug("Changing locale for system id {} to {}", systemId, locale);

    registry.put(systemId, locale);

    return new Response(message, ResponseCode.SUCCESS);
  }

}

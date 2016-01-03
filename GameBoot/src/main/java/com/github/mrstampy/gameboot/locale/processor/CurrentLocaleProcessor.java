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
package com.github.mrstampy.gameboot.locale.processor;

import java.util.Locale;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.github.mrstampy.gameboot.locale.messages.CurrentLocaleMessage;
import com.github.mrstampy.gameboot.messages.Response;
import com.github.mrstampy.gameboot.messages.Response.ResponseCode;
import com.github.mrstampy.gameboot.processor.AbstractGameBootProcessor;

/**
 * Returns the current locale for a connection.
 */
@Component
public class CurrentLocaleProcessor extends AbstractGameBootProcessor<CurrentLocaleMessage> {

  @Autowired
  private LocaleRegistry registry;

  /*
   * (non-Javadoc)
   * 
   * @see com.github.mrstampy.gameboot.processor.GameBootProcessor#getType()
   */
  @Override
  public String getType() {
    return CurrentLocaleMessage.TYPE;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * com.github.mrstampy.gameboot.processor.AbstractGameBootProcessor#validate(
   * com.github.mrstampy.gameboot.messages.AbstractGameBootMessage)
   */
  @Override
  protected void validate(CurrentLocaleMessage message) throws Exception {
    if (message == null) throw new NullPointerException("No message");
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.github.mrstampy.gameboot.processor.AbstractGameBootProcessor#
   * processImpl(com.github.mrstampy.gameboot.messages.AbstractGameBootMessage)
   */
  @Override
  protected Response processImpl(CurrentLocaleMessage message) throws Exception {
    return new Response(ResponseCode.SUCCESS, new LocaleBean(registry.get(message.getSystemId())));
  }

  /**
   * Bean to represent the locale as JSON.
   */
  public static final class LocaleBean {

    private String languageCode;
    private String countryCode;

    /**
     * Instantiates a new locale bean.
     */
    public LocaleBean() {
    }

    /**
     * Instantiates a new locale bean.
     *
     * @param locale
     *          the locale
     */
    public LocaleBean(Locale locale) {
      setLanguageCode(locale.getLanguage());
      setCountryCode(locale.getCountry());
    }

    /**
     * Gets the language code.
     *
     * @return the language code
     */
    public String getLanguageCode() {
      return languageCode;
    }

    /**
     * Sets the language code.
     *
     * @param languageCode
     *          the new language code
     */
    public void setLanguageCode(String languageCode) {
      this.languageCode = languageCode;
    }

    /**
     * Gets the country code.
     *
     * @return the country code
     */
    public String getCountryCode() {
      return countryCode;
    }

    /**
     * Sets the country code.
     *
     * @param countryCode
     *          the new country code
     */
    public void setCountryCode(String countryCode) {
      this.countryCode = countryCode;
    }
  }

}

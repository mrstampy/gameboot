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
package com.github.mrstampy.gameboot.locale.messages;

import java.util.Locale;
import java.util.ResourceBundle;

import com.github.mrstampy.gameboot.messages.AbstractGameBootMessage;
import com.github.mrstampy.gameboot.messages.context.ResponseContextLookup;

/**
 * The Class LocaleMessage is used to set the {@link Locale} for a client for
 * purpose of {@link ResponseContextLookup}s and other {@link Locale}-specific
 * functions.
 */
public class LocaleMessage extends AbstractGameBootMessage {

  /** The Constant TYPE. */
  public static final String TYPE = "LocaleMessage";

  private String languageCode;

  private String countryCode;

  /**
   * Instantiates a new locale message.
   */
  public LocaleMessage() {
    super(TYPE);
  }

  /**
   * Gets the language code ie. 'en', 'fr', 'es'
   *
   * @return the language code
   * @see ResourceBundle
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
   * Gets the country code ie. 'CA', 'AU', 'DK'
   *
   * @return the country code
   * @see ResourceBundle
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

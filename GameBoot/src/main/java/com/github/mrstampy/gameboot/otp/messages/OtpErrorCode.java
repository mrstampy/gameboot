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
package com.github.mrstampy.gameboot.otp.messages;

/**
 * The Enum OtpErrorCode.
 */
public enum OtpErrorCode {
  //@formatter:off
  // when the systemId has not been set
  /** The no system id. */
  NO_SYSTEM_ID(-1, "No systemId"),
  // powers of 2
  /** The KE y_ power s_ o f_2. */
  KEY_POWERS_OF_2(-2, "Key size must be a power of 2"),
  // when the supplied systemId does not match the systemId of the channel
  /** The system id mismatch. */
  SYSTEM_ID_MISMATCH(-3, "Processor id mismatch"),
  // must be one of NEW, DELETE
  /** The invalid key function. */
  INVALID_KEY_FUNCTION(-4, "Invalid function"),
  // no key to activate (expired?)
  /** The no key. */
  NO_KEY(-5, "No key to activate"),
  // unspecified
  /** The server error. */
  SERVER_ERROR(-99, "Unspecified error")
  ;
  //@formatter:on

  /** The code. */
  int code;

  /** The description. */
  String description;

  /**
   * Instantiates a new otp error code.
   *
   * @param code
   *          the code
   * @param description
   *          the description
   */
  OtpErrorCode(int code, String description) {
    this.code = code;
    this.description = description;
  }

  /**
   * Gets the code.
   *
   * @return the code
   */
  public int getCode() {
    return code;
  }

  /**
   * Gets the description.
   *
   * @return the description
   */
  public String getDescription() {
    return description;
  }

}

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
package com.github.mrstampy.gameboot.messages.error;

/**
 * The Interface ErrorCodes.
 */
public interface ErrorCodes {

  /** The Constant UNEXPECTED_ERROR. */
  public static final Integer UNEXPECTED_ERROR = -99;

  /** The Constant NO_MESSAGE. */
  public static final Integer NO_MESSAGE = -98;

  /** The Constant NO_TYPE. */
  public static final Integer NO_TYPE = -97;

  /** The Constant UNKNOWN_MESSAGE. */
  public static final Integer UNKNOWN_MESSAGE = -96;

  /** The Constant INVALID_KEY_FUNCTION. */
  public static final Integer INVALID_KEY_FUNCTION = -95;

  /** The Constant NO_SYSTEM_ID. */
  public static final Integer NO_SYSTEM_ID = -94;

  /** The Constant SYSTEM_ID_MISMATCH. */
  public static final Integer SYSTEM_ID_MISMATCH = -93;

  /** The Constant INVALID_KEY_SIZE. */
  public static final Integer INVALID_KEY_SIZE = -92;

  /** The Constant NEW_KEY_ACTIVATION_FAIL. */
  public static final Integer NEW_KEY_ACTIVATION_FAIL = -91;

  /** The Constant USER_SESSION_EXISTS. */
  public static final Integer USER_SESSION_EXISTS = -90;

  /** The Constant NO_USER_RECORD. */
  public static final Integer NO_USER_RECORD = -89;

  /** The Constant NO_USER_SESSION. */
  public static final Integer NO_USER_SESSION = -88;

  /** The Constant INVALID_SESSION_ID. */
  public static final Integer INVALID_SESSION_ID = -87;

  /** The Constant NO_USERNAME. */
  public static final Integer NO_USERNAME = -86;

  /** The Constant INVALID_USER_FUNCTION. */
  public static final Integer INVALID_USER_FUNCTION = -85;

  /** The Constant OLD_PASSWORD_MISSING. */
  public static final Integer OLD_PASSWORD_MISSING = -84;

  /** The Constant NEW_PASSWORD_MISSING. */
  public static final Integer NEW_PASSWORD_MISSING = -83;

  /** The Constant NO_USER_DATA. */
  public static final Integer NO_USER_DATA = -82;

  /** The Constant CANNOT_DELETE_USER. */
  public static final Integer CANNOT_DELETE_USER = -81;

  /** The Constant INVALID_PASSWORD. */
  public static final Integer INVALID_PASSWORD = -80;

  /** The Constant USER_UNCHANGED. */
  public static final Integer USER_UNCHANGED = -79;
}

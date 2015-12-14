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
 * The Class OtpKeyRequest.
 */
public class OtpKeyRequest extends OtpMessage {

  /** The Constant TYPE. */
  public static final String TYPE = "OtpKeyRequest";

  private Integer size;

  /**
   * The Enum KeyFunction.
   */
  public enum KeyFunction {

    /** The new. */
    NEW,
    /** The delete. */
    DELETE;
  }

  private KeyFunction keyFunction;

  /**
   * Instantiates a new otp new key request.
   */
  public OtpKeyRequest() {
    super(TYPE);
  }

  /**
   * Gets the size.
   *
   * @return the size
   */
  public Integer getSize() {
    return size;
  }

  /**
   * Sets the size.
   *
   * @param size
   *          the new size
   */
  public void setSize(Integer size) {
    this.size = size;
  }

  /**
   * Gets the key function.
   *
   * @return the key function
   */
  public KeyFunction getKeyFunction() {
    return keyFunction;
  }

  /**
   * Sets the key function.
   *
   * @param keyFunction
   *          the new key function
   */
  public void setKeyFunction(KeyFunction keyFunction) {
    this.keyFunction = keyFunction;
  }

}

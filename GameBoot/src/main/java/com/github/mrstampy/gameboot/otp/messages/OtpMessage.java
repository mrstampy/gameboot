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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.github.mrstampy.gameboot.messages.AbstractGameBootMessage;
import com.github.mrstampy.gameboot.otp.processor.OtpKeyRequestProcessor;

/**
 * The Class OtpMessage.
 */
public abstract class OtpMessage extends AbstractGameBootMessage {

  private Long processorKey;

  /**
   * Instantiates a new otp message.
   *
   * @param type
   *          the type
   */
  protected OtpMessage(String type) {
    super(type);
  }

  /**
   * Gets the processor key.
   *
   * @return the processor key
   */
  @JsonIgnore
  public Long getProcessorKey() {
    return processorKey;
  }

  /**
   * Sets the processor key by the system prior to submission to the
   * {@link OtpKeyRequestProcessor}. Will fail if not set correctly.
   *
   * @param processorKey
   *          the new processor key
   */
  public void setProcessorKey(Long processorKey) {
    this.processorKey = processorKey;
  }

}

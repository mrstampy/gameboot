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

import com.github.mrstampy.gameboot.SystemId;
import com.github.mrstampy.gameboot.otp.netty.OtpClearNettyHandler;
import com.github.mrstampy.gameboot.otp.websocket.OtpClearWebSocketHandler;
import com.github.mrstampy.gameboot.systemid.processor.SystemIdMessageProcessor;

/**
 * The Class SystemIdResponse is used to encapsulate the {@link SystemId} value
 * for the connection, sent to the client.
 * 
 * @see SystemIdMessageProcessor
 * @see OtpClearNettyHandler
 * @see OtpClearWebSocketHandler
 */
public class SystemIdResponse {

  private Long systemId;

  /**
   * Instantiates a new otp system id.
   */
  public SystemIdResponse() {
  }

  /**
   * Instantiates a new otp system id.
   *
   * @param systemId
   *          the system id
   */
  public SystemIdResponse(Long systemId) {
    setSystemId(systemId);
  }

  /**
   * Gets the system id.
   *
   * @return the system id
   */
  public Long getSystemId() {
    return systemId;
  }

  /**
   * Sets the system id.
   *
   * @param systemId
   *          the new system id
   */
  public void setSystemId(Long systemId) {
    this.systemId = systemId;
  }
}

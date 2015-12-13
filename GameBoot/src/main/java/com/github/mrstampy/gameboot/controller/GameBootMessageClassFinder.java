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
package com.github.mrstampy.gameboot.controller;

import java.lang.invoke.MethodHandles;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.mrstampy.gameboot.otp.messages.OtpKeyRequest;
import com.github.mrstampy.gameboot.otp.messages.OtpNewKeyAck;
import com.github.mrstampy.gameboot.usersession.messages.UserMessage;

/**
 * The default implementation of the {@link MessageClassFinder} interface.
 * 
 * @see GameBootControllerConfiguration
 */
public class GameBootMessageClassFinder implements MessageClassFinder {
  private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  /*
   * (non-Javadoc)
   * 
   * @see
   * com.github.mrstampy.gameboot.controller.MessageClassFinder#findClass(java.
   * lang.String)
   */
  @Override
  public Class<?> findClass(String type) {
    switch (type) {
    case UserMessage.TYPE:
      return UserMessage.class;
    case OtpKeyRequest.TYPE:
      return OtpKeyRequest.class;
    case OtpNewKeyAck.TYPE:
      return OtpNewKeyAck.class;
    default:
      log.error("No class defined for type {}", type);
      return null;
    }
  }

}

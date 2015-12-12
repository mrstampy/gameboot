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
package com.github.mrstampy.gameboot.otp.netty;

import com.github.mrstampy.gameboot.otp.KeyRegistry;

import io.netty.channel.Channel;

/**
 * The Class OtpConnections.
 */
public class OtpNettyConnections {

  private Channel encryptedChannel;

  private Channel clearChannel;

  /**
   * Gets the encrypted channel.
   *
   * @return the encrypted channel
   */
  public Channel getEncryptedChannel() {
    return encryptedChannel;
  }

  /**
   * Sets the encrypted channel.
   *
   * @param encryptedChannel
   *          the new encrypted channel
   */
  public void setEncryptedChannel(Channel encryptedChannel) {
    this.encryptedChannel = encryptedChannel;
  }

  /**
   * Gets the clear channel.
   *
   * @return the clear channel
   */
  public Channel getClearChannel() {
    return clearChannel;
  }

  /**
   * Sets the clear channel.
   *
   * @param clearChannel
   *          the new clear channel
   */
  public void setClearChannel(Channel clearChannel) {
    this.clearChannel = clearChannel;
  }

  /**
   * Checks if both {@link #isClearChannelActive()} &&
   * {@link #isEncryptedChannelActive()}.
   *
   * @return true, if is active
   */
  public boolean isActive() {
    return isClearChannelActive() && isEncryptedChannelActive();
  }

  /**
   * Checks if the clear channel is active.
   *
   * @return true, if is clear channel active
   */
  public boolean isClearChannelActive() {
    return clearChannel != null && clearChannel.isActive();
  }

  /**
   * Checks if the encrypted channel is active.
   *
   * @return true, if is encrypted channel active
   */
  public boolean isEncryptedChannelActive() {
    return encryptedChannel != null && encryptedChannel.isActive();
  }

  /**
   * Creates the otp key used by an {@link OtpNettyHandler} instance to check
   * the {@link KeyRegistry} for the OTP key and in the
   * {@link OtpNettyRegistry#setClearChannel(Comparable, Channel)} via
   * {@link #getClearChannel()#remoteAddress()#toString()} or null if not
   * {@link #isClearChannelActive()}.
   *
   * @return the otp key
   * @see OtpNettyHandler
   */
  public String createOtpKey() {
    if (!isClearChannelActive()) return null;

    return getClearChannel().remoteAddress().toString();
  }

}

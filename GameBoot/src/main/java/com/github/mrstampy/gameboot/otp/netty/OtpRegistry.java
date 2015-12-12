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

import static org.apache.commons.lang3.StringUtils.isEmpty;

import java.lang.invoke.MethodHandles;
import java.net.SocketAddress;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.github.mrstampy.gameboot.util.GameBootRegistry;
import com.github.mrstampy.gameboot.util.netty.NettyUtils;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.ssl.SslHandler;

/**
 * The Class OtpRegistry keeps an {@link OtpConnections} pairing to be able to
 * send OTP keys thru the {@link OtpConnections#getEncryptedChannel()} and
 * OTP-encrypted messages in the {@link OtpConnections#getClearChannel()}.
 */
@Component
public class OtpRegistry extends GameBootRegistry<OtpConnections> {

  private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Autowired
  private NettyUtils utils;

  /**
   * Sets the clear channel.
   *
   * @param key
   *          the key
   * @param channel
   *          the channel
   */
  public void setClearChannel(Comparable<?> key, Channel channel) {
    OtpConnections connections = getContainer(key, channel);

    connections.setClearChannel(channel);

    channel.closeFuture().addListener(f -> evaluate(key));
  }

  /**
   * Sets the encrypted channel.
   *
   * @param key
   *          the key
   * @param channel
   *          the channel
   */
  public void setEncryptedChannel(Comparable<?> key, Channel channel) {
    encryptedChannelCheck(channel);
    OtpConnections connections = getContainer(key, channel);

    connections.setEncryptedChannel(channel);

    channel.closeFuture().addListener(f -> evaluate(key));
  }

  /**
   * Send new otp key.
   *
   * @param key
   *          the key
   * @param otpKey
   *          the otp key
   * @param listeners
   *          the listeners
   */
  public void sendNewOtpKey(Comparable<?> key, byte[] otpKey, ChannelFutureListener... listeners) {
    if (otpKey == null || otpKey.length == 0) fail("No OTP key");

    OtpConnections connections = getOrLog(key);
    if (connections == null) return;

    Channel encryptedChannel = connections.getEncryptedChannel();
    if (!isActive(encryptedChannel)) {
      log.warn("No encrypted channel, cannot send new OTP key for {}", key);
      return;
    }

    ChannelFutureListener[] all = utils.addToArray(f -> evaluateNewOtpKeySend(f), listeners);

    send(otpKey, encryptedChannel, all);
  }

  /**
   * Send clear message.
   *
   * @param key
   *          the key
   * @param message
   *          the message
   * @param listeners
   *          the listeners
   */
  public void sendClearMessage(Comparable<?> key, String message, ChannelFutureListener... listeners) {
    if (isEmpty(message)) fail("No message");

    sendClearImpl(key, message, listeners);
  }

  /**
   * Send clear message.
   *
   * @param key
   *          the key
   * @param message
   *          the message
   * @param listeners
   *          the listeners
   */
  public void sendClearMessage(Comparable<?> key, byte[] message, ChannelFutureListener... listeners) {
    if (message == null || message.length == 0) fail("No message");

    sendClearImpl(key, message, listeners);
  }

  /**
   * Checks if is active.
   *
   * @param key
   *          the key
   * @return true, if is active
   */
  public boolean isActive(Comparable<?> key) {
    checkKey(key);

    OtpConnections connections = get(key);

    return connections == null ? false : isActive(connections);
  }

  private <T> void sendClearImpl(Comparable<?> key, T message, ChannelFutureListener... listeners) {
    OtpConnections connections = getOrLog(key);
    if (connections == null) return;

    Channel clearChannel = connections.getClearChannel();
    if (!isActive(clearChannel)) {
      log.warn("No clear channel, cannot send message for {}", key);
      return;
    }

    ChannelFutureListener[] all = utils.addToArray(f -> evaluateMessageSend(f), listeners);

    send(message, connections.getClearChannel(), all);
  }

  private <T> void send(T message, Channel channel, ChannelFutureListener... listeners) {
    checkChannel(channel);
    ChannelFuture cf = channel.writeAndFlush(message);
    if (listeners != null) {
      for (ChannelFutureListener cfl : listeners) {
        cf.addListener(cfl);
      }
    }
  }

  private void encryptedChannelCheck(Channel channel) {
    checkChannel(channel);

    ChannelPipeline pipeline = channel.pipeline();

    SslHandler handler = pipeline.get(SslHandler.class);

    if (handler == null) fail("Not an encrypted channel");
  }

  private void evaluateNewOtpKeySend(ChannelFuture f) {
    SocketAddress remoteAddress = f.channel().remoteAddress();
    if (f.isSuccess()) {
      log.debug("Successful send of new OTP key to {}", remoteAddress);
    } else {
      log.error("Could not send new OTP key to {}", remoteAddress, f.cause());
    }
  }

  private void evaluateMessageSend(ChannelFuture f) {
    SocketAddress remoteAddress = f.channel().remoteAddress();
    if (f.isSuccess()) {
      log.debug("Successful send of message to {}", remoteAddress);
    } else {
      log.error("Could not send message to {}", remoteAddress, f.cause());
    }
  }

  private OtpConnections getOrLog(Comparable<?> key) {
    checkKey(key);
    OtpConnections connections = get(key);
    if (connections == null) {
      log.warn("No OTP connections for {}", key);
    }
    return connections;
  }

  private OtpConnections getContainer(Comparable<?> key, Channel channel) {
    checkKey(key);
    checkChannel(channel);

    OtpConnections connections = getOrInit(key);

    return connections;
  }

  private void evaluate(Comparable<?> key) {
    OtpConnections connections = get(key);
    if (connections == null) return;

    if (isActive(connections)) return;

    remove(key);
  }

  private boolean isActive(OtpConnections connections) {
    return isActive(connections.getClearChannel()) && isActive(connections.getEncryptedChannel());
  }

  private boolean isActive(Channel c) {
    return c != null && c.isActive();
  }

  private OtpConnections getOrInit(Comparable<?> key) {
    OtpConnections connections = get(key);
    if (connections == null) {
      connections = new OtpConnections();
      put(key, connections);
    }
    return connections;
  }

  private void checkChannel(Channel channel) {
    if (channel == null || !channel.isActive()) fail("No channel");
  }
}

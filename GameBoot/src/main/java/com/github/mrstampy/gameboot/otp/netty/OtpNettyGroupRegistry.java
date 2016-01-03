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
package com.github.mrstampy.gameboot.otp.netty;

import static org.apache.commons.lang3.StringUtils.isEmpty;

import java.lang.invoke.MethodHandles;
import java.util.Arrays;
import java.util.Map.Entry;
import java.util.Optional;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.github.mrstampy.gameboot.metrics.MetricsHelper;
import com.github.mrstampy.gameboot.netty.NettyConnectionRegistry;
import com.github.mrstampy.gameboot.otp.KeyRegistry;
import com.github.mrstampy.gameboot.otp.OneTimePad;
import com.github.mrstampy.gameboot.otp.OtpConfiguration;
import com.github.mrstampy.gameboot.systemid.SystemIdKey;
import com.github.mrstampy.gameboot.util.registry.AbstractRegistryKey;
import com.github.mrstampy.gameboot.util.registry.GameBootRegistry;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.group.ChannelGroup;

/**
 * The Class OtpNettyGroupRegistry facilitates communication between
 * {@link OtpClearNettyProcessor} connections, encrypting the message for each
 * connection as required.
 * 
 * @see NettyConnectionRegistry
 */
@Component
@Profile(OtpConfiguration.OTP_PROFILE)
public class OtpNettyGroupRegistry extends GameBootRegistry<Channel> {
  private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private static final String OTP_NETTY_CONNECTIONS = "OTP Netty Connections";

  @Autowired
  private NettyConnectionRegistry registry;

  @Autowired
  private KeyRegistry keyRegistry;

  @Autowired
  private OneTimePad pad;

  @Autowired
  private MetricsHelper helper;

  /**
   * Post construct.
   *
   * @throws Exception
   *           the exception
   */
  @PostConstruct
  public void postConstruct() throws Exception {
    helper.gauge(() -> size(), OTP_NETTY_CONNECTIONS, getClass(), "netty", "connections");
  }

  /**
   * Put.
   *
   * @param key
   *          the key
   * @param channel
   *          the channel
   */
  public void put(AbstractRegistryKey<?> key, Channel channel) {
    super.put(key, channel);
    channel.closeFuture().addListener(f -> map.remove(key));
  }

  /**
   * Put in group, delegating to the {@link NettyConnectionRegistry}.
   *
   * @param groupName
   *          the group name
   * @param channel
   *          the channel
   */
  public void putInGroup(String groupName, Channel channel) {
    registry.putInGroup(groupName, channel);
  }

  /**
   * Send.
   *
   * @param key
   *          the key
   * @param message
   *          the message
   * @param listeners
   *          the listeners
   * @throws Exception
   *           the exception
   */
  public void send(AbstractRegistryKey<?> key, byte[] message, ChannelFutureListener... listeners) throws Exception {
    SystemIdKey systemId = (key instanceof SystemIdKey) ? (SystemIdKey) key : getKeyForChannel(registry.get(key));

    byte[] otp = systemId == null ? null : keyRegistry.get(systemId);

    if (otp == null) {
      registry.send(key, Arrays.copyOf(message, message.length), listeners);
    } else {
      byte[] converted = pad.convert(otp, message);
      registry.send(key, converted, listeners);
    }
  }

  /**
   * Send to group.
   *
   * @param groupName
   *          the group name
   * @param message
   *          the message
   * @param listeners
   *          the listeners
   */
  public void sendToGroup(String groupName, byte[] message, ChannelFutureListener... listeners) {
    ChannelGroup group = getGroup(groupName);

    if (group == null) return;

    group.forEach(c -> {
      try {
        send(getKeyForChannel(c), message, listeners);
      } catch (Exception e) {
        log.error("Unexpected exception sending message to {}", c, e);
      }
    });
  }

  /**
   * Send to group.
   *
   * @param groupName
   *          the group name
   * @param message
   *          the message
   * @param except
   *          the except
   */
  public void sendToGroup(String groupName, byte[] message, SystemIdKey... except) {
    ChannelGroup group = getGroup(groupName);

    if (group == null) return;

    group.forEach(c -> {
      try {
        AbstractRegistryKey<?> systemId = getKeyForChannel(c);
        if (!excepted(systemId, except)) send(systemId, message);
      } catch (Exception e) {
        log.error("Unexpected exception sending message to {}", c, e);
      }
    });

  }

  /**
   * Send.
   *
   * @param key
   *          the key
   * @param message
   *          the message
   * @param listeners
   *          the listeners
   * @throws Exception
   *           the exception
   */
  public void send(AbstractRegistryKey<?> key, String message, ChannelFutureListener... listeners) throws Exception {
    if (!messageCheck(message)) return;

    send(key, message.getBytes(), listeners);
  }

  /**
   * Send to group.
   *
   * @param groupName
   *          the group name
   * @param message
   *          the message
   * @param except
   *          the except
   */
  public void sendToGroup(String groupName, String message, SystemIdKey... except) {
    if (!messageCheck(message)) return;

    sendToGroup(groupName, message.getBytes(), except);
  }

  /**
   * Send to group.
   *
   * @param groupKey
   *          the group key
   * @param message
   *          the message
   * @param listeners
   *          the listeners
   */
  public void sendToGroup(String groupKey, String message, ChannelFutureListener... listeners) {
    if (!messageCheck(message)) return;

    sendToGroup(groupKey, message.getBytes(), listeners);
  }

  private ChannelGroup getGroup(String groupKey) {
    ChannelGroup group = registry.getGroup(groupKey);

    if (group == null) {
      log.warn("No group {}, cannot send message", groupKey);
      return null;
    }

    return group;
  }

  private boolean messageCheck(String message) {
    if (isEmpty(message)) {
      log.warn("No message");
      return false;
    }

    return true;
  }

  private boolean excepted(AbstractRegistryKey<?> systemId, SystemIdKey[] except) {
    if (except == null || except.length == 0) return false;

    for (SystemIdKey key : except) {
      if (key.equals(systemId)) return true;
    }

    return false;
  }

  private SystemIdKey getKeyForChannel(Channel c) {
    Optional<Entry<AbstractRegistryKey<?>, Channel>> o = registry.getKeysForValue(c).stream()
        .filter(e -> e.getKey() instanceof SystemIdKey).findFirst();

    return o.isPresent() ? (SystemIdKey) o.get().getKey() : null;
  }
}

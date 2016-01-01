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
package com.github.mrstampy.gameboot.messages;

import java.net.InetSocketAddress;

import org.apache.commons.lang3.builder.ToStringBuilder;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.github.mrstampy.gameboot.netty.AbstractNettyProcessor;
import com.github.mrstampy.gameboot.processor.GameBootProcessor;
import com.github.mrstampy.gameboot.systemid.SystemId;
import com.github.mrstampy.gameboot.systemid.SystemIdKey;
import com.github.mrstampy.gameboot.util.registry.GameBootRegistry;
import com.github.mrstampy.gameboot.web.WebProcessor;
import com.github.mrstampy.gameboot.websocket.AbstractWebSocketProcessor;

/**
 * The superclass for all GameBoot JSON messages.
 */
@JsonInclude(Include.NON_NULL)
public abstract class AbstractGameBootMessage {

  private Integer id;

  private String type;

  private SystemIdKey systemId;

  /**
   * The Enum Transport, used to indicate to the {@link GameBootProcessor} how
   * the connection to the client is managed.
   */
  public enum Transport {

    /** Indicates that the message was received over the web. */
    WEB,
    /** Indicates that the message was received over a websocket. */
    WEB_SOCKET,
    /** Indicates that the message was received over Netty */
    NETTY;
  }

  private Transport transport = Transport.WEB;

  private InetSocketAddress local;

  private InetSocketAddress remote;

  /**
   * Instantiates a new abstract game boot message.
   *
   * @param type
   *          the type
   */
  protected AbstractGameBootMessage(String type) {
    setType(type);
  }

  /**
   * Gets the id.
   *
   * @return the id
   */
  public Integer getId() {
    return id;
  }

  /**
   * Sets the id.
   *
   * @param id
   *          the new id
   */
  public void setId(Integer id) {
    this.id = id;
  }

  /**
   * Gets the type.
   *
   * @return the type
   */
  public String getType() {
    return type;
  }

  /**
   * Sets the type.
   *
   * @param type
   *          the new type
   */
  public void setType(String type) {
    this.type = type;
  }

  /*
   * (non-Javadoc)
   * 
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    return ToStringBuilder.reflectionToString(this);
  }

  /**
   * Transient value set by the GameBoot implementation prior to processing by
   * its {@link GameBootProcessor}, can be used by the {@link GameBootProcessor}
   * to get and set transient objects related to the connected client in the
   * various {@link GameBootRegistry}s.
   *
   * @return the system session id
   * @see SystemId
   */
  @JsonIgnore
  public SystemIdKey getSystemId() {
    return systemId;
  }

  /**
   * Sets the system session id.
   *
   * @param systemSessionId
   *          the new system session id
   */
  public void setSystemId(SystemIdKey systemSessionId) {
    this.systemId = systemSessionId;
  }

  /**
   * This transient value is set by the GameBoot implementation prior to
   * processing by its {@link GameBootProcessor}.
   *
   * @return the transport
   * @see AbstractNettyProcessor#process(io.netty.channel.ChannelHandlerContext,
   *      com.github.mrstampy.gameboot.controller.GameBootMessageController,
   *      AbstractGameBootMessage)
   * @see AbstractWebSocketProcessor#process(org.springframework.web.socket.WebSocketSession,
   *      com.github.mrstampy.gameboot.controller.GameBootMessageController,
   *      AbstractGameBootMessage)
   * @see WebProcessor#process(javax.servlet.http.HttpSession,
   *      com.github.mrstampy.gameboot.controller.GameBootMessageController,
   *      AbstractGameBootMessage)
   */
  @JsonIgnore
  public Transport getTransport() {
    return transport;
  }

  /**
   * Sets the transport.
   *
   * @param transport
   *          the new transport
   */
  public void setTransport(Transport transport) {
    this.transport = transport;
  }

  /**
   * Gets the local.
   *
   * @return the local
   */
  @JsonIgnore
  public InetSocketAddress getLocal() {
    return local;
  }

  /**
   * Sets the local.
   *
   * @param local
   *          the new local
   */
  public void setLocal(InetSocketAddress local) {
    this.local = local;
  }

  /**
   * Gets the remote.
   *
   * @return the remote
   */
  @JsonIgnore
  public InetSocketAddress getRemote() {
    return remote;
  }

  /**
   * Sets the remote.
   *
   * @param remote
   *          the new remote
   */
  public void setRemote(InetSocketAddress remote) {
    this.remote = remote;
  }

}

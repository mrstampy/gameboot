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
package com.github.mrstampy.gameboot.otp.websocket;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.server.HandshakeHandler;
import org.springframework.web.socket.server.standard.ServletServerContainerFactoryBean;
import org.springframework.web.socket.server.standard.TomcatRequestUpgradeStrategy;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;
import org.springframework.web.socket.server.support.HttpSessionHandshakeInterceptor;
import org.springframework.web.socket.sockjs.transport.handler.WebSocketTransportHandler;

/**
 * The Class OtpWebSocketTestConfiguration.
 */
@Configuration
@PropertySource("/test.properties")
public class OtpWebSocketTestConfiguration implements WebSocketConfigurer {

  /** The Constant CLIENT_ENCRYPTED_BOOTSTRAP. */
  public static final String CLIENT_ENCRYPTED_BOOTSTRAP = "CLIENT_ENCRYPTED_BOOTSTRAP";

  /** The Constant CLIENT_CLEAR_BOOTSTRAP. */
  public static final String CLIENT_CLEAR_BOOTSTRAP = "CLIENT_CLEAR_BOOTSTRAP";

  @Value("${ws.path}")
  private String clrPath;

  @Value("${wss.path}")
  private String encPath;

  @Autowired
  private OtpClearWebSocketHandler clearHandler;

  @Autowired
  private OtpEncryptedWebSocketHandler encHandler;

  /**
   * Initer.
   *
   * @return the web socket test initializer
   */
  @Bean
  public WebSocketTestInitializer initer() {
    return new WebSocketTestInitializer();
  }

  @Bean
  public ServletServerContainerFactoryBean createWebSocketContainer() {
    ServletServerContainerFactoryBean container = new ServletServerContainerFactoryBean();
    container.setMaxTextMessageBufferSize(8192);
    container.setMaxBinaryMessageBufferSize(8192);

    return container;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.springframework.web.socket.config.annotation.WebSocketConfigurer#
   * registerWebSocketHandlers(org.springframework.web.socket.config.annotation.
   * WebSocketHandlerRegistry)
   */
  @Override
  public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
    //@formatter:off
    registry
      .addHandler(clearHandler, clrPath)
      .addHandler(encHandler, encPath)
      .addInterceptors(new HttpSessionHandshakeInterceptor())
      .setHandshakeHandler(createHandshakeHandler());
    //@formatter:on
  }

  /**
   * Creates the handshake handler.
   *
   * @return the handshake handler
   */
  @Bean
  public HandshakeHandler createHandshakeHandler() {
    return new WebSocketTransportHandler(new DefaultHandshakeHandler(new TomcatRequestUpgradeStrategy()));
  }

  /**
   * Encrypted handler.
   *
   * @return the otp encrypted web socket handler
   */
  @Bean
  public OtpEncryptedWebSocketHandler encryptedHandler() {
    return new OtpEncryptedWebSocketHandler();
  }

  /**
   * Clear handler.
   *
   * @return the otp clear web socket handler
   */
  @Bean
  public OtpClearWebSocketHandler clearHandler() {
    return new OtpClearWebSocketHandler();
  }

}

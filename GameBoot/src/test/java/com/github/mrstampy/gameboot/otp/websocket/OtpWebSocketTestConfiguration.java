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

import java.util.Arrays;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.server.HandshakeHandler;
import org.springframework.web.socket.server.standard.TomcatRequestUpgradeStrategy;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;
import org.springframework.web.socket.server.support.HttpSessionHandshakeInterceptor;
import org.springframework.web.socket.server.support.WebSocketHttpRequestHandler;
import org.springframework.web.socket.sockjs.transport.handler.WebSocketTransportHandler;

import com.github.mrstampy.gameboot.otp.netty.client.ClientHandler;
import com.github.mrstampy.gameboot.otp.websocket.client.ClearClientInitializer;
import com.github.mrstampy.gameboot.otp.websocket.client.EncryptedClientInitializer;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;

/**
 * The Class OtpWebSocketTestConfiguration.
 */
@Configuration
@PropertySource("/test.properties")
@EnableWebSocket
public class OtpWebSocketTestConfiguration implements WebSocketConfigurer {

  /** The Constant CLIENT_ENCRYPTED_BOOTSTRAP. */
  public static final String CLIENT_ENCRYPTED_BOOTSTRAP = "CLIENT_ENCRYPTED_BOOTSTRAP";

  /** The Constant CLIENT_CLEAR_BOOTSTRAP. */
  public static final String CLIENT_CLEAR_BOOTSTRAP = "CLIENT_CLEAR_BOOTSTRAP";

  /** The Constant ENCRYPTED_WS. */
  public static final String ENCRYPTED_WS = "Encrypted ws";

  /** The Constant CLEAR_WS. */
  public static final String CLEAR_WS = "Clear ws";

  @Value("${ws.path}")
  private String clrPath;

  @Value("${wss.path}")
  private String encPath;

  /**
   * Initer.
   *
   * @return the web socket test initializer
   */
  @Bean
  public WebSocketTestInitializer initer() {
    return new WebSocketTestInitializer();
  }
  //
  // @Bean
  // public ServletServerContainerFactoryBean createWebSocketContainer() {
  // ServletServerContainerFactoryBean container = new
  // ServletServerContainerFactoryBean();
  // container.setMaxTextMessageBufferSize(8192);
  // container.setMaxBinaryMessageBufferSize(8192);
  //
  // return container;
  // }

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
      .addHandler(clearHandler(), clrPath)
      .addHandler(encryptedHandler(), encPath)
      .addInterceptors(new HttpSessionHandshakeInterceptor())
      .setHandshakeHandler(createHandshakeHandler());
    //@formatter:on
  }

  /**
   * Encrypted.
   *
   * @return the web socket http request handler
   */
  @Bean(name = ENCRYPTED_WS)
  public WebSocketHttpRequestHandler encrypted() {
    WebSocketHttpRequestHandler ws = new WebSocketHttpRequestHandler(encryptedHandler(), createHandshakeHandler());

    ws.setHandshakeInterceptors(Arrays.asList(new HttpSessionHandshakeInterceptor()));

    return ws;
  }

  /**
   * Clear.
   *
   * @return the web socket http request handler
   */
  @Bean(name = CLEAR_WS)
  public WebSocketHttpRequestHandler clear() {
    WebSocketHttpRequestHandler ws = new WebSocketHttpRequestHandler(clearHandler(), createHandshakeHandler());

    ws.setHandshakeInterceptors(Arrays.asList(new HttpSessionHandshakeInterceptor()));

    return ws;
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

  /**
   * Encrypted client bootstrap.
   *
   * @return the bootstrap
   * @throws Exception
   *           the exception
   */
  @Bean(name = CLIENT_ENCRYPTED_BOOTSTRAP)
  public Bootstrap encryptedClientBootstrap() throws Exception {
    //@formatter:off
    return new Bootstrap()
        .channel(NioSocketChannel.class)
        .group(new NioEventLoopGroup())
        .handler(encryptedClientInitializer());
    //@formatter:on
  }

  /**
   * Clear client bootstrap.
   *
   * @return the bootstrap
   * @throws Exception
   *           the exception
   */
  @Bean(name = CLIENT_CLEAR_BOOTSTRAP)
  public Bootstrap clearClientBootstrap() throws Exception {
    //@formatter:off
    return new Bootstrap()
        .channel(NioSocketChannel.class)
        .group(new NioEventLoopGroup())
        .handler(clearClientInitializer());
    //@formatter:on
  }

  /**
   * Encrypted client initializer.
   *
   * @return the encrypted client initializer
   */
  @Bean
  public EncryptedClientInitializer encryptedClientInitializer() {
    return new EncryptedClientInitializer();
  }

  /**
   * Clear client initializer.
   *
   * @return the clear client initializer
   */
  @Bean
  public ClearClientInitializer clearClientInitializer() {
    return new ClearClientInitializer();
  }

  /**
   * Client handler.
   *
   * @return the client handler
   */
  @Bean
  public ClientHandler clientHandler() {
    return new ClientHandler();
  }

}

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

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.github.mrstampy.gameboot.otp.netty.client.ClearClientInitializer;
import com.github.mrstampy.gameboot.otp.netty.client.ClientHandler;
import com.github.mrstampy.gameboot.otp.netty.client.EncryptedClientInitializer;
import com.github.mrstampy.gameboot.otp.netty.server.ClearServerInitializer;
import com.github.mrstampy.gameboot.otp.netty.server.EncryptedServerInitializer;

import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;

/**
 * The Class OtpNettyTestConfiguration.
 */
@Configuration
public class OtpNettyTestConfiguration {

  /** The Constant CLIENT_ENCRYPTED_BOOTSTRAP. */
  public static final String CLIENT_ENCRYPTED_BOOTSTRAP = "CLIENT_ENCRYPTED_BOOTSTRAP";

  /** The Constant CLIENT_CLEAR_BOOTSTRAP. */
  public static final String CLIENT_CLEAR_BOOTSTRAP = "CLIENT_CLEAR_BOOTSTRAP";

  /** The Constant SERVER_ENCRYPTED_BOOTSTRAP. */
  public static final String SERVER_ENCRYPTED_BOOTSTRAP = "SERVER_ENCRYPTED_BOOTSTRAP";

  /** The Constant SERVER_CLEAR_BOOTSTRAP. */
  public static final String SERVER_CLEAR_BOOTSTRAP = "SERVER_CLEAR_BOOTSTRAP";

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
   * Server encrypted bootstrap.
   *
   * @return the server bootstrap
   * @throws Exception
   *           the exception
   */
  @Bean(name = SERVER_ENCRYPTED_BOOTSTRAP)
  public ServerBootstrap serverEncryptedBootstrap() throws Exception {
    //@formatter:off
    return new ServerBootstrap()
        .channel(NioServerSocketChannel.class)
        .group(new NioEventLoopGroup(), new NioEventLoopGroup())
        .childHandler(encryptedServerInitializer());
    //@formatter:on
  }

  /**
   * Server clear bootstrap.
   *
   * @return the server bootstrap
   * @throws Exception
   *           the exception
   */
  @Bean(name = SERVER_CLEAR_BOOTSTRAP)
  public ServerBootstrap serverClearBootstrap() throws Exception {
    //@formatter:off
    return new ServerBootstrap()
        .channel(NioServerSocketChannel.class)
        .group(new NioEventLoopGroup(), new NioEventLoopGroup())
        .childHandler(clearServerInitializer());
    //@formatter:on
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
   * Clear server initializer.
   *
   * @return the clear server initializer
   */
  @Bean
  public ClearServerInitializer clearServerInitializer() {
    return new ClearServerInitializer();
  }

  /**
   * Encrypted server initializer.
   *
   * @return the encrypted server initializer
   */
  @Bean
  public EncryptedServerInitializer encryptedServerInitializer() {
    return new EncryptedServerInitializer();
  }
}

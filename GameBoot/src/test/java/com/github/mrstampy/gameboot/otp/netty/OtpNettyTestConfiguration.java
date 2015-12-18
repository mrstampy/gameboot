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
package com.github.mrstampy.gameboot.otp.netty;

import java.io.InputStream;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;

import javax.jms.IllegalStateException;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

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

  /** The Constant SERVER_SSL_CONTEXT. */
  public static final String SERVER_SSL_CONTEXT = "Netty Server Context";

  /** The Constant CLIENT_SSL_CONTEXT. */
  public static final String CLIENT_SSL_CONTEXT = "Netty Client Context";

  /** The Constant CLIENT_ENCRYPTED_BOOTSTRAP. */
  public static final String CLIENT_ENCRYPTED_BOOTSTRAP = "CLIENT_ENCRYPTED_BOOTSTRAP";

  /** The Constant CLIENT_CLEAR_BOOTSTRAP. */
  public static final String CLIENT_CLEAR_BOOTSTRAP = "CLIENT_CLEAR_BOOTSTRAP";

  /** The Constant SERVER_ENCRYPTED_BOOTSTRAP. */
  public static final String SERVER_ENCRYPTED_BOOTSTRAP = "SERVER_ENCRYPTED_BOOTSTRAP";

  /** The Constant SERVER_CLEAR_BOOTSTRAP. */
  public static final String SERVER_CLEAR_BOOTSTRAP = "SERVER_CLEAR_BOOTSTRAP";

  private static final String X_509 = "X.509";
  private static final String PROTOCOL = "TLS";

  private static final String JKS_LOCATION = "GameBoot.jks";
  private static final String CERT_LOCATION = "GameBoot.cer";

  private static final String HARDCODED_NSA_APPROVED_PASSWORD = "password";
  private static final String ALIAS = "GameBoot";

  /**
   * Ssl context.
   *
   * @return the SSL context
   * @throws Exception
   *           the exception
   */
  @Bean(name = SERVER_SSL_CONTEXT)
  public SSLContext sslContext() throws Exception {
    char[] password = HARDCODED_NSA_APPROVED_PASSWORD.toCharArray();

    KeyStore keystore = getKeyStore();
    keystore.load(getResource(JKS_LOCATION), password);

    KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());

    kmf.init(keystore, password);

    return createContext(keystore, kmf);
  }

  /**
   * Client context.
   *
   * @return the SSL context
   * @throws Exception
   *           the exception
   */
  @Bean(name = CLIENT_SSL_CONTEXT)
  public SSLContext clientContext() throws Exception {
    CertificateFactory cf = CertificateFactory.getInstance(X_509);
    Certificate cert = cf.generateCertificate(getResource(CERT_LOCATION));

    KeyStore keystore = getKeyStore();
    keystore.load(null);
    keystore.setCertificateEntry(ALIAS, cert);

    return createContext(keystore, null);
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

  @Bean
  public ClearClientInitializer clearClientInitializer() {
    return new ClearClientInitializer();
  }

  @Bean
  public ClientHandler clientHandler() {
    return new ClientHandler();
  }

  @Bean
  public EncryptedClientInitializer encryptedClientInitializer() {
    return new EncryptedClientInitializer();
  }

  @Bean
  public ClearServerInitializer clearServerInitializer() {
    return new ClearServerInitializer();
  }

  @Bean
  public EncryptedServerInitializer encryptedServerInitializer() {
    return new EncryptedServerInitializer();
  }

  private InputStream getResource(String name) throws Exception {
    ClassPathResource r = new ClassPathResource(name);

    if (!r.exists()) throw new IllegalStateException("No " + name + " on the classpath");

    return r.getInputStream();
  }

  // JKS.
  private KeyStore getKeyStore() throws KeyStoreException {
    return KeyStore.getInstance(KeyStore.getDefaultType());
  }

  private SSLContext createContext(KeyStore keystore, KeyManagerFactory kmf) throws Exception {
    TrustManagerFactory trustFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
    trustFactory.init(keystore);

    SSLContext sslContext = SSLContext.getInstance(PROTOCOL);
    sslContext.init(kmf == null ? null : kmf.getKeyManagers(), trustFactory.getTrustManagers(), null);

    return sslContext;
  }
}

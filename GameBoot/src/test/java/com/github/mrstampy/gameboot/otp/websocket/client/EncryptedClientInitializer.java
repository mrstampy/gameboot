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
package com.github.mrstampy.gameboot.otp.websocket.client;

import java.net.URISyntaxException;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.websocket.ClientEndpoint;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;

import com.github.mrstampy.gameboot.otp.OtpTestConfiguration;
import com.github.mrstampy.gameboot.otp.netty.client.ClientHandler;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.ssl.SslHandler;

/**
 * The Class EncryptedClientInitializer.
 */
@ClientEndpoint
public class EncryptedClientInitializer extends ChannelInitializer<NioSocketChannel> {

  @Autowired
  @Qualifier(OtpTestConfiguration.CLIENT_SSL_CONTEXT)
  private SSLContext sslContext;

  @Autowired
  private ClientHandler clientHandler;

  @Value("${wss.host}")
  private String host;

  @Value("${wss.port}")
  private int port;

  @Value("${wss.path}")
  private String path;

  /*
   * (non-Javadoc)
   * 
   * @see
   * io.netty.channel.ChannelInitializer#initChannel(io.netty.channel.Channel)
   */
  @Override
  protected void initChannel(NioSocketChannel ch) throws Exception {
    ChannelPipeline pipeline = ch.pipeline();

    pipeline.addLast(new SslHandler(createSslEngine()));
    pipeline.addLast(new HttpClientCodec());
    pipeline.addLast(new HttpObjectAggregator(8192));
    pipeline.addLast(getWebSocketHandler());

    pipeline.addLast(clientHandler);
  }

  private WebSocketHandler getWebSocketHandler() throws URISyntaxException {
    WebSocketHandler handler = new WebSocketHandler();

    handler.setHost(host);
    handler.setPort(port);
    handler.setPath(path);
    handler.setEncrypted(true);
    handler.init();

    return handler;
  }

  private SSLEngine createSslEngine() {
    SSLEngine engine = sslContext.createSSLEngine();

    engine.setUseClientMode(true);
    engine.setNeedClientAuth(false);

    return engine;
  }

}

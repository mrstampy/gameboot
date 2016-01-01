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
package com.github.mrstampy.gameboot.otp.websocket.client;

import java.lang.invoke.MethodHandles;
import java.net.URI;
import java.net.URISyntaxException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshaker;
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshakerFactory;
import io.netty.handler.codec.http.websocketx.WebSocketVersion;

/**
 * The Class WebSocketHandler.
 */
public class WebSocketHandler extends SimpleChannelInboundHandler<Object> {
  private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private HttpHeaders customHeaders = new DefaultHttpHeaders();

  private WebSocketClientHandshaker handshaker;
  private ChannelPromise handshakeFuture;

  private boolean encrypted = false;
  private String host;
  private int port;
  private String path;

  /*
   * (non-Javadoc)
   * 
   * @see io.netty.channel.ChannelHandlerAdapter#handlerAdded(io.netty.channel.
   * ChannelHandlerContext)
   */
  @Override
  public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
    handshakeFuture = ctx.newPromise();
  }

  /*
   * (non-Javadoc)
   * 
   * @see io.netty.channel.ChannelInboundHandlerAdapter#channelActive(io.netty.
   * channel.ChannelHandlerContext)
   */
  @Override
  public void channelActive(final ChannelHandlerContext ctx) throws Exception {
    handshaker.handshake(ctx.channel());
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * io.netty.channel.SimpleChannelInboundHandler#channelRead0(io.netty.channel.
   * ChannelHandlerContext, java.lang.Object)
   */
  @Override
  public void channelRead0(ChannelHandlerContext ctx, Object msg) throws Exception {
    Channel ch = ctx.channel();

    if (!handshaker.isHandshakeComplete()) {
      handshaker.finishHandshake(ch, (FullHttpResponse) msg);
      handshakeFuture.setSuccess();
      return;
    }

    if (!(msg instanceof BinaryWebSocketFrame)) {
      ch.close();
      log.warn("Received {}, closing", msg);
      return;
    }

    byte[] b = extractBytes(msg);

    ctx.fireChannelRead(b);
  }

  private byte[] extractBytes(Object msg) {
    BinaryWebSocketFrame frame = (BinaryWebSocketFrame) msg;

    ByteBuf buf = frame.content();

    byte[] b = new byte[buf.readableBytes()];

    buf.readBytes(b);

    return b;
  }

  /**
   * Inits the.
   *
   * @throws URISyntaxException
   *           the URI syntax exception
   */
  public void init() throws URISyntaxException {
    handshaker = WebSocketClientHandshakerFactory.newHandshaker(createUri(),
        WebSocketVersion.V13,
        null,
        false,
        customHeaders);
  }

  /**
   * Checks if is encrypted.
   *
   * @return true, if is encrypted
   */
  public boolean isEncrypted() {
    return encrypted;
  }

  /**
   * Sets the encrypted.
   *
   * @param encrypted
   *          the new encrypted
   */
  public void setEncrypted(boolean encrypted) {
    this.encrypted = encrypted;
  }

  /**
   * Gets the host.
   *
   * @return the host
   */
  public String getHost() {
    return host;
  }

  /**
   * Sets the host.
   *
   * @param host
   *          the new host
   */
  public void setHost(String host) {
    this.host = host;
  }

  /**
   * Gets the port.
   *
   * @return the port
   */
  public int getPort() {
    return port;
  }

  /**
   * Sets the port.
   *
   * @param port
   *          the new port
   */
  public void setPort(int port) {
    this.port = port;
  }

  /**
   * Gets the path.
   *
   * @return the path
   */
  public String getPath() {
    return path;
  }

  /**
   * Sets the path.
   *
   * @param path
   *          the new path
   */
  public void setPath(String path) {
    this.path = path;
  }

  private URI createUri() throws URISyntaxException {
    String protocol = isEncrypted() ? "wss://" : "ws://";

    String uri = protocol + getHost() + ":" + getPort() + getPath();

    log.debug("Created uri {}", uri);

    return new URI(uri);
  }
}
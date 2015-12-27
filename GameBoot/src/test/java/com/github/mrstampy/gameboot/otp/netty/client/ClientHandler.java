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
package com.github.mrstampy.gameboot.otp.netty.client;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import javax.resource.spi.IllegalStateException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.mrstampy.gameboot.messages.Response;
import com.github.mrstampy.gameboot.messages.Response.ResponseCode;
import com.github.mrstampy.gameboot.otp.OneTimePad;
import com.github.mrstampy.gameboot.otp.messages.OtpKeyRequest;

import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.handler.ssl.SslHandler;
import io.netty.util.concurrent.Future;

/**
 * The Class ClientHandler.
 */
@Sharable
public class ClientHandler extends ChannelDuplexHandler {
  private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Autowired
  private ObjectMapper mapper;

  @Autowired
  private OneTimePad pad;

  private byte[] otpKey;

  private Channel clearChannel;

  private Long systemId;

  private Response lastResponse;

  private CountDownLatch responseLatch;

  /*
   * (non-Javadoc)
   * 
   * @see io.netty.channel.ChannelInboundHandlerAdapter#channelActive(io.netty.
   * channel.ChannelHandlerContext)
   */
  @Override
  public void channelActive(ChannelHandlerContext ctx) throws Exception {
    SslHandler handler = ctx.pipeline().get(SslHandler.class);

    if (handler == null) return;

    handler.handshakeFuture().addListener(f -> validate(f, ctx));
  }

  private void validate(Future<? super Channel> f, ChannelHandlerContext ctx) {
    if (f.isSuccess()) {
      log.debug("Handshake successful with {}", ctx.channel());
    } else {
      log.error("Handshake unsuccessful, disconnecting {}", ctx.channel(), f.cause());
      ctx.close();
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * io.netty.channel.ChannelInboundHandlerAdapter#channelRead(io.netty.channel.
   * ChannelHandlerContext, java.lang.Object)
   */
  @Override
  public void channelRead(ChannelHandlerContext ctx, Object o) throws Exception {
    try {
      byte[] msg = (byte[]) o;

      if (isEncrypting(ctx)) {
        try {
          decrypt(msg);
          return;
        } catch (Exception e) {
          log.error("Cannot decrypt: assuming delete request sent: {}", e.getMessage());
        }
      }

      unencrypted(ctx, msg);
    } finally {
      if (responseLatch != null) responseLatch.countDown();
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see io.netty.channel.ChannelDuplexHandler#write(io.netty.channel.
   * ChannelHandlerContext, java.lang.Object, io.netty.channel.ChannelPromise)
   */
  @Override
  public void write(ChannelHandlerContext ctx, Object o, ChannelPromise promise) throws Exception {
    byte[] msg = (byte[]) o;

    msg = isEncrypting(ctx) ? pad.convert(otpKey, msg) : msg;

    ctx.write(msg, promise);
  }

  private boolean isEncrypting(ChannelHandlerContext ctx) {
    return otpKey != null && clearChannel == ctx.channel();
  }

  private void unencrypted(ChannelHandlerContext ctx, byte[] msg) throws Exception {
    Response r = getResponse(msg);
    lastResponse = r;

    boolean c = ctx.pipeline().get(SslHandler.class) != null;

    log.info("Unencrypted: on {} channel\n{}", (c ? "secured" : "unsecured"), mapper.writeValueAsString(r));

    if (!ok(r.getResponseCode())) return;

    if (ResponseCode.INFO == r.getResponseCode()) {
      Object[] payload = r.getResponse();
      if (payload == null || payload.length == 0 || !(payload[0] instanceof Map<?, ?>)) {
        throw new IllegalStateException("Expecting map of systemId:[value]");
      }

      String s = ((Map<?, ?>) payload[0]).get("systemId").toString();

      systemId = new Long(s);

      log.info("Setting system id {}", systemId);
      clearChannel = ctx.channel();
      return;
    }

    JsonNode node = mapper.readTree(msg);
    JsonNode response = node.get("response");

    boolean hasKey = response != null && response.isArray() && response.size() == 1;

    if (hasKey) {
      log.info("Setting key");
      otpKey = response.get(0).binaryValue();
      return;
    }

    switch (r.getType()) {
    case OtpKeyRequest.TYPE:
      log.info("Deleting key");
      otpKey = null;
      break;
    default:
      break;
    }
  }

  private Response getResponse(byte[] msg) throws IOException, JsonParseException, JsonMappingException {
    return mapper.readValue(msg, Response.class);
  }

  private boolean ok(ResponseCode responseCode) {
    switch (responseCode) {
    case SUCCESS:
    case INFO:
      return true;
    default:
      return false;
    }
  }

  private void decrypt(byte[] msg) throws Exception {
    byte[] converted = pad.convert(otpKey, msg);

    Response r = getResponse(converted);
    lastResponse = r;

    log.info("Encrypted: \n{}", mapper.writeValueAsString(r));
  }

  /**
   * Gets the system id.
   *
   * @return the system id
   */
  public Long getSystemId() {
    return systemId;
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
   * Gets the last response.
   *
   * @return the last response
   */
  public Response getLastResponse() {
    return lastResponse;
  }

  /**
   * Sets the response latch.
   *
   * @param responseLatch
   *          the new response latch
   */
  public void setResponseLatch(CountDownLatch responseLatch) {
    this.responseLatch = responseLatch;
  }

  /**
   * Checks for key.
   *
   * @return true, if successful
   */
  public boolean hasKey() {
    return otpKey != null;
  }

}

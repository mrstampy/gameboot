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

import java.lang.invoke.MethodHandles;
import java.util.concurrent.ExecutorService;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Profile;

import com.github.mrstampy.gameboot.concurrent.GameBootConcurrentConfiguration;
import com.github.mrstampy.gameboot.exception.GameBootException;
import com.github.mrstampy.gameboot.exception.GameBootRuntimeException;
import com.github.mrstampy.gameboot.messages.AbstractGameBootMessage;
import com.github.mrstampy.gameboot.messages.GameBootMessageConverter;
import com.github.mrstampy.gameboot.messages.Response;
import com.github.mrstampy.gameboot.messages.Response.ResponseCode;
import com.github.mrstampy.gameboot.metrics.MetricsHelper;
import com.github.mrstampy.gameboot.netty.AbstractGameBootNettyMessageHandler;
import com.github.mrstampy.gameboot.otp.KeyRegistry;
import com.github.mrstampy.gameboot.otp.OneTimePad;
import com.github.mrstampy.gameboot.otp.OtpConfiguration;
import com.github.mrstampy.gameboot.otp.messages.OtpKeyRequest;
import com.github.mrstampy.gameboot.otp.messages.OtpKeyRequest.KeyFunction;
import com.github.mrstampy.gameboot.otp.messages.OtpMessage;
import com.github.mrstampy.gameboot.otp.messages.OtpNewKeyAck;
import com.github.mrstampy.gameboot.otp.processor.OtpNewKeyRegistry;
import com.github.mrstampy.gameboot.util.GameBootUtils;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.ChannelPromise;

/**
 * The Class OtpClearNettyHandler is intended to provide a transparent means of
 * using the {@link OneTimePad} utility to encrypt outgoing and decrypt incoming
 * messages on unencrypted Netty connections. It is intended that this is a
 * last-in-pipeline handler and the message is a byte array. Inbound messages
 * are later converted to strings, all outbound messages are byte arrays.<br>
 * <br>
 * 
 * By default messages are unencrypted. An INFO message is sent to the client
 * containing the {@link Response#getSystemId()} value upon connection. The
 * client then creates a connection to the socket server containing the
 * {@link OtpEncryptedNettyHandler} in the pipeline and sends a message of type
 * {@link OtpKeyRequest} with a {@link KeyFunction} of {@link KeyFunction#NEW}
 * thru it to the server. The {@link OtpKeyRequest#getSystemId()} value will
 * have been set in the client as the value obtained from the clear connection's
 * INFO message.<br>
 * <br>
 * 
 * If the key generation is successful a {@link Response} object is returned in
 * the encrypted channel containing the new OTP key as the only element of the
 * {@link Response#getResponse()} array. When sending is complete the encrypted
 * channel is disconnected. The client then sends a message of type
 * {@link OtpNewKeyAck} encrypted using the new key in the clear channel. When
 * received the GameBoot server activates the new key for all traffic on the
 * {@link OtpClearNettyHandler} channel.<br>
 * <br>
 * 
 * To delete a key a message of type {@link OtpKeyRequest} with a
 * {@link KeyFunction} of {@link KeyFunction#DELETE} is sent to the server on
 * the encrypting clear channel. A {@link Response} of
 * {@link ResponseCode#SUCCESS} will be sent on success, clear text.<br>
 * <br>
 * 
 * Should any failures occur the old key, should it exist, is considered active.
 * <br>
 * <br>
 * 
 * It is intended that full implementations of GameBoot will implement
 * subclasses of this class to restrict message type processing to a whitelist.
 * <br>
 * <br>
 * 
 * Do not instantiate directly as this is a prototype Spring managed bean. Use
 * {@link GameBootUtils#getBean(Class)} to obtain a unique instance when
 * constructing the {@link ChannelPipeline}.
 * 
 * @see KeyRegistry
 * @see OneTimePad
 * @see OtpConfiguration
 * @see #inspect(ChannelHandlerContext, AbstractGameBootMessage)
 * @see #isValidType(ChannelHandlerContext, AbstractGameBootMessage)
 */
@Profile(OtpConfiguration.OTP_PROFILE)
public class OtpClearNettyHandler extends AbstractGameBootNettyMessageHandler {
  private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private static final String OTP_DECRYPT_COUNTER = "Netty OTP Decrypt Counter";

  private static final String OTP_ENCRYPT_COUNTER = "Netty OTP Encrypt Counter";

  @Autowired
  private OneTimePad oneTimePad;

  @Autowired
  private KeyRegistry keyRegistry;

  @Autowired
  private OtpNewKeyRegistry newKeyRegistry;

  @Autowired
  private MetricsHelper helper;

  @Autowired
  @Qualifier(GameBootConcurrentConfiguration.GAME_BOOT_EXECUTOR)
  private ExecutorService svc;

  @Autowired
  private GameBootMessageConverter converter;

  /**
   * Post construct.
   *
   * @throws Exception
   *           the exception
   */
  @PostConstruct
  public void postConstruct() throws Exception {
    super.postConstruct();

    if (!helper.containsCounter(OTP_DECRYPT_COUNTER)) {
      helper.counter(OTP_DECRYPT_COUNTER, getClass(), "otp", "decrypt", "counter");
    }

    if (!helper.containsCounter(OTP_ENCRYPT_COUNTER)) {
      helper.counter(OTP_ENCRYPT_COUNTER, getClass(), "otp", "encrypt", "counter");
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see io.netty.channel.ChannelInboundHandlerAdapter#channelActive(io.netty.
   * channel.ChannelHandlerContext)
   */
  @Override
  public void channelActive(ChannelHandlerContext ctx) throws Exception {
    super.channelActive(ctx);

    Response r = new Response(ResponseCode.INFO);
    r.setSystemId(getSystemId());

    ctx.channel().writeAndFlush(converter.toJsonArray(r));
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * io.netty.channel.ChannelInboundHandlerAdapter#channelInactive(io.netty.
   * channel.ChannelHandlerContext)
   */
  @Override
  public void channelInactive(ChannelHandlerContext ctx) throws Exception {
    super.channelInactive(ctx);

    oneTimePad = null;
    keyRegistry = null;
    helper = null;
    converter = null;
    svc = null;
    newKeyRegistry = null;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * io.netty.channel.ChannelInboundHandlerAdapter#channelRead(io.netty.channel.
   * ChannelHandlerContext, java.lang.Object)
   */
  @Override
  public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
    if (!(msg instanceof byte[])) {
      sendError(NOT_BYTE_ARRAY, ctx, "Message must be a byte array");
      return;
    }

    byte[] mb = (byte[]) msg;

    byte[] key = keyRegistry.get(getSystemId());

    if (key == null) {
      super.channelRead(ctx, evaluateForNewKeyAck(ctx, mb));
      return;
    }

    helper.incr(OTP_DECRYPT_COUNTER);

    byte[] b = evaluateForNewKeyAck(ctx, mb);
    byte[] converted = b == mb ? oneTimePad.convert(key, mb) : b;

    super.channelRead(ctx, converted);
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * com.github.mrstampy.gameboot.netty.AbstractGameBootNettyMessageHandler#
   * channelReadImpl(io.netty.channel.ChannelHandlerContext, byte[])
   */
  protected void channelReadImpl(ChannelHandlerContext ctx, byte[] msg) throws Exception {
    svc.execute(() -> {
      try {
        process(ctx, msg);
      } catch (GameBootException | GameBootRuntimeException e) {
        sendError(ctx, e);
      } catch (Exception e) {
        log.error("Unexpected exception", e);
        sendUnexpectedError(ctx);
      }
    });
  }

  @SuppressWarnings("unused")
  private byte[] evaluateForNewKeyAck(ChannelHandlerContext ctx, byte[] msg) {
    Long systemId = getSystemId();
    if (!newKeyRegistry.contains(systemId)) return msg;

    byte[] newKey = newKeyRegistry.get(systemId);

    try {
      byte[] converted = oneTimePad.convert(newKey, msg);
      OtpNewKeyAck ack = converter.fromJson(converted);
      return converted;
    } catch (Exception e) {
      String s = keyRegistry.contains(systemId) ? "old key" : "unencrypted";
      log.warn("Awaiting new key ack, assuming {} for {}, system id {}.", s, ctx.channel(), systemId);
    }

    return msg;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * com.github.mrstampy.gameboot.netty.AbstractGameBootNettyMessageHandler#
   * investigate(io.netty.channel.ChannelHandlerContext,
   * com.github.mrstampy.gameboot.messages.AbstractGameBootMessage)
   */
  protected <AGBM extends AbstractGameBootMessage> boolean inspect(ChannelHandlerContext ctx, AGBM agbm)
      throws Exception {
    boolean ok = true;

    switch (agbm.getType()) {
    case OtpKeyRequest.TYPE:
      ok = isDeleteRequest(ctx, (OtpKeyRequest) agbm);
      if (!ok) {
        Response fail = fail(UNEXPECTED_MESSAGE, agbm, null);
        ctx.writeAndFlush(converter.toJsonArray(fail));
      }
    case OtpNewKeyAck.TYPE:
      ((OtpMessage) agbm).setProcessorKey(getSystemId());
      break;
    default:
      ok = isValidType(ctx, agbm);
    }

    return ok;
  }

  /**
   * Checks if is valid type.
   *
   * @param <AGBM>
   *          the generic type
   * @param ctx
   *          the ctx
   * @param agbm
   *          the agbm
   * @return true, if is valid type
   */
  protected <AGBM extends AbstractGameBootMessage> boolean isValidType(ChannelHandlerContext ctx, AGBM agbm) {
    return true;
  }

  private boolean isDeleteRequest(ChannelHandlerContext ctx, OtpKeyRequest keyRequest) {
    boolean d = KeyFunction.DELETE == keyRequest.getKeyFunction();

    Long sysId = keyRequest.getSystemId();
    boolean ok = d && isEncrypting() && getSystemId().equals(sysId);

    if (!ok) log.error("Delete key for {} received on {}, key {}", sysId, ctx.channel(), getSystemId());

    return ok;
  }

  private boolean isEncrypting() {
    return keyRegistry.contains(getSystemId());
  }

  /*
   * (non-Javadoc)
   * 
   * @see io.netty.channel.ChannelDuplexHandler#write(io.netty.channel.
   * ChannelHandlerContext, java.lang.Object, io.netty.channel.ChannelPromise)
   */
  @Override
  public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
    if (!(msg instanceof String) && !(msg instanceof byte[])) {
      log.error("Internal error; object is not a string or byte array: {}", msg.getClass());
      return;
    }

    byte[] key = keyRegistry.get(getSystemId());

    byte[] processed = (msg instanceof byte[]) ? (byte[]) msg : ((String) msg).getBytes();

    if (key == null) {
      ctx.write(processed, promise);
      return;
    }

    helper.incr(OTP_ENCRYPT_COUNTER);

    byte[] converted = oneTimePad.convert(key, processed);

    ctx.write(converted, promise);
  }

}

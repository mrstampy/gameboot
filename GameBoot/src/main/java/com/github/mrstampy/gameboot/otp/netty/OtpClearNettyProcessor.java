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
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicReference;

import javax.annotation.PostConstruct;

import org.ehcache.internal.concurrent.ConcurrentHashMap;
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
import com.github.mrstampy.gameboot.metrics.MetricsHelper;
import com.github.mrstampy.gameboot.netty.AbstractNettyProcessor;
import com.github.mrstampy.gameboot.otp.KeyRegistry;
import com.github.mrstampy.gameboot.otp.OneTimePad;
import com.github.mrstampy.gameboot.otp.OtpConfiguration;
import com.github.mrstampy.gameboot.otp.messages.OtpKeyRequest;
import com.github.mrstampy.gameboot.otp.messages.OtpKeyRequest.KeyFunction;
import com.github.mrstampy.gameboot.otp.messages.OtpMessage;
import com.github.mrstampy.gameboot.otp.messages.OtpNewKeyAck;
import com.github.mrstampy.gameboot.otp.processor.OtpNewKeyRegistry;
import com.github.mrstampy.gameboot.util.concurrent.MDCRunnable;

import io.netty.channel.ChannelHandlerContext;

/**
 * The Class OtpClearNettyProcessor.
 */
@Profile(OtpConfiguration.OTP_PROFILE)
public class OtpClearNettyProcessor extends AbstractNettyProcessor {
  private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private static final Integer DEFAULT_KEY_CHANGE_ID = Integer.MAX_VALUE;

  private static final String OTP_DECRYPT_COUNTER = "Netty OTP Decrypt Counter";

  private static final String OTP_ENCRYPT_COUNTER = "Netty OTP Encrypt Counter";

  @Autowired
  private GameBootMessageConverter converter;

  @Autowired
  private OneTimePad oneTimePad;

  @Autowired
  private MetricsHelper helper;

  @Autowired
  @Qualifier(GameBootConcurrentConfiguration.GAME_BOOT_EXECUTOR)
  private ExecutorService svc;

  @Autowired
  private KeyRegistry keyRegistry;

  @Autowired
  private OtpNewKeyRegistry newKeyRegistry;

  /** The expecting key change. */
  protected Map<Integer, Boolean> expectingKeyChange = new ConcurrentHashMap<>();

  /** The otp key. */
  protected AtomicReference<byte[]> otpKey = new AtomicReference<>();

  /*
   * (non-Javadoc)
   * 
   * @see
   * com.github.mrstampy.gameboot.netty.AbstractNettyProcessor#postConstruct()
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
   * @see
   * com.github.mrstampy.gameboot.netty.AbstractNettyProcessor#onMessage(io.
   * netty.channel.ChannelHandlerContext, java.lang.Object)
   */
  public void onMessage(ChannelHandlerContext ctx, Object msg) throws Exception {
    if (!(msg instanceof byte[])) {
      sendError(getResponseContext(NOT_BYTE_ARRAY, ctx), ctx, "Message must be a byte array");
      return;
    }

    byte[] mb = (byte[]) msg;

    byte[] key = otpKey.get();

    byte[] b = evaluateForNewKeyAck(ctx, mb);

    if (key == null) {
      super.onMessage(ctx, b);
      return;
    }

    helper.incr(OTP_DECRYPT_COUNTER);

    byte[] converted = b == mb ? oneTimePad.convert(key, mb) : b;

    super.onMessage(ctx, converted);
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
   * com.github.mrstampy.gameboot.netty.AbstractNettyProcessor#onMessageImpl(io.
   * netty.channel.ChannelHandlerContext, byte[])
   */
  protected void onMessageImpl(ChannelHandlerContext ctx, byte[] msg) throws Exception {
    svc.execute(new MDCRunnable() {

      @Override
      protected void runImpl() {
        try {
          process(ctx, msg);
        } catch (GameBootException | GameBootRuntimeException e) {
          sendError(ctx, e);
        } catch (Exception e) {
          log.error("Unexpected exception", e);
          sendUnexpectedError(ctx);
        }
      }
    });
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.github.mrstampy.gameboot.processor.connection.ConnectionProcessor#
   * preProcess(java.lang.Object,
   * com.github.mrstampy.gameboot.messages.AbstractGameBootMessage)
   */
  @Override
  public <AGBM extends AbstractGameBootMessage> boolean preProcess(ChannelHandlerContext ctx, AGBM agbm)
      throws Exception {
    boolean ok = true;

    switch (agbm.getType()) {
    case OtpKeyRequest.TYPE:
      ok = isDeleteRequest(ctx, (OtpKeyRequest) agbm);
      if (ok) {
        pendingKeyChange(agbm);
      } else {
        Response fail = fail(getResponseContext(UNEXPECTED_MESSAGE, ctx), agbm);
        sendMessage(ctx, converter.toJsonArray(agbm), fail);
      }
      break;
    case OtpNewKeyAck.TYPE:
      pendingKeyChange(agbm);
      break;
    default:
      ok = isValidType(ctx, agbm);
    }

    return ok;
  }

  /**
   * Checks if is encrypting.
   *
   * @return true, if is encrypting
   */
  public boolean isEncrypting() {
    return otpKey.get() != null;
  }

  /**
   * Encrypt if required.
   *
   * @param msg
   *          the msg
   * @return the byte[]
   * @throws Exception
   *           the exception
   */
  public byte[] encryptIfRequired(Object msg) throws Exception {
    if (!(msg instanceof String) && !(msg instanceof byte[])) {
      log.error("Internal error; object is not a string or byte array: {}", msg.getClass());
      return null;
    }

    byte[] processed = (msg instanceof byte[]) ? (byte[]) msg : ((String) msg).getBytes();
    if (!isEncrypting()) return processed;

    byte[] key = otpKey.get();

    helper.incr(OTP_ENCRYPT_COUNTER);

    return oneTimePad.convert(key, processed);
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.github.mrstampy.gameboot.processor.connection.ConnectionProcessor#
   * postProcess(java.lang.Object,
   * com.github.mrstampy.gameboot.messages.AbstractGameBootMessage,
   * com.github.mrstampy.gameboot.messages.Response)
   */
  @Override
  public <AGBM extends AbstractGameBootMessage> void postProcess(ChannelHandlerContext ctx, AGBM agbm, Response r) {
    Integer id = agbm.getId();
    if (id == null) return;

    Boolean b = expectingKeyChange.get(id);
    if (b == null) return;

    try {
      postProcessForKey(agbm, r);
    } finally {
      expectingKeyChange.remove(id);
    }
  }

  /**
   * Post process for key.
   *
   * @param <AGBM>
   *          the generic type
   * @param agbm
   *          the agbm
   * @param r
   *          the r
   */
  protected <AGBM extends AbstractGameBootMessage> void postProcessForKey(AGBM agbm, Response r) {
    if (DEFAULT_KEY_CHANGE_ID.equals(agbm.getId())) {
      agbm.setId(null);
      r.setId(null);
    }

    if (!r.isSuccess()) return;

    switch (agbm.getType()) {
    case OtpNewKeyAck.TYPE:
      activateNewKey();
      break;
    case OtpKeyRequest.TYPE:
      deactivateKey();
      break;
    default:
      break;
    }
  }

  /**
   * Deactivate key.
   */
  protected void deactivateKey() {
    otpKey.set(null);
  }

  /**
   * Activate new key.
   */
  protected void activateNewKey() {
    otpKey.set(keyRegistry.get(getSystemId()));
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * com.github.mrstampy.gameboot.netty.AbstractNettyProcessor#onMessageImpl(io.
   * netty.channel.ChannelHandlerContext, java.lang.String)
   */
  @Override
  protected void onMessageImpl(ChannelHandlerContext ctx, String msg) throws Exception {
    throw new UnsupportedOperationException("Must be a byte array");
  }

  /**
   * Pending key change.
   *
   * @param <AGBM>
   *          the generic type
   * @param agbm
   *          the agbm
   */
  protected <AGBM extends AbstractGameBootMessage> void pendingKeyChange(AGBM agbm) {
    if (agbm.getId() == null) agbm.setId(DEFAULT_KEY_CHANGE_ID);
    expectingKeyChange.put(agbm.getId(), Boolean.TRUE);
    ((OtpMessage) agbm).setProcessorKey(getSystemId());
  }

  /**
   * Implement in subclasses to white list {@link AbstractGameBootMessage}s.
   *
   * @param <AGBM>
   *          the generic type
   * @param ctx
   *          the ctx
   * @param agbm
   *          the agbm
   * @return true, if is valid type
   * @see OtpConfiguration
   */
  protected <AGBM extends AbstractGameBootMessage> boolean isValidType(ChannelHandlerContext ctx, AGBM agbm) {
    return true;
  }

  private boolean isDeleteRequest(ChannelHandlerContext ctx, OtpKeyRequest keyRequest) {
    boolean d = KeyFunction.DELETE == keyRequest.getKeyFunction();

    Long sysId = keyRequest.getOtpSystemId();
    boolean ok = d && isEncrypting() && getSystemId().equals(sysId);

    if (!ok) log.error("Delete key for {} received on {}, key {}", sysId, ctx.channel(), getSystemId());

    return ok;
  }

}

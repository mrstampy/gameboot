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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.lang.invoke.MethodHandles;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.web.socket.server.support.WebSocketHttpRequestHandler;

import com.github.mrstampy.gameboot.TestConfiguration;
import com.github.mrstampy.gameboot.messages.AbstractGameBootMessage;
import com.github.mrstampy.gameboot.messages.GameBootMessageConverter;
import com.github.mrstampy.gameboot.messages.Response;
import com.github.mrstampy.gameboot.messages.Response.ResponseCode;
import com.github.mrstampy.gameboot.otp.OtpConfiguration;
import com.github.mrstampy.gameboot.otp.messages.OtpKeyRequest;
import com.github.mrstampy.gameboot.otp.messages.OtpKeyRequest.KeyFunction;
import com.github.mrstampy.gameboot.otp.messages.OtpNewKeyAck;
import com.github.mrstampy.gameboot.otp.netty.client.ClientHandler;
import com.github.mrstampy.gameboot.usersession.UserSessionConfiguration;
import com.github.mrstampy.gameboot.usersession.messages.UserMessage;
import com.github.mrstampy.gameboot.usersession.messages.UserMessage.Function;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;

/**
 * The Class OtpNettyTest.
 */
//@RunWith(SpringJUnit4ClassRunner.class)
//@SpringApplicationConfiguration({ TestConfiguration.class, OtpWebSocketTestConfiguration.class })
//@ActiveProfiles({ OtpConfiguration.OTP_PROFILE, UserSessionConfiguration.USER_SESSION_PROFILE })
public class OtpWebSocketTest {
  private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private static final String PASSWORD = "password";

  private static final String TEST_USER = "testuser";

  @Value("${wss.host}")
  private String encHost;

  @Value("${wss.port}")
  private int encPort;

  @Value("${ws.host}")
  private String clrHost;

  @Value("${ws.port}")
  private int clrPort;

  @Autowired
  @Qualifier(OtpWebSocketTestConfiguration.CLIENT_ENCRYPTED_BOOTSTRAP)
  private Bootstrap encClient;

  @Autowired
  @Qualifier(OtpWebSocketTestConfiguration.CLIENT_CLEAR_BOOTSTRAP)
  private Bootstrap clearClient;

  @Autowired
  private ClientHandler clientHandler;

  @Autowired
  private GameBootMessageConverter converter;

  @Autowired
  @Qualifier(OtpWebSocketTestConfiguration.ENCRYPTED_WS)
  private WebSocketHttpRequestHandler encrypted;

  @Autowired
  @Qualifier(OtpWebSocketTestConfiguration.CLEAR_WS)
  private WebSocketHttpRequestHandler clear;

  private Channel encChannel;

  private Channel clearChannel;

  /**
   * Before.
   *
   * @throws Exception
   *           the exception
   */
//  @Before
  public void before() throws Exception {
    createClearChannel();

    createEncryptedChannel();

    encryptClearChannel();
  }

  /**
   * After.
   *
   * @throws Exception
   *           the exception
   */
//  @After
  public void after() throws Exception {
    deleteOtpKey();

    clearChannel.close();
  }

  /**
   * Test encrypted create user.
   *
   * @throws Exception
   *           the exception
   */
//  @Test
  public void testEncryptedCreateUser() throws Exception {
    UserMessage m = new UserMessage();
    m.setId(1);
    m.setFunction(Function.CREATE);
    m.setUserName(TEST_USER);
    m.setNewPassword(PASSWORD);

    sendMessage(m, clearChannel);

    Response r = clientHandler.getLastResponse();

    assertEquals(m.getId(), r.getId());
    assertEquals(ResponseCode.SUCCESS, r.getResponseCode());
    assertNotNull(r.getResponse());
    assertEquals(1, r.getResponse().length);
  }

  /**
   * Test encrypted channel.
   *
   * @throws Exception
   *           the exception
   */
//  @Test
  public void testEncryptedChannel() throws Exception {
    deleteOtpKey();
    createEncryptedChannel();

    assertTrue(encChannel.isActive());

    OtpKeyRequest newKey = new OtpKeyRequest();

    sendMessage(newKey, encChannel);

    assertFalse(encChannel.isActive());
    createEncryptedChannel();

    newKey.setSystemId(clientHandler.getSystemId());

    sendMessage(newKey, encChannel);

    assertFalse(encChannel.isActive());
    createEncryptedChannel();

    newKey.setKeyFunction(KeyFunction.DELETE);

    sendMessage(newKey, encChannel);

    assertFalse(encChannel.isActive());
    createEncryptedChannel();

    newKey.setSystemId(12345l);
    newKey.setKeyFunction(KeyFunction.NEW);

    sendMessage(newKey, encChannel);

    assertFalse(encChannel.isActive());
    createEncryptedChannel();

    UserMessage m = new UserMessage();

    sendMessage(m, encChannel);

    assertFalse(encChannel.isActive());

    createEncryptedChannel();
    encryptClearChannel();
  }

  /**
   * Delete unencrypted.
   *
   * @throws Exception
   *           the exception
   */
//  @Test
  public void deleteUnencrypted() throws Exception {
    deleteOtpKey();

    OtpKeyRequest del = new OtpKeyRequest();
    del.setId(99);
    del.setSystemId(clientHandler.getSystemId());
    del.setKeyFunction(KeyFunction.DELETE);

    sendMessage(del, clearChannel);

    Response r = clientHandler.getLastResponse();
    assertFalse(r.isSuccess());
    assertEquals(del.getId(), r.getId());

    createEncryptedChannel();
    encryptClearChannel();
  }

  private void deleteOtpKey() throws Exception {
    OtpKeyRequest delKey = new OtpKeyRequest();
    delKey.setId(3);
    delKey.setSystemId(clientHandler.getSystemId());
    delKey.setKeyFunction(KeyFunction.DELETE);

    sendMessage(delKey, clearChannel);

    Response r = clientHandler.getLastResponse();

    assertTrue(r.isSuccess());
    assertEquals(3, r.getId().intValue());
    assertFalse(clientHandler.hasKey());
  }

  private void encryptClearChannel() throws Exception {
    assertFalse(clientHandler.hasKey());

    OtpKeyRequest newKey = new OtpKeyRequest();
    newKey.setId(1);
    newKey.setSystemId(clientHandler.getSystemId());
    newKey.setKeyFunction(KeyFunction.NEW);

    // send new key request on encrypted channel
    sendMessage(newKey, encChannel);

    assertTrue(clientHandler.hasKey());

    Response r = clientHandler.getLastResponse();

    assertTrue(r.isSuccess());
    assertEquals(1, r.getId().intValue());

    OtpNewKeyAck ack = new OtpNewKeyAck();
    ack.setSystemId(clientHandler.getSystemId());
    ack.setId(2);

    // send new key ack on clear channel, will be encrypted
    sendMessage(ack, clearChannel);

    r = clientHandler.getLastResponse();

    assertTrue(r.isSuccess());
    assertEquals(2, r.getId().intValue());

    assertFalse(encChannel.isActive());
  }

  private void createClearChannel() throws Exception {
    // Session session =
    // ContainerProvider.getWebSocketContainer().connectToServer(ClearClientInitializer.class,
    // new URI("ws://localhost:12345/otp/clear"));
    //
    // System.out.println(session.isOpen());
    CountDownLatch cdl = new CountDownLatch(1);
    clientHandler.setResponseLatch(cdl);

    ChannelFuture cf = clearClient.connect(clrHost, clrPort);
    cdl.await(1, TimeUnit.SECONDS);

    assertTrue(cf.isSuccess());
    clearChannel = cf.channel();

    assertNotNull(clientHandler.getSystemId());
    assertEquals(clearChannel, clientHandler.getClearChannel());
  }

  private void createEncryptedChannel() throws InterruptedException {
    ChannelFuture cf = encClient.connect(encHost, encPort);
    cf.await(1, TimeUnit.SECONDS);

    assertTrue(cf.isSuccess());
    encChannel = cf.channel();
  }

  private void sendMessage(AbstractGameBootMessage message, Channel channel) throws Exception {
    CountDownLatch cdl = new CountDownLatch(1);
    clientHandler.setResponseLatch(cdl);

    boolean b = clientHandler.hasKey();

    channel.writeAndFlush(converter.toJsonArray(message));

    log.info("Sending {}: {}", (b ? "encrypted" : "unencrypted"), converter.toJson(message));

    cdl.await(1, TimeUnit.SECONDS);
  }

}

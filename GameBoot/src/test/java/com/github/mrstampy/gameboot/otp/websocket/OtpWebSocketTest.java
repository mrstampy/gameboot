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
import java.net.URI;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLContext;
import javax.websocket.ClientEndpointConfig;
import javax.websocket.ContainerProvider;
import javax.websocket.Session;

import org.apache.tomcat.websocket.WsWebSocketContainer;
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
import org.springframework.boot.test.WebIntegrationTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.github.mrstampy.gameboot.TestConfiguration;
import com.github.mrstampy.gameboot.messages.AbstractGameBootMessage;
import com.github.mrstampy.gameboot.messages.GameBootMessageConverter;
import com.github.mrstampy.gameboot.messages.Response;
import com.github.mrstampy.gameboot.messages.Response.ResponseCode;
import com.github.mrstampy.gameboot.otp.OtpConfiguration;
import com.github.mrstampy.gameboot.otp.OtpTestConfiguration;
import com.github.mrstampy.gameboot.otp.messages.OtpKeyRequest;
import com.github.mrstampy.gameboot.otp.messages.OtpKeyRequest.KeyFunction;
import com.github.mrstampy.gameboot.otp.messages.OtpNewKeyAck;
import com.github.mrstampy.gameboot.usersession.UserSessionConfiguration;
import com.github.mrstampy.gameboot.usersession.messages.UserMessage;
import com.github.mrstampy.gameboot.usersession.messages.UserMessage.Function;

/**
 * The Class OtpNettyTest.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration({ TestConfiguration.class, OtpWebSocketTestConfiguration.class })
@ActiveProfiles({ OtpConfiguration.OTP_PROFILE, UserSessionConfiguration.USER_SESSION_PROFILE })
@WebIntegrationTest
public class OtpWebSocketTest {
  private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private static final String PASSWORD = "password";

  private static final String TEST_USER = "testuser";

  @Value("${server.port}")
  private int port;

  @Value("${ws.path}")
  private String clearPath;

  @Value("${wss.path}")
  private String encPath;

  @Autowired
  private GameBootMessageConverter converter;

  @Autowired
  @Qualifier(OtpTestConfiguration.CLIENT_SSL_CONTEXT)
  private SSLContext sslContext;

  @Autowired
  private WebSocketEndpoint endpoint;

  private Session clearChannel;

  private Session encChannel;

  /**
   * Before.
   *
   * @throws Exception
   *           the exception
   */
  @Before
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
  @After
  public void after() throws Exception {
    deleteOtpKey();

    if (clearChannel != null) clearChannel.close();
  }

  /**
   * Test encrypted create user.
   *
   * @throws Exception
   *           the exception
   */
  // @Test
  public void testEncryptedCreateUser() throws Exception {
    UserMessage m = new UserMessage();
    m.setId(1);
    m.setFunction(Function.CREATE);
    m.setUserName(TEST_USER);
    m.setNewPassword(PASSWORD);

    sendMessage(m, clearChannel);

    Response r = endpoint.getLastResponse();

    assertEquals(m.getId(), r.getId());
    assertEquals(ResponseCode.SUCCESS, r.getResponseCode());
    assertNotNull(r.getPayload());
    assertEquals(1, r.getPayload().length);
  }

  /**
   * Test encrypted channel.
   *
   * @throws Exception
   *           the exception
   */
  @Test
  public void testEncryptedChannel() throws Exception {
    deleteOtpKey();
    createEncryptedChannel();

    assertTrue(encChannel.isOpen());

    OtpKeyRequest newKey = new OtpKeyRequest();

    sendMessage(newKey, encChannel);

    assertFalse(encChannel.isOpen());
    createEncryptedChannel();

    newKey.setOtpSystemId(endpoint.getSystemId());

    sendMessage(newKey, encChannel);

    assertFalse(encChannel.isOpen());
    createEncryptedChannel();

    newKey.setKeyFunction(KeyFunction.DELETE);

    sendMessage(newKey, encChannel);

    assertFalse(encChannel.isOpen());
    createEncryptedChannel();

    newKey.setOtpSystemId(12345l);
    newKey.setKeyFunction(KeyFunction.NEW);

    sendMessage(newKey, encChannel);

    assertFalse(encChannel.isOpen());
    createEncryptedChannel();

    UserMessage m = new UserMessage();

    sendMessage(m, encChannel);

    assertFalse(encChannel.isOpen());

    createEncryptedChannel();
    encryptClearChannel();
  }

  /**
   * Delete unencrypted.
   *
   * @throws Exception
   *           the exception
   */
  @Test
  public void deleteUnencrypted() throws Exception {
    deleteOtpKey();

    OtpKeyRequest del = new OtpKeyRequest();
    del.setId(99);
    del.setOtpSystemId(endpoint.getSystemId());
    del.setKeyFunction(KeyFunction.DELETE);

    sendMessage(del, clearChannel);

    Response r = endpoint.getLastResponse();
    assertFalse(r.isSuccess());
    assertEquals(del.getId(), r.getId());

    createEncryptedChannel();
    encryptClearChannel();
  }

  private void deleteOtpKey() throws Exception {
    OtpKeyRequest delKey = new OtpKeyRequest();
    delKey.setId(3);
    delKey.setOtpSystemId(endpoint.getSystemId());
    delKey.setKeyFunction(KeyFunction.DELETE);

    sendMessage(delKey, clearChannel);

    Response r = endpoint.getLastResponse();

    if (r == null) return;

    assertTrue(r.isSuccess());
    assertEquals(3, r.getId().intValue());
    assertFalse(endpoint.hasKey());
  }

  private void encryptClearChannel() throws Exception {
    assertFalse(endpoint.hasKey());

    OtpKeyRequest newKey = new OtpKeyRequest();
    newKey.setId(1);
    newKey.setOtpSystemId(endpoint.getSystemId());
    newKey.setKeyFunction(KeyFunction.NEW);

    // send new key request on encrypted channel
    sendMessage(newKey, encChannel);

    assertTrue(endpoint.hasKey());

    Response r = endpoint.getLastResponse();

    assertTrue(r.isSuccess());
    assertEquals(1, r.getId().intValue());

    OtpNewKeyAck ack = new OtpNewKeyAck();
    ack.setOtpSystemId(endpoint.getSystemId());
    ack.setId(2);

    // send new key ack on clear channel, will be encrypted
    sendMessage(ack, clearChannel);

    r = endpoint.getLastResponse();

    assertTrue(r.isSuccess());
    assertEquals(2, r.getId().intValue());

    Thread.sleep(300);

    assertFalse(encChannel.isOpen());
  }

  private void createClearChannel() throws Exception {
    ClientEndpointConfig config = ClientEndpointConfig.Builder.create().build();
    config.getUserProperties().put(WsWebSocketContainer.SSL_CONTEXT_PROPERTY, sslContext);
    clearChannel = ContainerProvider.getWebSocketContainer().connectToServer(endpoint,
        config,
        new URI(createClearUriString()));

    assertTrue(clearChannel.isOpen());

    CountDownLatch cdl = new CountDownLatch(1);
    endpoint.setResponseLatch(cdl);

    cdl.await(1, TimeUnit.SECONDS);

    assertNotNull(endpoint.getSystemId());
    assertEquals(clearChannel, endpoint.getSession());
  }

  private void createEncryptedChannel() throws Exception {
    ClientEndpointConfig config = ClientEndpointConfig.Builder.create().build();
    config.getUserProperties().put(WsWebSocketContainer.SSL_CONTEXT_PROPERTY, sslContext);
    encChannel = ContainerProvider.getWebSocketContainer().connectToServer(endpoint,
        config,
        new URI(createEncUriString()));

    assertTrue(encChannel.isOpen());
  }

  private String createClearUriString() {
    return "wss://localhost:" + port + clearPath;
  }

  private String createEncUriString() {
    return "wss://localhost:" + port + encPath;
  }

  private void sendMessage(AbstractGameBootMessage message, Session channel) throws Exception {
    if (channel == null || !channel.isOpen()) return;

    CountDownLatch cdl = new CountDownLatch(1);
    endpoint.setResponseLatch(cdl);

    boolean b = endpoint.hasKey();

    log.info("Sending {} to session {}: {}",
        (b ? "encrypted" : "unencrypted"),
        channel.getId(),
        converter.toJson(message));

    endpoint.sendMessage(converter.toJsonArray(message), channel);

    cdl.await(1, TimeUnit.SECONDS);
  }

}

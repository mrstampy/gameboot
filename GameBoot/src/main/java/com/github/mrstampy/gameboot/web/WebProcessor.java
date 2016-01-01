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
package com.github.mrstampy.gameboot.web;

import static org.apache.commons.lang3.StringUtils.isEmpty;

import java.lang.invoke.MethodHandles;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpSession;

import org.apache.commons.lang3.NotImplementedException;
import org.ehcache.internal.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.github.mrstampy.gameboot.concurrent.GameBootConcurrentConfiguration;
import com.github.mrstampy.gameboot.controller.GameBootMessageController;
import com.github.mrstampy.gameboot.exception.GameBootException;
import com.github.mrstampy.gameboot.exception.GameBootRuntimeException;
import com.github.mrstampy.gameboot.exception.GameBootThrowable;
import com.github.mrstampy.gameboot.messages.AbstractGameBootMessage;
import com.github.mrstampy.gameboot.messages.AbstractGameBootMessage.Transport;
import com.github.mrstampy.gameboot.messages.GameBootMessageConverter;
import com.github.mrstampy.gameboot.messages.Response;
import com.github.mrstampy.gameboot.messages.context.ResponseContext;
import com.github.mrstampy.gameboot.metrics.MetricsHelper;
import com.github.mrstampy.gameboot.processor.connection.AbstractConnectionProcessor;
import com.github.mrstampy.gameboot.systemid.SystemId;
import com.github.mrstampy.gameboot.systemid.SystemIdKey;
import com.github.mrstampy.gameboot.util.GameBootUtils;
import com.github.mrstampy.gameboot.util.registry.AbstractRegistryKey;
import com.github.mrstampy.gameboot.util.registry.RegistryCleaner;

/**
 * The Class WebProcessor.
 */
@Component
public class WebProcessor extends AbstractConnectionProcessor<HttpSession> {
  private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private static final String MESSAGE_COUNTER = "GameBoot Web Message Counter";

  private static final String FAILED_MESSAGE_COUNTER = "GameBoot Web Failed Message Counter";

  @Autowired
  private SystemId generator;

  @Autowired
  private MetricsHelper helper;

  @Autowired
  private HttpSessionRegistry registry;

  @Autowired
  private RegistryCleaner cleaner;

  @Autowired
  private GameBootMessageConverter converter;

  @Autowired
  private GameBootUtils utils;

  private WebAllowable allowable;

  /** The system ids. */
  protected Map<String, SystemIdKey> systemIds = new ConcurrentHashMap<>();

  /**
   * Post construct, invoke from {@link PostConstruct}-annotated subclass
   * methods.
   *
   * @throws Exception
   *           the exception
   */
  @PostConstruct
  public void postConstruct() throws Exception {
    if (!helper.containsCounter(MESSAGE_COUNTER)) {
      helper.counter(MESSAGE_COUNTER, getClass(), "inbound", "messages");
    }

    if (!helper.containsCounter(FAILED_MESSAGE_COUNTER)) {
      helper.counter(FAILED_MESSAGE_COUNTER, getClass(), "failed", "messages");
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.github.mrstampy.gameboot.processor.connection.ConnectionProcessor#
   * onConnection(java.lang.Object)
   */
  @Override
  public void onConnection(HttpSession httpSession) throws Exception {
    SystemIdKey key = generator.next();

    systemIds.put(httpSession.getId(), key);

    registry.put(key, httpSession);
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.github.mrstampy.gameboot.processor.connection.ConnectionProcessor#
   * onDisconnection(java.lang.Object)
   */
  @Override
  public void onDisconnection(HttpSession httpSession) throws Exception {
    String id = httpSession.getId();

    SystemIdKey systemId = systemIds.remove(id);
    cleaner.cleanup(systemId);

    Set<Entry<AbstractRegistryKey<?>, HttpSession>> set = registry.getKeysForValue(httpSession);

    set.forEach(e -> registry.remove(e.getKey()));
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.github.mrstampy.gameboot.processor.connection.ConnectionProcessor#
   * onMessage(java.lang.Object, java.lang.Object)
   */
  @Override
  public void onMessage(HttpSession httpSession, Object msg) throws Exception {
    if (msg instanceof String) {
      onMessageImpl(httpSession, (String) msg);
    } else if (msg instanceof byte[]) {
      onMessageImpl(httpSession, (byte[]) msg);
    } else {
      log.error("Only strings or byte arrays: {} from {}. Disconnecting", msg.getClass(), httpSession);
    }
  }

  /**
   * On message impl delegates to {@link #process(HttpSession, byte[])},
   * override to process the message using one of the executors in
   * {@link GameBootConcurrentConfiguration}.
   *
   * @param httpSession
   *          the httpSession
   * @param msg
   *          the msg
   * @throws Exception
   *           the exception
   */
  protected void onMessageImpl(HttpSession httpSession, byte[] msg) throws Exception {
    process(httpSession, msg);
  }

  /**
   * On message impl delegates to {@link #process(HttpSession, String)},
   * override to process the message using one of the executors in
   * {@link GameBootConcurrentConfiguration}.
   *
   * @param httpSession
   *          the httpSession
   * @param msg
   *          the msg
   * @throws Exception
   *           the exception
   */
  protected void onMessageImpl(HttpSession httpSession, String msg) throws Exception {
    process(httpSession, msg);
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.github.mrstampy.gameboot.processor.connection.ConnectionProcessor#
   * process(java.lang.Object, java.lang.String)
   */
  @Override
  public <AGBM extends AbstractGameBootMessage> Response process(HttpSession httpSession, String msg) throws Exception {
    if (isEmpty(msg)) return null;

    helper.incr(MESSAGE_COUNTER);

    GameBootMessageController controller = utils.getBean(GameBootMessageController.class);

    Response response = null;
    AGBM agbm = null;
    String type = null;
    Integer id = null;
    try {
      agbm = converter.fromJson(msg);

      if (!allowable.isAllowable(agbm)) return fail(getResponseContext(UNEXPECTED_MESSAGE, httpSession), agbm);

      type = agbm.getType();
      id = agbm.getId();

      if (!preProcess(httpSession, agbm)) return null;

      response = process(httpSession, controller, agbm);
    } catch (GameBootException | GameBootRuntimeException e) {
      helper.incr(FAILED_MESSAGE_COUNTER);
      response = fail(httpSession, agbm, e);
    } catch (Exception e) {
      helper.incr(FAILED_MESSAGE_COUNTER);
      log.error("Unexpected exception processing message type {}, id {} on channel {}", type, id, httpSession, e);
      response = fail(getResponseContext(UNEXPECTED_ERROR, httpSession), agbm, "An unexpected error has occurred");
    }

    postProcess(httpSession, agbm, response);

    return response;
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.github.mrstampy.gameboot.processor.connection.ConnectionProcessor#
   * process(java.lang.Object, byte[])
   */
  @Override
  public <AGBM extends AbstractGameBootMessage> Response process(HttpSession httpSession, byte[] msg) throws Exception {
    if (msg == null || msg.length == 0) return null;

    helper.incr(MESSAGE_COUNTER);

    GameBootMessageController controller = utils.getBean(GameBootMessageController.class);

    Response response = null;
    AGBM agbm = null;
    String type = null;
    Integer id = null;
    try {
      agbm = converter.fromJson(msg);

      if (!allowable.isAllowable(agbm)) return fail(getResponseContext(UNEXPECTED_MESSAGE, httpSession), agbm);

      type = agbm.getType();
      id = agbm.getId();

      if (!preProcess(httpSession, agbm)) return null;

      response = process(httpSession, controller, agbm);
    } catch (GameBootException | GameBootRuntimeException e) {
      helper.incr(FAILED_MESSAGE_COUNTER);
      response = fail(httpSession, agbm, e);
    } catch (Exception e) {
      helper.incr(FAILED_MESSAGE_COUNTER);
      log.error("Unexpected exception processing message type {}, id {} on channel {}", type, id, httpSession, e);
      response = fail(getResponseContext(UNEXPECTED_ERROR, httpSession), agbm, "An unexpected error has occurred");
    }

    postProcess(httpSession, agbm, response);

    return response;
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.github.mrstampy.gameboot.processor.connection.ConnectionProcessor#
   * process(java.lang.Object,
   * com.github.mrstampy.gameboot.controller.GameBootMessageController,
   * com.github.mrstampy.gameboot.messages.AbstractGameBootMessage)
   */
  @Override
  public <AGBM extends AbstractGameBootMessage> Response process(HttpSession httpSession,
      GameBootMessageController controller, AGBM agbm) throws Exception {
    agbm.setSystemId(getSystemId(httpSession));
    agbm.setTransport(Transport.WEB);

    Response r = controller.process(agbm);
    processMappingKeys(r, httpSession);
    r.setSystemId(agbm.getSystemId());

    return r;
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.github.mrstampy.gameboot.processor.connection.ConnectionProcessor#
   * sendMessage(java.lang.Object, java.lang.Object,
   * com.github.mrstampy.gameboot.messages.Response)
   */
  @Override
  public void sendMessage(HttpSession httpSession, Object msg, Response response) throws Exception {
    throw new NotImplementedException("Cannot send a message to an HttpSession");
  }

  /**
   * Blank implementation returns true, override to execute any pre process
   * logic for the {@link AbstractGameBootMessage}.
   *
   * @param <AGBM>
   *          the generic type
   * @param httpSession
   *          the http session
   * @param agbm
   *          the agbm
   * @return true, if successful
   * @throws Exception
   *           the exception
   */
  @Override
  public <AGBM extends AbstractGameBootMessage> boolean preProcess(HttpSession httpSession, AGBM agbm)
      throws Exception {
    return true;
  }

  /**
   * Blank implementation, override to execute any post process logic for the
   * {@link AbstractGameBootMessage}.
   *
   * @param <AGBM>
   *          the generic type
   * @param httpSession
   *          the http session
   * @param agbm
   *          the agbm
   * @param r
   *          the r
   */
  @Override
  public <AGBM extends AbstractGameBootMessage> void postProcess(HttpSession httpSession, AGBM agbm, Response r) {
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.github.mrstampy.gameboot.processor.connection.ConnectionProcessor#
   * sendError(java.lang.Object,
   * com.github.mrstampy.gameboot.exception.GameBootThrowable)
   */
  @Override
  public void sendError(HttpSession httpSession, GameBootThrowable e) {
    throw new NotImplementedException("Cannot send a message to an HttpSession");
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.github.mrstampy.gameboot.processor.connection.ConnectionProcessor#
   * sendError(com.github.mrstampy.gameboot.messages.context.ResponseContext,
   * java.lang.Object, java.lang.String)
   */
  @Override
  public void sendError(ResponseContext rc, HttpSession httpSession, String message) {
    throw new NotImplementedException("Cannot send a message to an HttpSession");
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.github.mrstampy.gameboot.processor.connection.ConnectionProcessor#
   * getSystemId(java.lang.Object)
   */
  @Override
  public SystemIdKey getSystemId(HttpSession httpSession) {
    return systemIds.get(httpSession.getId());
  }

  private void processMappingKeys(Response r, HttpSession httpSession) {
    AbstractRegistryKey<?>[] keys = r.getMappingKeys();
    if (keys == null || keys.length == 0) return;

    for (int i = 0; i < keys.length; i++) {
      registry.put(keys[i], httpSession);
    }
  }

  /**
   * Sets the allowable component used to discriminate
   * {@link AbstractGameBootMessage}s, exposed for overriding by subclasses.
   *
   * @param allowable
   *          the new allowable
   */
  @Autowired
  public void setAllowable(WebAllowable allowable) {
    this.allowable = allowable;
  }

}

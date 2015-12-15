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
package com.github.mrstampy.gameboot.messages;

import static org.apache.commons.lang3.StringUtils.isEmpty;

import java.io.IOException;
import java.lang.invoke.MethodHandles;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.mrstampy.gameboot.controller.MessageClassFinder;
import com.github.mrstampy.gameboot.exception.GameBootException;

/**
 * The Class GameBootMessageConverter.
 */
@Component
public class GameBootMessageConverter {
  private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private static final String TYPE_NODE_NAME = "type";

  @Autowired
  private ObjectMapper mapper;

  @Autowired
  private MessageClassFinder finder;

  /**
   * From json.
   *
   * @param <AGBM>
   *          the generic type
   * @param message
   *          the message
   * @return the agbm
   * @throws IOException
   *           Signals that an I/O exception has occurred.
   * @throws GameBootException
   *           the game boot exception
   */
  public <AGBM extends AbstractGameBootMessage> AGBM fromJson(String message) throws IOException, GameBootException {
    if (isEmpty(message)) fail("No message");

    JsonNode node = mapper.readTree(message);

    return fromJson(message.getBytes(), node);
  }

  /**
   * From json.
   *
   * @param <AGBM>
   *          the generic type
   * @param message
   *          the message
   * @return the agbm
   * @throws IOException
   *           Signals that an I/O exception has occurred.
   * @throws GameBootException
   *           the game boot exception
   */
  public <AGBM extends AbstractGameBootMessage> AGBM fromJson(byte[] message) throws IOException, GameBootException {
    if (message == null || message.length == 0) fail("No message");

    JsonNode node = mapper.readTree(message);

    return fromJson(message, node);
  }

  @SuppressWarnings("unchecked")
  private <AGBM extends AbstractGameBootMessage> AGBM fromJson(byte[] message, JsonNode node)
      throws GameBootException, IOException, JsonParseException, JsonMappingException {
    JsonNode typeNode = node.get(TYPE_NODE_NAME);

    if (typeNode == null) fail("No type specified");

    String type = typeNode.asText();

    if (isEmpty(type)) fail("No type specified");

    Class<?> clz = finder.findClass(type);

    if (clz == null) {
      log.error("Unknown message type {} for message {}", type, message);
      fail("Unrecognized message");
    }

    return (AGBM) mapper.readValue(message, clz);
  }

  /**
   * To json.
   *
   * @param <AGBM>
   *          the generic type
   * @param msg
   *          the msg
   * @return the string
   * @throws JsonProcessingException
   *           the json processing exception
   * @throws GameBootException
   *           the game boot exception
   */
  public <AGBM extends AbstractGameBootMessage> String toJson(AGBM msg)
      throws JsonProcessingException, GameBootException {
    if (msg == null) fail("No message");

    return mapper.writeValueAsString(msg);
  }

  /**
   * To json array.
   *
   * @param <AGBM>
   *          the generic type
   * @param msg
   *          the msg
   * @return the byte[]
   * @throws JsonProcessingException
   *           the json processing exception
   * @throws GameBootException
   *           the game boot exception
   */
  public <AGBM extends AbstractGameBootMessage> byte[] toJsonArray(AGBM msg)
      throws JsonProcessingException, GameBootException {
    if (msg == null) fail("No message");

    return mapper.writeValueAsBytes(msg);
  }

  private void fail(String msg, Object...payload) throws GameBootException {
    throw new GameBootException(msg, payload);
  }
}

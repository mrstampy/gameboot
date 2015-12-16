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
package com.github.mrstampy.gameboot.usersession.netty;

import static org.apache.commons.lang3.StringUtils.isNotEmpty;

import java.io.IOException;
import java.lang.invoke.MethodHandles;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.mrstampy.gameboot.messages.AbstractGameBootMessage;
import com.github.mrstampy.gameboot.netty.AbstractGameBootNettyMessageHandler;
import com.github.mrstampy.gameboot.netty.NettyConnectionRegistry;
import com.github.mrstampy.gameboot.usersession.UserSessionConfiguration;
import com.github.mrstampy.gameboot.util.GameBootUtils;
import com.github.mrstampy.gameboot.util.RegistryCleaner;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.SimpleChannelInboundHandler;

/**
 * This handler is intended to be the penultimate handler before an instance of
 * any {@link AbstractGameBootNettyMessageHandler} for
 * {@link AbstractGameBootMessage} subclasses which expose a 'userName' (string)
 * or a 'sessionId' (long). These values are used to add the {@link Channel} to
 * the {@link NettyConnectionRegistry}.<br>
 * <br>
 * 
 * Do not instantiate directly as this is a prototype Spring managed bean. Use
 * {@link GameBootUtils#getBean(Class)} to obtain a unique instance when
 * constructing the {@link ChannelPipeline}.
 */
@Component
@Scope("prototype")
@Profile(UserSessionConfiguration.USER_SESSION_PROFILE)
public class UserSessionInboundHandler extends SimpleChannelInboundHandler<String> {
  private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  /** The Constant USER_NAME. */
  public static final String USER_NAME = "userName";

  /** The Constant SESSION_ID. */
  public static final String SESSION_ID = "sessionId";

  @Autowired
  private ObjectMapper mapper;

  @Autowired
  private RegistryCleaner cleaner;

  /** The user name. */
  protected String userName;

  /** The session id. */
  protected Long sessionId;

  /*
   * (non-Javadoc)
   * 
   * @see
   * io.netty.channel.ChannelInboundHandlerAdapter#channelInactive(io.netty.
   * channel.ChannelHandlerContext)
   */
  @Override
  public void channelInactive(ChannelHandlerContext ctx) throws Exception {
    if (isNotEmpty(userName)) cleaner.cleanup(userName);
    if (sessionId != null) cleaner.cleanup(sessionId);

    cleaner.cleanup(SESSION_ID);

    mapper = null;
    userName = null;
    sessionId = null;
    cleaner = null;

    ctx.fireChannelInactive();
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * io.netty.channel.SimpleChannelInboundHandler#channelRead0(io.netty.channel.
   * ChannelHandlerContext, java.lang.Object)
   */
  @Override
  protected void channelRead0(ChannelHandlerContext ctx, String msg) throws Exception {
    if (isNotEmpty(userName) && sessionId != null) return;

    JsonNode node;
    try {
      node = mapper.readTree(msg);
    } catch (IOException e) {
      log.error("Unexpected exception processing message {} on {}", msg, ctx.channel(), e);
      return;
    }

    if (userName == null && hasValue(node, USER_NAME)) userName = node.get(USER_NAME).asText();

    if (sessionId == null && hasValue(node, SESSION_ID)) sessionId = node.get(SESSION_ID).asLong();
  }

  private boolean hasValue(JsonNode node, String key) {
    return node.has(key) && isNotEmpty(node.get(key).asText());
  }

}

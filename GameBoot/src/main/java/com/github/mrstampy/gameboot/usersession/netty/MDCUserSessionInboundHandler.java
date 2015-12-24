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

import org.slf4j.MDC;
import org.springframework.context.annotation.Profile;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.github.mrstampy.gameboot.usersession.UserSessionConfiguration;
import com.github.mrstampy.gameboot.util.GameBootUtils;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;

/**
 * The Class MDCUserSessionInboundHandler clears the Logback {@link MDC} context
 * and sets the {@link #USER_NAME} (userName) and {@link #SESSION_ID}
 * (sessionId) in the {@link MDC} context should these values have been
 * extracted by the {@link UserSessionInboundHandler} superclass. <br>
 * <br>
 * 
 * Do not instantiate directly as this is a prototype Spring managed bean. Use
 * {@link GameBootUtils#getBean(Class)} to obtain a unique instance when
 * constructing the {@link ChannelPipeline}.
 */
@Component
@Scope("prototype")
@Profile(UserSessionConfiguration.USER_SESSION_PROFILE)
public class MDCUserSessionInboundHandler extends UserSessionInboundHandler {

  /*
   * (non-Javadoc)
   * 
   * @see
   * io.netty.channel.SimpleChannelInboundHandler#channelRead0(io.netty.channel.
   * ChannelHandlerContext, java.lang.Object)
   */
  @Override
  protected void channelRead0(ChannelHandlerContext ctx, String msg) throws Exception {
    super.channelRead0(ctx, msg);

    MDC.clear();
    initMdc();
  }

  private void initMdc() {
    if (isNotEmpty(userName)) MDC.put(USER_NAME, userName);

    if (sessionId != null) MDC.put(SESSION_ID, sessionId.toString());
  }
}

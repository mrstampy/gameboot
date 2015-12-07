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
package com.github.mrstampy.gameboot.usersession;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.github.mrstampy.gameboot.data.entity.UserSession;
import com.github.mrstampy.gameboot.metrics.MetricsHelper;

/**
 * Maintains a map in memory of usernames vs. active session ids for up-to-date
 * checking of active sessions. This class is managed by
 * {@link UserSessionAssist}.
 */
@Component
public class ActiveSessions {

  private static final String ACTIVE_SESSIONS = "ActiveSessions";

  private Map<String, Long> sessions = new ConcurrentHashMap<>();

  @Autowired
  private MetricsHelper helper;

  /**
   * Post construct.
   *
   * @throws Exception
   *           the exception
   */
  public void postConstruct() throws Exception {
    helper.gauge(() -> sessions.size(), ACTIVE_SESSIONS, ActiveSessions.class, "active", "sessions");
  }

  /**
   * Adds the session.
   *
   * @param session
   *          the session
   */
  public void addSession(UserSession session) {
    sessions.put(session.getUser().getUserName(), session.getId());
  }

  /**
   * Checks for session.
   *
   * @param userName
   *          the user name
   * @return true, if successful
   */
  public boolean hasSession(String userName) {
    return sessions.containsKey(userName);
  }

  /**
   * Checks for session.
   *
   * @param id
   *          the id
   * @return true, if successful
   */
  public boolean hasSession(long id) {
    return sessions.containsValue(id);
  }

  /**
   * Removes the session.
   *
   * @param session
   *          the session
   */
  public void removeSession(UserSession session) {
    sessions.remove(session.getUser().getUserName());
  }

  /**
   * Gets the session ids.
   *
   * @return the session ids
   */
  public Collection<Long> getSessionIds() {
    return sessions.values();
  }

  /**
   * Size.
   *
   * @return the int
   */
  public int size() {
    return sessions.size();
  }
}

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
package com.github.mrstampy.gameboot.usersession.data.repository;

import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import com.github.mrstampy.gameboot.usersession.data.entity.User;
import com.github.mrstampy.gameboot.usersession.data.entity.UserSession;

/**
 * The Interface UserSessionRepository.
 */
public interface UserSessionRepository extends CrudRepository<UserSession, Long> {

  /**
   * Find by user and ended is null.
   *
   * @param user
   *          the user
   * @return the user session
   */
  UserSession findByUserAndEndedIsNull(User user);

  /**
   * Find by user name and ended is null.
   *
   * @param userName
   *          the user name
   * @return the user session
   */
  @Query("SELECT us FROM UserSession us JOIN FETCH us.user WHERE us.ended is null AND us.user.userName = :userName")
  UserSession findOpenSession(@Param("userName") String userName);

  /**
   * Find by id and ended is null.
   *
   * @param id
   *          the id
   * @return the user session
   */
  @Query("SELECT us FROM UserSession us JOIN FETCH us.user WHERE us.ended is null AND us.id = :id")
  UserSession findOpenSession(@Param("id") Long id);

  /**
   * Find by ended is null.
   *
   * @return the list
   */
  @Query("SELECT us FROM UserSession us JOIN FETCH us.user WHERE us.ended is null ORDER BY us.created DESC")
  List<UserSession> openSessions();
}

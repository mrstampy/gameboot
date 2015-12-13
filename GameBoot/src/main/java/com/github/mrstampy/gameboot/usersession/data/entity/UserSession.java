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
package com.github.mrstampy.gameboot.usersession.data.entity;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import com.github.mrstampy.gameboot.data.entity.AbstractGameBootEntity;

/**
 * The Class UserSession.
 */
@Entity
@Table
public class UserSession extends AbstractGameBootEntity {

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  private User user;

  @Column
  @Temporal(TemporalType.TIMESTAMP)
  private Date ended;

  /**
   * Gets the user.
   *
   * @return the user
   */
  public User getUser() {
    return user;
  }

  /**
   * Sets the user.
   *
   * @param user
   *          the new user
   */
  public void setUser(User user) {
    this.user = user;
  }

  /**
   * Gets the ended.
   *
   * @return the ended
   */
  public Date getEnded() {
    return ended;
  }

  /**
   * Sets the ended.
   *
   * @param ended
   *          the new ended
   */
  public void setEnded(Date ended) {
    this.ended = ended;
  }
}

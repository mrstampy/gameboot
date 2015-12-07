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
package com.github.mrstampy.gameboot.concurrent;

import java.lang.Thread.UncaughtExceptionHandler;
import java.lang.invoke.MethodHandles;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A configurable thread factory enabling full thread customization.
 */
public class GameBootThreadFactory implements ThreadFactory {
  private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private boolean daemon = true;

  private String name = "GameBoot Thread Factory";

  private ThreadGroup group;

  private AtomicInteger count = new AtomicInteger(1);

  private UncaughtExceptionHandler handler;

  private ClassLoader classLoader;

  /**
   * The Enum Priority.
   */
  public enum Priority {
    /** The max. */
    MAX,
    /** The min. */
    MIN,
    /** The default. */
    DEFAULT;
  }

  private Priority priority = Priority.DEFAULT;

  static {
    Thread.setDefaultUncaughtExceptionHandler(new UncaughtExceptionHandler() {

      @Override
      public void uncaughtException(Thread t, Throwable e) {
        log.error("Uncaught exception in thread {}", t.getName(), e);
      }

    });
  }

  /**
   * Instantiates a new game boot thread factory.
   *
   * @param name
   *          the name
   */
  public GameBootThreadFactory(String name) {
    setName(name);
  }

  /**
   * Instantiates a new game boot thread factory.
   *
   * @param name
   *          the name
   * @param priority
   *          the priority
   */
  public GameBootThreadFactory(String name, Priority priority) {
    setName(name);
    setPriority(priority);
  }

  /*
   * (non-Javadoc)
   * 
   * @see java.util.concurrent.ThreadFactory#newThread(java.lang.Runnable)
   */
  @Override
  public Thread newThread(Runnable r) {
    int nbr = count.getAndIncrement();

    String tn = nbr == 1 ? name : name + "-" + nbr;
    Thread thread = group == null ? new Thread(r, tn) : new Thread(group, r, tn);

    thread.setDaemon(daemon);

    if (handler != null) thread.setUncaughtExceptionHandler(handler);
    if (classLoader != null) thread.setContextClassLoader(classLoader);

    if (priority == null) {
      thread.setPriority(Thread.NORM_PRIORITY);
    } else {
      switch (priority) {
      case MAX:
        thread.setPriority(Thread.MAX_PRIORITY);
        break;
      case MIN:
        thread.setPriority(Thread.MIN_PRIORITY);
        break;
      default:
        thread.setPriority(Thread.NORM_PRIORITY);
        break;
      }
    }

    return thread;
  }

  /**
   * Checks if is daemon.
   *
   * @return true, if is daemon
   */
  public boolean isDaemon() {
    return daemon;
  }

  /**
   * Gets the name.
   *
   * @return the name
   */
  public String getName() {
    return name;
  }

  /**
   * Gets the group.
   *
   * @return the group
   */
  public ThreadGroup getGroup() {
    return group;
  }

  /**
   * Sets the daemon.
   *
   * @param daemon
   *          the new daemon
   */
  public void setDaemon(boolean daemon) {
    this.daemon = daemon;
  }

  /**
   * Sets the name.
   *
   * @param name
   *          the new name
   */
  public void setName(String name) {
    this.name = name;
  }

  /**
   * Sets the group.
   *
   * @param group
   *          the new group
   */
  public void setGroup(ThreadGroup group) {
    this.group = group;
  }

  /**
   * Gets the handler.
   *
   * @return the handler
   */
  public UncaughtExceptionHandler getHandler() {
    return handler;
  }

  /**
   * Sets the handler.
   *
   * @param handler
   *          the new handler
   */
  public void setHandler(UncaughtExceptionHandler handler) {
    this.handler = handler;
  }

  /**
   * Gets the class loader.
   *
   * @return the class loader
   */
  public ClassLoader getClassLoader() {
    return classLoader;
  }

  /**
   * Sets the class loader.
   *
   * @param classLoader
   *          the new class loader
   */
  public void setClassLoader(ClassLoader classLoader) {
    this.classLoader = classLoader;
  }

  /**
   * Gets the priority.
   *
   * @return the priority
   */
  public Priority getPriority() {
    return priority;
  }

  /**
   * Sets the priority.
   *
   * @param priority
   *          the new priority
   */
  public void setPriority(Priority priority) {
    this.priority = priority;
  }

}

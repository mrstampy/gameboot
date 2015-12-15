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
package com.github.mrstampy.gameboot.concurrent;

import static org.apache.commons.lang3.StringUtils.isEmpty;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import javax.annotation.PostConstruct;
import javax.resource.spi.IllegalStateException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ConcurrentTaskExecutor;
import org.springframework.scheduling.concurrent.ConcurrentTaskScheduler;

import com.github.mrstampy.gameboot.util.concurrent.GameBootThreadFactory;

import co.paralleluniverse.fibers.FiberExecutorScheduler;
import co.paralleluniverse.fibers.FiberForkJoinScheduler;

/**
 * Concurrent configuration for GameBoot set in gameboot.properties.
 */
@Configuration
@EnableScheduling
public class GameBootConcurrentConfiguration {

  /** The Constant GAME_BOOT_EXECUTOR. */
  public static final String GAME_BOOT_EXECUTOR = "GameBoot Executor";

  /** The Constant GAME_BOOT_SCHEDULED_EXECUTOR. */
  public static final String GAME_BOOT_SCHEDULED_EXECUTOR = "GameBoot Scheduled Executor";

  /** The Constant GAME_BOOT_TASK_SCHEDULER. */
  public static final String GAME_BOOT_TASK_SCHEDULER = "GameBoot Task Scheduler";

  /** The Constant GAME_BOOT_TASK_EXECUTOR. */
  public static final String GAME_BOOT_TASK_EXECUTOR = "GameBoot Task Executor";

  @Value("${task.scheduler.name}")
  private String taskSchedulerName;

  @Value("${task.scheduler.pool.size}")
  private int taskSchedulerPoolSize;

  @Value("${task.executor.name}")
  private String taskExecutorName;

  @Value("${task.executor.pool.size}")
  private int taskExecutorPoolSize;

  @Value("${pu.fiber.scheduler.name}")
  private String fiberExecutorName;

  @Value("${pu.fiber.scheduler.pool.size}")
  private int fiberPoolSize;

  @Value("${pu.fiber.fj.scheduler.name}")
  private String fiberForkJoinName;

  @Value("${pu.fiber.fj.scheduler.pool.size}")
  private int fiberForkJoinPoolSize;

  @Value("${executor.name}")
  private String executorName;

  @Value("${executor.pool.size}")
  private int executorPoolSize;

  @Value("${scheduler.name}")
  private String schedulerName;

  @Value("${scheduler.pool.size}")
  private int schedulerPoolSize;

  /**
   * Post construct.
   *
   * @throws Exception
   *           the exception
   */
  @PostConstruct
  public void postConstruct() throws Exception {
    checkSize(taskSchedulerPoolSize, "task.scheduler.pool.size");
    checkSize(taskExecutorPoolSize, "task.executor.pool.size");
    checkSize(fiberPoolSize, "pu.fiber.scheduler.pool.size");
    checkSize(fiberForkJoinPoolSize, "pu.fiber.fj.scheduler.pool.size");
    checkSize(executorPoolSize, "executor.pool.size");
    checkSize(schedulerPoolSize, "scheduler.pool.size");
  }

  private void checkSize(int val, String name) throws IllegalStateException {
    if (val <= 0) throw new IllegalStateException(name + " must be > 0");
  }

  /**
   * Task scheduler.
   *
   * @return the task scheduler
   */
  @Bean(name = GAME_BOOT_TASK_SCHEDULER)
  public TaskScheduler taskScheduler() {
    String name = isEmpty(taskSchedulerName) ? "GameBoot Task Scheduler" : taskSchedulerName;

    GameBootThreadFactory factory = new GameBootThreadFactory(name);

    ScheduledExecutorService exe = Executors.newScheduledThreadPool(taskSchedulerPoolSize, factory);

    return new ConcurrentTaskScheduler(exe);
  }

  /**
   * Task executor.
   *
   * @return the task executor
   */
  @Bean(name = GAME_BOOT_TASK_EXECUTOR)
  public TaskExecutor taskExecutor() {
    String name = isEmpty(taskExecutorName) ? "GameBoot Task Executor" : taskExecutorName;

    GameBootThreadFactory factory = new GameBootThreadFactory(name);

    Executor exe = Executors.newFixedThreadPool(taskExecutorPoolSize, factory);

    return new ConcurrentTaskExecutor(exe);
  }

  /**
   * Executor service.
   *
   * @return the executor service
   */
  @Bean(name = GAME_BOOT_EXECUTOR)
  public ExecutorService executorService() {
    String name = isEmpty(executorName) ? "GameBoot Executor" : executorName;

    GameBootThreadFactory factory = new GameBootThreadFactory(name);

    return Executors.newFixedThreadPool(executorPoolSize, factory);
  }

  /**
   * Scheduled executor service.
   *
   * @return the scheduled executor service
   */
  @Bean(name = GAME_BOOT_SCHEDULED_EXECUTOR)
  public ScheduledExecutorService scheduledExecutorService() {
    String name = isEmpty(schedulerName) ? "GameBoot Scheduled Executor" : schedulerName;

    GameBootThreadFactory factory = new GameBootThreadFactory(name);

    return Executors.newScheduledThreadPool(schedulerPoolSize, factory);
  }

  /**
   * Fiber executor scheduler.
   *
   * @return the fiber executor scheduler
   */
  @Bean
  @Primary
  public FiberExecutorScheduler fiberExecutorScheduler() {
    String name = isEmpty(fiberExecutorName) ? "Fiber Scheduler" : fiberExecutorName;

    GameBootThreadFactory factory = new GameBootThreadFactory(name);

    Executor exe = Executors.newFixedThreadPool(fiberPoolSize, factory);

    return new FiberExecutorScheduler(name, exe, null, true);
  }

  /**
   * Fiber fork join scheduler.
   *
   * @return the fiber fork join scheduler
   */
  @Bean
  @Primary
  public FiberForkJoinScheduler fiberForkJoinScheduler() {
    String name = isEmpty(fiberForkJoinName) ? "Fiber Fork Join Scheduler" : fiberForkJoinName;

    return new FiberForkJoinScheduler(name, fiberForkJoinPoolSize, null, true);
  }
}

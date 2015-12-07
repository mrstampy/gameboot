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

import static org.apache.commons.lang3.StringUtils.isEmpty;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ConcurrentTaskExecutor;
import org.springframework.scheduling.concurrent.ConcurrentTaskScheduler;

import co.paralleluniverse.fibers.FiberExecutorScheduler;
import co.paralleluniverse.fibers.FiberForkJoinScheduler;

/**
 * Concurrent configuration for GameBoot.
 */
@Configuration
@EnableScheduling
public class GameBootConcurrentConfiguration {
	
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
	 * Task scheduler.
	 *
	 * @return the task scheduler
	 */
	@Bean
	@Primary
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
	@Bean
	@Primary
	public TaskExecutor taskExecutor() {
		String name = isEmpty(taskExecutorName) ? "GameBoot Task Executor" : taskExecutorName;

		GameBootThreadFactory factory = new GameBootThreadFactory(name);

		Executor exe = Executors.newFixedThreadPool(taskExecutorPoolSize, factory);

		return new ConcurrentTaskExecutor(exe);
	}

	/**
	 * Completable executor service.
	 *
	 * @return the completable executor service
	 */
	@Bean
	@Primary
	public ExecutorService completableExecutorService() {
		String name = isEmpty(executorName) ? "GameBoot Executor" : executorName;

		GameBootThreadFactory factory = new GameBootThreadFactory(name);

		return Executors.newFixedThreadPool(executorPoolSize, factory);
	}

	/**
	 * Completable scheduled executor service.
	 *
	 * @return the completable scheduled executor service
	 */
	@Bean
	@Primary
	public ScheduledExecutorService completableScheduledExecutorService() {
		String name = isEmpty(schedulerName) ? "GameBoot Scheduled Executor" : schedulerName;

		GameBootThreadFactory factory = new GameBootThreadFactory(name);

		return Executors.newScheduledThreadPool(schedulerPoolSize, factory);
	}

	/**
	 * Fiber executor scheduler.
	 *
	 * @return the fiber executor scheduler
	 * @see FiberCreator
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
	 * @see FiberCreator
	 */
	@Bean
	@Primary
	public FiberForkJoinScheduler fiberForkJoinScheduler() {
		String name = isEmpty(fiberForkJoinName) ? "Fiber Fork Join Scheduler" : fiberForkJoinName;

		return new FiberForkJoinScheduler(name, fiberForkJoinPoolSize, null, true);
	}

}

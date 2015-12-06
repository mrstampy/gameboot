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
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ConcurrentTaskExecutor;
import org.springframework.scheduling.concurrent.ConcurrentTaskScheduler;

import co.paralleluniverse.fibers.FiberExecutorScheduler;
import co.paralleluniverse.fibers.FiberForkJoinScheduler;

// TODO: Auto-generated Javadoc
/**
 * The Class ConcurrentConfiguration.
 */
@Configuration
public class GameBootConcurrentConfiguration {

	@Value("${quasar.fiber.scheduler.pool.size}")
	private int poolSize;

	@Value("${quasar.fiber.fj.scheduler.pool.size}")
	private int forkPoolSize;

	@Value("${task.scheduler.pool.size}")
	private int taskSchedulerPoolSize;

	@Value("${task.executor.pool.size}")
	private int taskExecutorPoolSize;

	@Value("${task.scheduler.name}")
	private String taskSchedulerName;

	@Value("${task.executor.name}")
	private String taskExecutorName;

	@Value("${quasar.fiber.scheduler.name}")
	private String fiberExecutorName;

	@Value("${quasar.fiber.fj.scheduler.name}")
	private String fiberForkJoinName;

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
	 * Fiber executor scheduler.
	 *
	 * @return the fiber executor scheduler
	 */
	@Bean
	@Primary
	public FiberExecutorScheduler fiberExecutorScheduler() {
		String name = isEmpty(fiberExecutorName) ? "Fiber Scheduler" : fiberExecutorName;

		GameBootThreadFactory factory = new GameBootThreadFactory(name);

		Executor exe = Executors.newFixedThreadPool(poolSize, factory);

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

		return new FiberForkJoinScheduler(name, forkPoolSize, null, true);
	}

}

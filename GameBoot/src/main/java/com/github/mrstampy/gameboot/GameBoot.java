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
package com.github.mrstampy.gameboot;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Primary;

import com.github.mrstampy.gameboot.concurrent.GameBootThreadFactory;

import co.paralleluniverse.fibers.FiberExecutorScheduler;
import co.paralleluniverse.fibers.FiberForkJoinScheduler;
import co.paralleluniverse.springframework.boot.security.autoconfigure.web.FiberSecureSpringBootApplication;

// TODO: Auto-generated Javadoc
/**
 * The Class GameBoot.
 */
@Configuration
@EnableAutoConfiguration
@ComponentScan(basePackages = "com.github.mrstampy.gameboot")
@EnableAspectJAutoProxy(proxyTargetClass = true)
@EnableCaching
@FiberSecureSpringBootApplication
public class GameBoot {

	@Value("${quasar.fiber.scheduler.pool.size}")
	private int poolSize;

	@Value("${quasar.fiber.fj.scheduler.pool.size}")
	private int forkPoolSize;

	@Bean
	@Primary
	public FiberExecutorScheduler fiberExecutorScheduler() {
		String name = "Fiber Scheduler";
		
		GameBootThreadFactory factory = new GameBootThreadFactory(name);

		Executor exe = Executors.newFixedThreadPool(poolSize, factory);

		return new FiberExecutorScheduler(name, exe, null, true);
	}

	@Bean
	@Primary
	public FiberForkJoinScheduler fiberForkJoinScheduler() {
		String name = "Fiber Fork Join Scheduler";

		return new FiberForkJoinScheduler(name, forkPoolSize, null, true);
	}

	/**
	 * The main method.
	 *
	 * @param args
	 *          the arguments
	 */
	public static void main(String[] args) {
		SpringApplication.run(GameBoot.class, args);
	}

}

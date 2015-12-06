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

import java.util.concurrent.ExecutionException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import co.paralleluniverse.fibers.Fiber;
import co.paralleluniverse.fibers.FiberExecutorScheduler;
import co.paralleluniverse.fibers.FiberForkJoinScheduler;
import co.paralleluniverse.strands.SuspendableCallable;

// TODO: Auto-generated Javadoc
/**
 * The Class GameBootFiberSchedulerExecutor.
 */
@Component
public class GameBootFiberSchedulerExecutor {

	@Autowired
	private FiberExecutorScheduler fiberExecutorScheduler;

	@Autowired
	private FiberForkJoinScheduler fiberForkJoinScheduler;

	/**
	 * Execute in fiber.
	 *
	 * @param <T>
	 *          the generic type
	 * @param c
	 *          the c
	 * @return the t
	 * @throws ExecutionException
	 *           the execution exception
	 * @throws InterruptedException
	 *           the interrupted exception
	 */
	public <T> T executeInFiber(SuspendableCallable<T> c) throws ExecutionException, InterruptedException {
		Fiber<T> fiber = fiberExecutorScheduler.newFiber(c);

		return fiber.start().get();
	}

	/**
	 * Execute in fork join.
	 *
	 * @param <T>
	 *          the generic type
	 * @param c
	 *          the c
	 * @return the t
	 * @throws ExecutionException
	 *           the execution exception
	 * @throws InterruptedException
	 *           the interrupted exception
	 */
	public <T> T executeInForkJoin(SuspendableCallable<T> c) throws ExecutionException, InterruptedException {
		Fiber<T> fiber = fiberForkJoinScheduler.newFiber(c);

		return fiber.start().get();
	}
}

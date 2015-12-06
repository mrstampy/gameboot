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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import co.paralleluniverse.fibers.Fiber;
import co.paralleluniverse.fibers.FiberExecutorScheduler;
import co.paralleluniverse.fibers.FiberForkJoinScheduler;
import co.paralleluniverse.strands.SuspendableCallable;
import co.paralleluniverse.strands.SuspendableRunnable;

// TODO: Auto-generated Javadoc
/**
 * The Class FiberCreator.
 */
@Component
public class FiberCreator {

	@Autowired
	private FiberExecutorScheduler fiberExecutorScheduler;

	@Autowired
	private FiberForkJoinScheduler fiberForkJoinScheduler;

	/**
	 * New fiber.
	 *
	 * @param <T>
	 *          the generic type
	 * @return the fiber
	 */
	public <T> Fiber<T> newFiber() {
		return new Fiber<>(fiberExecutorScheduler);
	}

	/**
	 * New fiber.
	 *
	 * @param <T>
	 *          the generic type
	 * @param sr
	 *          the sr
	 * @return the fiber
	 */
	public <T> Fiber<T> newFiber(SuspendableRunnable sr) {
		return new Fiber<>(fiberExecutorScheduler, sr);
	}

	/**
	 * New fiber.
	 *
	 * @param <T>
	 *          the generic type
	 * @param name
	 *          the name
	 * @param sr
	 *          the sr
	 * @return the fiber
	 */
	public <T> Fiber<T> newFiber(String name, SuspendableRunnable sr) {
		return new Fiber<>(name, fiberExecutorScheduler, sr);
	}

	/**
	 * New fiber.
	 *
	 * @param <T>
	 *          the generic type
	 * @param fiber
	 *          the fiber
	 * @param sr
	 *          the sr
	 * @return the fiber
	 */
	public <T> Fiber<T> newFiber(Fiber<T> fiber, SuspendableRunnable sr) {
		return new Fiber<>(fiber, fiberExecutorScheduler, sr);
	}

	/**
	 * New fiber.
	 *
	 * @param <T>
	 *          the generic type
	 * @param sr
	 *          the sr
	 * @return the fiber
	 */
	public <T> Fiber<T> newFiber(SuspendableCallable<T> sr) {
		return new Fiber<>(fiberExecutorScheduler, sr);
	}

	/**
	 * New fiber.
	 *
	 * @param <T>
	 *          the generic type
	 * @param name
	 *          the name
	 * @param sr
	 *          the sr
	 * @return the fiber
	 */
	public <T> Fiber<T> newFiber(String name, SuspendableCallable<T> sr) {
		return new Fiber<>(name, fiberExecutorScheduler, sr);
	}

	/**
	 * New fiber.
	 *
	 * @param <T>
	 *          the generic type
	 * @param fiber
	 *          the fiber
	 * @param sr
	 *          the sr
	 * @return the fiber
	 */
	public <T> Fiber<T> newFiber(Fiber<T> fiber, SuspendableCallable<T> sr) {
		return new Fiber<>(fiber, fiberExecutorScheduler, sr);
	}

	/**
	 * New fiber fork join.
	 *
	 * @param <T>
	 *          the generic type
	 * @return the fiber
	 */
	public <T> Fiber<T> newFiberForkJoin() {
		return new Fiber<>(fiberForkJoinScheduler);
	}

	/**
	 * New fiber fork join.
	 *
	 * @param <T>
	 *          the generic type
	 * @param sr
	 *          the sr
	 * @return the fiber
	 */
	public <T> Fiber<T> newFiberForkJoin(SuspendableRunnable sr) {
		return new Fiber<>(fiberForkJoinScheduler, sr);
	}

	/**
	 * New fiber fork join.
	 *
	 * @param <T>
	 *          the generic type
	 * @param name
	 *          the name
	 * @param sr
	 *          the sr
	 * @return the fiber
	 */
	public <T> Fiber<T> newFiberForkJoin(String name, SuspendableRunnable sr) {
		return new Fiber<>(name, fiberForkJoinScheduler, sr);
	}

	/**
	 * New fiber fork join.
	 *
	 * @param <T>
	 *          the generic type
	 * @param fiber
	 *          the fiber
	 * @param sr
	 *          the sr
	 * @return the fiber
	 */
	public <T> Fiber<T> newFiberForkJoin(Fiber<T> fiber, SuspendableRunnable sr) {
		return new Fiber<>(fiber, fiberForkJoinScheduler, sr);
	}

	/**
	 * New fiber fork join.
	 *
	 * @param <T>
	 *          the generic type
	 * @param sr
	 *          the sr
	 * @return the fiber
	 */
	public <T> Fiber<T> newFiberForkJoin(SuspendableCallable<T> sr) {
		return new Fiber<>(fiberForkJoinScheduler, sr);
	}

	/**
	 * New fiber fork join.
	 *
	 * @param <T>
	 *          the generic type
	 * @param name
	 *          the name
	 * @param sr
	 *          the sr
	 * @return the fiber
	 */
	public <T> Fiber<T> newFiberForkJoin(String name, SuspendableCallable<T> sr) {
		return new Fiber<>(name, fiberForkJoinScheduler, sr);
	}

	/**
	 * New fiber fork join.
	 *
	 * @param <T>
	 *          the generic type
	 * @param fiber
	 *          the fiber
	 * @param sr
	 *          the sr
	 * @return the fiber
	 */
	public <T> Fiber<T> newFiberForkJoin(Fiber<T> fiber, SuspendableCallable<T> sr) {
		return new Fiber<>(fiber, fiberForkJoinScheduler, sr);
	}
}

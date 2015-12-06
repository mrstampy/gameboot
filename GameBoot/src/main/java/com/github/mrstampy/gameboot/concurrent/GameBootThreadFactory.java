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

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

// TODO: Auto-generated Javadoc
/**
 * A factory for creating GameBootThread objects.
 */
public class GameBootThreadFactory implements ThreadFactory {

	private boolean daemon = true;

	private String name;

	private ThreadGroup group;

	private AtomicInteger count = new AtomicInteger(1);

	/**
	 * Instantiates a new game boot thread factory.
	 */
	public GameBootThreadFactory() {
	}

	/**
	 * Instantiates a new game boot thread factory.
	 *
	 * @param name
	 *          the name
	 */
	public GameBootThreadFactory(String name) {
		this.name = name;
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

}

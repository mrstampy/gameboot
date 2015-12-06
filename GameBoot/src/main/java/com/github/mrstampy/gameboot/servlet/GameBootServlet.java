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
package com.github.mrstampy.gameboot.servlet;

import java.io.IOException;

import javax.annotation.PostConstruct;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.embedded.ServletRegistrationBean;
import org.springframework.web.servlet.DispatcherServlet;

import co.paralleluniverse.fibers.Suspendable;
import co.paralleluniverse.fibers.servlet.FiberHttpServlet;

// TODO: Auto-generated Javadoc
/**
 * The Class GameBootServlet.
 */
@WebServlet
public class GameBootServlet extends FiberHttpServlet {

	private static final long serialVersionUID = 5717054170484010018L;

	private DispatcherServlet dispatcherServlet;

	/**
	 * Post construct.
	 *
	 * @throws Exception
	 *           the exception
	 */
	@PostConstruct
	public void postConstruct() throws Exception {
		ServletRegistrationBean bean = new ServletRegistrationBean(this, "/*");

		bean.setName("GameBootServlet");
		bean.setOrder(1);
		bean.setEnabled(true);
	}

	/**
	 * Sets the dispatcher servlet.
	 *
	 * @param dispatcherServlet
	 *          the new dispatcher servlet
	 */
	@Autowired
	public void setDispatcherServlet(DispatcherServlet dispatcherServlet) {
		this.dispatcherServlet = dispatcherServlet;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * co.paralleluniverse.fibers.servlet.FiberHttpServlet#service(javax.servlet.
	 * http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
	 */
	@Suspendable
	@Override
	protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		dispatcherServlet.service(req, resp);
	}

}

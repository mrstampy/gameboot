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
package com.github.mrstampy.gameboot.processor;

import javax.annotation.PostConstruct;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.codahale.metrics.Timer.Context;
import com.github.mrstampy.gameboot.messages.Response;
import com.github.mrstampy.gameboot.metrics.MetricsHelper;

// TODO: Auto-generated Javadoc
/**
 * The Class GameBootProcessorAspect.
 */
@Aspect
@Component
public class GameBootProcessorAspect {

	private static final String PROCESS_TIMER = "ProcessTimer";

	private static final String FAILED_REQUESTS = "FailedRequests";

	private static final String ALERT_REQUESTS = "AlertRequests";

	private static final String INFO_REQUESTS = "InfoRequests";

	private static final String SUCCESS_REQUESTS = "SuccessRequests";

	private static final String WARNING_REQUESTS = "WarningRequests";

	@Autowired
	private MetricsHelper helper;

	/**
	 * Post construct.
	 *
	 * @throws Exception
	 *           the exception
	 */
	@PostConstruct
	public void postConstruct() throws Exception {
		helper.timer(PROCESS_TIMER, AbstractGameBootProcessor.class, "process", "timer");
		helper.counter(FAILED_REQUESTS, AbstractGameBootProcessor.class, "failed", "requests");
		helper.counter(ALERT_REQUESTS, AbstractGameBootProcessor.class, "alert", "requests");
		helper.counter(INFO_REQUESTS, AbstractGameBootProcessor.class, "info", "requests");
		helper.counter(SUCCESS_REQUESTS, AbstractGameBootProcessor.class, "success", "requests");
		helper.counter(WARNING_REQUESTS, AbstractGameBootProcessor.class, "warning", "requests");
	}

	/**
	 * Metrics.
	 *
	 * @param pjp
	 *          the pjp
	 * @return the object
	 * @throws Throwable
	 *           the throwable
	 */
	@Around("this(com.github.mrstampy.gameboot.processor.GameBootProcessor) && execution(com.github.mrstampy.gameboot.messages.Response *.*(..))")
	public Object metrics(ProceedingJoinPoint pjp) throws Throwable {
		Context ctx = helper.startTimer(PROCESS_TIMER);

		try {
			Response r = (Response) pjp.proceed();

			switch (r.getResponseCode()) {
			case FAILURE:
				helper.incr(FAILED_REQUESTS);
				break;
			case ALERT:
				helper.incr(ALERT_REQUESTS);
				break;
			case INFO:
				helper.incr(INFO_REQUESTS);
				break;
			case SUCCESS:
				helper.incr(SUCCESS_REQUESTS);
				break;
			case WARNING:
				helper.incr(WARNING_REQUESTS);
				break;
			default:
				break;
			}

			return r;
		} finally {
			ctx.stop();
		}
	}
}

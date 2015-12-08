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

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebFilter;

import org.slf4j.MDC;
import org.springframework.core.annotation.Order;

import ch.qos.logback.classic.helpers.MDCInsertingServletFilter;

/**
 * Ensures the Logback {@link MDC} context is initialized with information from
 * the request:<br>
 * <br>
 *
 * <table>
 * <tr>
 * <th>MDC key</th>
 * <th>MDC value</th>
 * </tr>
 *
 * <tr class="alt">
 * <td><b>req.remoteHost</b></td>
 * <td>as returned by the <a href=
 * "http://java.sun.com/j2ee/sdk_1.3/techdocs/api/javax/servlet/ServletRequest.html#getRemoteHost%28%29">
 * getRemoteHost()</a> method</td>
 * </tr>
 *
 * <tr >
 * <td><b>req.xForwardedFor</b></td>
 * <td>value of the
 * <a href="http://en.wikipedia.org/wiki/X-Forwarded-For">"X-Forwarded-For"</a>
 * header</td>
 * </tr>
 *
 * <tr class="alt">
 * <td><b>req.method</b></td>
 * <td>as returned by <a href=
 * "http://java.sun.com/j2ee/sdk_1.3/techdocs/api/javax/servlet/http/HttpServletRequest.html#getMethod%28%29">
 * getMethod()</a> method</td>
 * </tr>
 *
 * <tr>
 * <td><b>req.requestURI</b></td>
 * <td>as returned by <a href=
 * "http://java.sun.com/j2ee/sdk_1.3/techdocs/api/javax/servlet/http/HttpServletRequest.html#getRequestURI%28%29">
 * getRequestURI()</a> method</td>
 * </tr>
 *
 * <tr class="alt">
 * <td><b>req.requestURL</b></td>
 * <td>as returned by <a href=
 * "http://java.sun.com/j2ee/sdk_1.3/techdocs/api/javax/servlet/http/HttpServletRequest.html#getRequestURL%28%29">
 * getRequestURL()</a> method</td>
 * </tr>
 *
 * <tr>
 * <td><b>req.queryString</b></td>
 * <td>as returned by <a href=
 * "http://java.sun.com/j2ee/sdk_1.3/techdocs/api/javax/servlet/http/HttpServletRequest.html#getQueryString%28%29">
 * getQueryString()</a> method</td>
 * </tr>
 *
 * <tr class="alt">
 * <td><b>req.userAgent</b></td>
 * <td>value of the "User-Agent" header</td>
 * </tr>
 *
 * </table>
 *
 * <br>
 * 
 * More information from the
 * <a href="http://logback.qos.ch/manual/mdc.html#mis">Logback Manual</a>
 */
@WebFilter
@Order(1)
public class GameBootMDCFilter extends MDCInsertingServletFilter {

  /*
   * (non-Javadoc)
   * 
   * @see
   * ch.qos.logback.classic.helpers.MDCInsertingServletFilter#doFilter(javax.
   * servlet.ServletRequest, javax.servlet.ServletResponse,
   * javax.servlet.FilterChain)
   */
  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
      throws IOException, ServletException {
    super.doFilter(request, response, chain);
  }

}

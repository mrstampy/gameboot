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
 * Copyright (C) 2015, 2016 Burton Alexander
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

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.web.socket.config.annotation.EnableWebSocket;

import com.github.mrstampy.gameboot.data.GameBootDataConfiguration;
import com.github.mrstampy.gameboot.locale.processor.LocaleProcessor;
import com.github.mrstampy.gameboot.locale.processor.LocaleRegistry;
import com.github.mrstampy.gameboot.messages.AbstractGameBootMessage;
import com.github.mrstampy.gameboot.messages.Response;
import com.github.mrstampy.gameboot.messages.Response.ResponseCode;
import com.github.mrstampy.gameboot.messages.context.ResponseContext;
import com.github.mrstampy.gameboot.messages.context.ResponseContextLoader;
import com.github.mrstampy.gameboot.messages.context.ResponseContextLookup;
import com.github.mrstampy.gameboot.messages.finder.MessageClassFinder;
import com.github.mrstampy.gameboot.metrics.MetricsHelper;
import com.github.mrstampy.gameboot.netty.AbstractNettyMessageHandler;
import com.github.mrstampy.gameboot.netty.AbstractNettyProcessor;
import com.github.mrstampy.gameboot.otp.OtpConfiguration;
import com.github.mrstampy.gameboot.processor.AbstractGameBootProcessor;
import com.github.mrstampy.gameboot.processor.AbstractTransactionalGameBootProcessor;
import com.github.mrstampy.gameboot.processor.GameBootProcessor;
import com.github.mrstampy.gameboot.processor.GameBootProcessorAspect;
import com.github.mrstampy.gameboot.processor.connection.ConnectionProcessor;
import com.github.mrstampy.gameboot.security.SecurityConfiguration;
import com.github.mrstampy.gameboot.systemid.SystemId;
import com.github.mrstampy.gameboot.usersession.UserSessionAssist;
import com.github.mrstampy.gameboot.usersession.UserSessionConfiguration;
import com.github.mrstampy.gameboot.usersession.messages.UserMessage;
import com.github.mrstampy.gameboot.web.WebProcessor;
import com.github.mrstampy.gameboot.websocket.AbstractGameBootWebSocketHandler;
import com.github.mrstampy.gameboot.websocket.AbstractWebSocketProcessor;

import co.paralleluniverse.springframework.boot.security.autoconfigure.web.FiberSecureSpringBootApplication;

/**
 * <h1>GameBoot</h1>
 * <h2>A gaming template server architecture</h2>
 * 
 * GameBoot is designed the purpose of creating multi-player games implemented
 * to run in a browser or as stand-alone clients. Once coded configuration of
 * the server is done via property files. The architecture is not game specific,
 * rather it is a JSON string and JSON binary processing engine augmented with
 * complimentary technologies that can be extended to process any new messages
 * as required. JSON messages are of the required form: <br>
 * <br>
 * 
 * 1. An implementation of {@link AbstractGameBootMessage} is created for all
 * new message types sent from the client to the server for processing.<br>
 * 2. A {@link Response} message (or no message) is sent back to the client for
 * messages processed.<br>
 * <br>
 * 
 * The interface {@link GameBootProcessor} is implemented for each
 * {@link AbstractGameBootMessage}, processing the message and returning the
 * {@link Response}.<br>
 * <br>
 * 
 * The interface {@link ConnectionProcessor} ({@link WebProcessor},
 * {@link AbstractNettyProcessor}, {@link AbstractWebSocketProcessor}) provides
 * context around incoming messages, making available the context object,
 * setting and management of {@link SystemId}s, processing of
 * {@link Response#getMappingKeys()} and any other contextual processing.<br>
 * <br>
 * 
 * Technologies have been included to assist with the rapid processing of high
 * message volumes, making the architecture scalable to many thousands of
 * concurrent users per instance. At the center of the architecture all that is
 * required to process new messages is:<br>
 * <br>
 * 
 * 1. Extend the GameBoot architecture by implementing a subclass of
 * {@link AbstractGameBootMessage} representing the new JSON message. This
 * message is sent from the client to the server for processing. Of importance
 * is to ensure that the {@link AbstractGameBootMessage#getType()} returns a
 * GameBoot-unique string, hereafter referred to as <b>The Type</b>.<br>
 * <br>
 * 
 * 2. Implement a {@link GameBootProcessor} (or extend one of
 * {@link AbstractGameBootProcessor} or
 * {@link AbstractTransactionalGameBootProcessor}) to process your message.
 * Ensure that the {@link GameBootProcessor#getType()} implementation returns
 * <b>The Type</b> of your new message. An {@link AbstractGameBootMessage} will
 * have only one {@link GameBootProcessor} related by <b>The Type</b>. <br>
 * <br>
 * 
 * 3. Implement the {@link MessageClassFinder} interface to include <b>The
 * Type</b>-to- {@link AbstractGameBootMessage} message class mapping for all
 * new messages. <br>
 * <br>
 * 
 * 4. Ensure the {@link ConnectionProcessor} related to the application's
 * {@link AbstractGameBootMessage#getTransport()} ({@link WebProcessor},
 * {@link AbstractNettyProcessor}, {@link AbstractWebSocketProcessor}) can
 * process the application's messages.<br>
 * <br>
 * 
 * 5. Create {@link Configuration}s aware of new classes and functionalities as
 * required by your application and a main class to start and wire together your
 * application.<br>
 * <br>
 * 
 * <h2>GameBoot Technologies</h2><br>
 * 
 * GameBoot is coded upon the
 * <a href="http://projects.spring.io/spring-boot/">Spring Boot</a> application
 * framework using <a href="http://java.oracle.com">Java 8</a> and as such any
 * implementation of a GameBoot server can easily include and is not limited to
 * any of the functionalities of a Spring Boot application. The technologies
 * included with GameBoot are:<br>
 * <br>
 * 
 * 1. <a href="http://projects.spring.io/spring-data-jpa/">Spring JPA</a> with a
 * choice of backing datastore of
 * <a href="http://www.h2database.com/html/main.html">H2</a>,
 * <a href="https://db.apache.org/derby/">Derby</a>,
 * <a href="https://www.mysql.com/">MySQL</a> or
 * <a href="http://www.postgresql.org/">Postgres</a>. (
 * {@link GameBootDataConfiguration})<br>
 * 2. <a href="http://hibernate.org/orm/envers/">Automatic Auditing</a> of
 * datastore records as required.<br>
 * 3. <a href=
 * "http://docs.spring.io/spring/docs/current/spring-framework-reference/html/cache.html">
 * Spring Caching</a> using <a href="http://www.ehcache.org/">EhCache</a> as a
 * <a href="https://github.com/jsr107/jsr107spec">JSR-107</a> cache provider (
 * {@link UserSessionAssist#activeSessions()}).<br>
 * 4. <a href="https://dropwizard.github.io/metrics/3.1.0/">Metrics
 * gathering</a> ({@link MetricsHelper})<br>
 * 5. <a href=
 * "http://docs.spring.io/spring/docs/current/spring-framework-reference/html/aop.html">
 * Spring AOP</a> ({@link GameBootProcessorAspect})<br>
 * 6. <a href=
 * "http://docs.spring.io/spring/docs/current/spring-framework-reference/html/mvc.html">
 * Spring Web</a><br>
 * 7. <a href="http://projects.spring.io/spring-security/">Spring Security</a>
 * <br>
 * 8. <a href=
 * "http://docs.spring.io/spring/docs/current/spring-framework-reference/html/websocket.html">
 * Spring Web Sockets</a> ({@link AbstractGameBootWebSocketHandler} and
 * {@link AbstractWebSocketProcessor})<br>
 * 9. <a href="http://netty.io/">Netty</a> (
 * {@link AbstractNettyMessageHandler} and
 * {@link AbstractNettyProcessor})<br>
 * 10. <a href="http://docs.paralleluniverse.co/comsat/">Comsat</a> to assist
 * web application development for high volume messages.<br>
 * 11. <a href="http://docs.paralleluniverse.co/quasar/">Quasar</a> to process
 * high volume messages.<br>
 * <br>
 * 
 * <h2>Application Property Files</h2> <br>
 * 
 * There are several configuration files required by a GameBoot implementation:
 * <br>
 * <br>
 * 
 * 1. application.properties - implementation supplied and can contain any
 * additional <a href=
 * "http://docs.spring.io/spring-boot/docs/current/reference/html/common-application-properties.html">
 * Spring Boot configuration properties</a> and application-specific properties.
 * <br>
 * 2. gameboot.properties - the main configuration file for a GameBoot server.
 * <br>
 * 3. database.properties - required if using {@link GameBootDataConfiguration},
 * properties can be included in application.properties otherwise.<br>
 * 4. logback.groovy - <a href="http://logback.qos.ch/">Logback
 * configuration</a><br>
 * 5. security.properties - required if using {@link SecurityConfiguration},
 * properties can be included in application.properties otherwise.<br>
 * 6. error.properties - required if using the default implementations of the
 * {@link ResponseContextLoader} and {@link ResponseContextLookup} interfaces.
 * <br>
 * 7. otp.properties - if including the OTP functionality (
 * {@link OtpConfiguration}).<br>
 * 8. ehcache.xml - if caching is enabled. The location of this file is
 * specified in gameboot.properties<br>
 * 9. gameboot.sql - if present this file will be used to initialize the
 * database on startup. For development and testing purposes.<br>
 * <br>
 * 
 * With the exception of application.properties, logback.groovy and ehcache.xml
 * property files are scanned for in the following order:<br>
 * <br>
 * 
 * 1. On the filesystem at the location the application was started.<br>
 * 2. In a '/gameboot' package on the classpath.<br>
 * 3. On the root of the classpath.<br>
 * 
 * <h2>Error Messages</h2> <br>
 * 
 * The default implementations of the {@link ResponseContextLoader} and
 * {@link ResponseContextLookup} interfaces expect an <b>'error.properties'</b>
 * file to be available. While intended for error message context any contextual
 * messages can be added which complement any of the {@link ResponseCode}s of
 * responses sent to the client. The default implementations facilitate the
 * rapid addition of new response contexts which can be definitively mapped on
 * the receiving client. The {@link ResponseContext} part of message processing
 * is returned to the client in the {@link Response} as appropriate.<br>
 * <br>
 * 
 * <h3>Internationalization and Parameters</h3><br>
 * 
 * The default implementation of the {@link ResponseContextLoader} and
 * {@link ResponseContextLookup} use the file <b>'error.properties'</b> as the
 * fallback for the creation of {@link ResponseContext} objects.
 * Internationalization is as easy as adding additional error property files
 * named using the {@link java.util.ResourceBundle} naming - an <b>'error_[lang
 * code]_[country code].properties'</b> or an <b>'error_[lang
 * code].properties'</b> file with messages appropriately translated. By default
 * a {@link Locale} object is associated with a {@link SystemId} generated value
 * in the {@link LocaleRegistry} and is used to return a localized
 * {@link ResponseContext}.<br>
 * <br>
 * 
 * Message descriptions can be parameterized using
 * {@link java.text.MessageFormat} notation and passing the parameters into the
 * {@link ResponseContextLookup}. The '{' and '}' brackets must be escaped in
 * the file ie. <b>my.new.message.description=\{0\} is not a \{1\}</b><br>
 * <br>
 * 
 * The property <b>'game.boot.additional.locales'</b> exists to be able to add
 * locales not included with Java.<br>
 * <br>
 * 
 * <h2>Example Applications</h2> <br>
 * 
 * To assist with the development of the architecture three mini-applications
 * were developed concurrently.<br>
 * <br>
 * 
 * 1. The '<b>usersession</b>' application ({@link UserSessionConfiguration})
 * processes {@link UserMessage}s to manage a simple
 * login/logout/creation/maintenance/game-specific session creation for a
 * client. This mini-app has a backing datastore and uses caching for the
 * retrieval of online user sessions.<br>
 * <br>
 * 
 * 2. The '<b>otp</b>' application ({@link OtpConfiguration}) is an
 * implementation of the
 * <a href="https://en.wikipedia.org/wiki/One-time_pad">One Time Pad</a>
 * encryption algorithm designed to provide a high level of encryption on clear
 * channels, bypassing the overhead of SSL/TLS for fast message processing
 * without sacrificing security.<br>
 * <br>
 * 
 * 3. The '<b>locale</b>' application ({@link LocaleProcessor}) to demonstrate
 * {@link Locale} switching in memory.<br>
 * <br>
 * 
 * These applications are available when the profiles ('usersession', 'otp' and
 * 'locale') are active.
 */
@Configuration
@EnableAutoConfiguration
@ComponentScan(basePackages = "com.github.mrstampy.gameboot")
@EnableAspectJAutoProxy(proxyTargetClass = true)
@FiberSecureSpringBootApplication
@EnableWebSocket
public class GameBoot {
  private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  /**
   * Use 'generate' as the first command line argument to write the necessary
   * GameBoot resources to the file system.
   */
  public static final String GENERATE = "generate";

  /**
   * The main method.
   *
   * @param args
   *          the arguments
   * @throws Exception
   *           the exception
   */
  public static void main(String[] args) throws Exception {
    ConfigurableApplicationContext ctx = SpringApplication.run(GameBoot.class, args);

    log.warn("GameBoot is being run in demonstration mode. It is intended to be used as a server library.");
    log.debug(
        "Add 'generate' as the first command line argument to write GameBoot's configuration files to the file system.");

    if (args == null || args.length == 0) return;

    if (GENERATE.equals(args[0])) generatePropertyFiles(ctx);
  }

  private static void generatePropertyFiles(ConfigurableApplicationContext ctx) throws IOException {
    GameBootDependencyWriter writer = ctx.getBean(GameBootDependencyWriter.class);

    writer.writeDependencies(ctx);

    ctx.close();
  }

}

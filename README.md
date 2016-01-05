# GameBoot

The one commonality with servers providing services for many clients is the processing of messages and communication between connected clients.  GameBoot strives to fulfil this requirement by providing an easy to use, robust and consistant framework for message processing - with a few bells and whistles.  JSON messages of any sort*, for any requirement, received via the web, web sockets or a [Netty](http://netty.io) connection.

Rather than create a generic message processing library GameBoot's dependencies include all the technologies necessary to create a high performant [Spring Boot-based](http://projects.spring.io/spring-boot/) server application running with Java 8.  With the exception of Spring Boot any use of GameBoot does not limit one to these technologies, and technologies not required can be excluded.  Refer to the GameBoot Javadoc for detailed information of the included technologies.

An understanding of the Spring Framework and Spring Boot is advantageous for using GameBoot, however there are several implementation examples** within the library itself and the unit tests demonstrate how the major components of GameBoot function.  Further detail on specifics is included in the [main class.](https://github.com/mrstampy/gameboot/blob/master/GameBoot/src/main/java/com/github/mrstampy/gameboot/GameBoot.java)

## Messages*

All GameBoot messages are JSON, delivered to and from the client as strings or byte arrays as required.  GameBoot messages all share the same basic structure, represented by the class [AbstractGameBootMessage](https://github.com/mrstampy/gameboot/blob/master/GameBoot/src/main/java/com/github/mrstampy/gameboot/messages/AbstractGameBootMessage.java):

	{
	  "id": integer value, optional but recommended to match responses,
	  "type": GameBoot-implementation unique string, mandatory
	}

Specific messages (based on type) can have any additional fields or arrays as required and are received by the GameBoot implementation from connected clients for processing.  Messages sent to the client are of a single structure, represented by the class [Response](https://github.com/mrstampy/gameboot/blob/master/GameBoot/src/main/java/com/github/mrstampy/gameboot/messages/Response.java):

	{
	  "id" : integer value, from the received message,

	  "type" : the type of the received message if known, 'RESPONSE' otherwise,

	  "responseCode" : one of SUCCESS, FAILURE, WARNING, INFO, ALERT, CRITICAL,

	  "context" : { // see error.properties, optional
	    "code": integer value,
	    "function": string identifying function
	    "description": context around the response, human readable
	  },

	  "payload" : [array of either JSON strings, plain strings or byte arrays, optional]
	}

Messages are converted to Java objects based on the implementation of the [MessageClassFinder](https://github.com/mrstampy/gameboot/blob/master/GameBoot/src/main/java/com/github/mrstampy/gameboot/messages/finder/MessageClassFinder.java) interface.  Inbound messages are processed by an implementation of [GameBootProcessor](https://github.com/mrstampy/gameboot/blob/master/GameBoot/src/main/java/com/github/mrstampy/gameboot/processor/GameBootProcessor.java) whose type matches the type of the message.

That's GameBoot in a nutshell.

## Transportation

The function of getting messages to GameBootProcessors and returning responses falls to implementations of [ConnectionProcessor](https://github.com/mrstampy/gameboot/blob/master/GameBoot/src/main/java/com/github/mrstampy/gameboot/processor/connection/ConnectionProcessor.java).  There are three transport-specific implementations:

1. [WebProcessor](https://github.com/mrstampy/gameboot/blob/master/GameBoot/src/main/java/com/github/mrstampy/gameboot/web/WebProcessor.java) for messages received via the web.
2. [AbstractWebSocketProcessor](https://github.com/mrstampy/gameboot/blob/master/GameBoot/src/main/java/com/github/mrstampy/gameboot/websocket/AbstractWebSocketProcessor.java) for messages received via web sockets.
3. [AbstractNettyProcessor](https://github.com/mrstampy/gameboot/blob/master/GameBoot/src/main/java/com/github/mrstampy/gameboot/netty/AbstractNettyProcessor.java) for messages received via Netty.

## [WebProcessor](https://github.com/mrstampy/gameboot/blob/master/GameBoot/src/main/java/com/github/mrstampy/gameboot/web/WebProcessor.java) Usage

All GameBoot messages received via the web are processed as per this pseudo code:

	@RestController
	public class MyRestController {

	  @Autowired
	  private WebProcessor processor;

	  @Autowired
	  private GameBootMessageConverter converter; // one could use Spring's converters to do this and return the Response

	  @AnnotatedMethodToReceivePostedJson
	  public String process(HttpSession session, String message) throws Exception {
	    processor.onConnection(session);

	    Response response = processor.process(session, message);

	    return converter.toJson(response);
	  }
	}

## [WebSocketProcessor](https://github.com/mrstampy/gameboot/blob/master/GameBoot/src/main/java/com/github/mrstampy/gameboot/websocket/AbstractWebSocketProcessor.java) Usage

Each GameBoot web socket is a subclass of [AbstractWebSocketHandler](https://github.com/mrstampy/gameboot/blob/master/GameBoot/src/main/java/com/github/mrstampy/gameboot/websocket/AbstractWebSocketHandler.java) and uses a subclass of [AbstractWebSocketProcessor](https://github.com/mrstampy/gameboot/blob/master/GameBoot/src/main/java/com/github/mrstampy/gameboot/websocket/AbstractWebSocketProcessor.java) for message handling.  Wire up the web socket as per the Spring Boot documentation and start the server.  No extra code required.

## [NettyProcessor](https://github.com/mrstampy/gameboot/blob/master/GameBoot/src/main/java/com/github/mrstampy/gameboot/netty/AbstractNettyProcessor.java) Usage

As the premiere Java socket library Netty's inclusion in GameBoot is intended to facilitate both web and non-web connections to the server - UDP, SPDY, async sockets, all that Netty supports.  Each GameBoot Netty connection uses a subclass of [AbstractNettyHandler](https://github.com/mrstampy/gameboot/blob/master/GameBoot/src/main/java/com/github/mrstampy/gameboot/netty/AbstractNettyHandler.java) as the last handler in the channel's pipeline.  Like with web sockets it is paired with a subclass of [AbstractNettyProcessor](https://github.com/mrstampy/gameboot/blob/master/GameBoot/src/main/java/com/github/mrstampy/gameboot/netty/AbstractNettyProcessor.java).  An understanding of Netty's handlers, pipelines, bootstraps is necessary to use Netty and its use within Spring does require some scaffolding code, however the [Netty OTP unit tests](https://github.com/mrstampy/gameboot/tree/master/GameBoot/src/test/java/com/github/mrstampy/gameboot/otp/netty) for GameBoot demonstrate how this is done.  Once configured start the server and the sockets will process and return responses for received messages.

## Bells and Whistles

1. Internationalization built in, Locale based.  The 'context' of Responses is created in the default implementation from an ['error.properties'](https://github.com/mrstampy/gameboot/blob/master/GameBoot/src/main/resources/error.properties) file.  Copies of this file named according to Java's ResourceBundle rules allows GameBoot to support multiple languages.  This functionality also includes parameterization of response context descriptions using Java's MessageFormat class.

2. [Registries](https://github.com/mrstampy/gameboot/blob/master/GameBoot/src/main/java/com/github/mrstampy/gameboot/util/registry) for ease of lookups for connection-transient objects, primarily intended to [facilitate sending messages between connected clients](https://github.com/mrstampy/gameboot/blob/master/GameBoot/src/main/java/com/github/mrstampy/gameboot/messaging/MessagingGroups.java) but which can be used for any connection-related purpose.

3. Ability to [group connections](https://github.com/mrstampy/gameboot/blob/master/GameBoot/src/main/java/com/github/mrstampy/gameboot/messaging/MessagingGroups.java) for the purpose of sending messages.

4. [Runnables and Callables](https://github.com/mrstampy/gameboot/blob/master/GameBoot/src/main/java/com/github/mrstampy/gameboot/util/concurrent) which preserve the Logback mapped diagnostic context.

5. [MetricsHelper](https://github.com/mrstampy/gameboot/blob/master/GameBoot/src/main/java/com/github/mrstampy/gameboot/metrics) to assist with the creation and usage of Metrics objects.

6. Detailed logging, avoiding logging of sensitive message detail.

7. Sensible defaults with ease of exclusion or overriding. 

## Implementation Examples**

These examples included with GameBoot coupled with the related unit tests conceptually demonstrate how the GameBoot architecture can be used, and can be used by any implementation.

1. The ['usersession'](https://github.com/mrstampy/gameboot/tree/master/GameBoot/src/main/java/com/github/mrstampy/gameboot/usersession) application processes [UserMessages](https://github.com/mrstampy/gameboot/blob/master/GameBoot/src/main/java/com/github/mrstampy/gameboot/usersession/messages/UserMessage.java) to manage a simple login/logout/creation/maintenance/game-specific session creation for a client. This mini-app has a backing datastore and uses JSR-107 caching for the retrieval of online user sessions.

2. The ['otp'](https://github.com/mrstampy/gameboot/tree/master/GameBoot/src/main/java/com/github/mrstampy/gameboot/otp) application is an implementation of the [One Time Pad](https://en.wikipedia.org/wiki/One-time_pad) encryption algorithm designed to provide a high level of encryption on clear channels, bypassing the overhead of SSL/TLS for fast message processing without sacrificing security.

3. The ['locale'](https://github.com/mrstampy/gameboot/tree/master/GameBoot/src/main/java/com/github/mrstampy/gameboot/locale) application to demonstrate Locale switching in memory.

These applications are available when the profiles ('usersession', 'otp' and 'locale') are active.


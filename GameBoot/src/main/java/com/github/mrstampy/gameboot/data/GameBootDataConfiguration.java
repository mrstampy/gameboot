package com.github.mrstampy.gameboot.data;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * <a href=
 * "http://docs.spring.io/spring-boot/docs/current/reference/html/howto-database-initialization.html#howto-initialize-a-database-using-spring-jdbc">
 * Data initialization</a>
 * 
 * @author burton
 *
 */
@Configuration
@EnableJpaRepositories(basePackages = "com.github.mrstampy.gameboot.data")
@EnableJpaAuditing
public class GameBootDataConfiguration {

}

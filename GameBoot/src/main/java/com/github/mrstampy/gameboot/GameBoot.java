package com.github.mrstampy.gameboot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@Configuration
@EnableAutoConfiguration
@ComponentScan(basePackages = "com.github.mrstampy.gameboot")
@EnableAspectJAutoProxy(proxyTargetClass=true)
public class GameBoot {

	public static void main(String[] args) {
		SpringApplication.run(GameBoot.class, args);
	}

}

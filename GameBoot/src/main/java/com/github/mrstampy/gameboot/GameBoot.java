package com.github.mrstampy.gameboot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Primary;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;

import com.fasterxml.jackson.databind.ObjectMapper;

@Configuration
@EnableAutoConfiguration
@ComponentScan(basePackages = "com.github.mrstampy.gameboot")
@EnableAspectJAutoProxy(proxyTargetClass = true)
@EnableCaching
public class GameBoot {

	@Bean
	@Primary
	public ObjectMapper mapper() {
		ObjectMapper mapper = new ObjectMapper();

		return mapper;
	}

	@Bean
	public MappingJackson2HttpMessageConverter converter() {
		MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter(mapper());

		return converter;
	}

	public static void main(String[] args) {
		SpringApplication.run(GameBoot.class, args);
	}

}

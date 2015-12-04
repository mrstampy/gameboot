package com.github.mrstampy.gameboot;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

@Configuration
@EnableAutoConfiguration
@ComponentScan(basePackages = "com.github.mrstampy.gameboot")
@EnableAspectJAutoProxy(proxyTargetClass = true)
public class GameBoot {

	@Value("${json.pretty.print}")
	private boolean prettyPrint;

	@Bean
	public ObjectMapper mapper() {
		ObjectMapper mapper = new ObjectMapper();

		if (prettyPrint) mapper.enable(SerializationFeature.INDENT_OUTPUT);

		return mapper;
	}

	@Bean
	public MappingJackson2HttpMessageConverter converter() {
		MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter(mapper());

		converter.setPrettyPrint(prettyPrint);

		return converter;
	}

	public static void main(String[] args) {
		SpringApplication.run(GameBoot.class, args);
	}

}

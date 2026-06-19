package com.bank.customer.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class SetuConfig {

	@Bean
	public WebClient webClient(WebClient.Builder builder) {
		return builder.build();
	}
}

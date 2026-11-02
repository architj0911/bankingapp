package com.bank.transaction.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class ExchangeRateConfig {
    @Bean
    RestClient exchangeRateRestClient(@Value("${exchange-rate.base-url:https://open.er-api.com/v6/latest}") String baseUrl) {
        return RestClient.builder().baseUrl(baseUrl).build();
    }
}
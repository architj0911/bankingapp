package com.bank.authservice.config;

import java.time.Duration;
import java.util.UUID;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.server.authorization.client.InMemoryRegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;

@Configuration
public class ClientConfig {

	@Bean
	public RegisteredClientRepository registeredClientRepository(PasswordEncoder passwordEncoder) {
		RegisteredClient bankingClient =  RegisteredClient.withId(UUID.randomUUID().toString()).clientId("api-gateway").clientSecret(passwordEncoder.encode("secret123"))
				.authorizationGrantType( new AuthorizationGrantType("otp"))
				.authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
		.authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
		.scope("accounts.read").tokenSettings(TokenSettings.builder().accessTokenTimeToLive(java.time.Duration.ofHours(3)).refreshTokenTimeToLive(Duration.ofDays(7))
				.reuseRefreshTokens(false).build()).build();
		return new InMemoryRegisteredClientRepository(bankingClient);
	}
}

package com.bank.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtGrantedAuthoritiesConverterAdapter;
import org.springframework.security.web.server.SecurityWebFilterChain;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

	@Bean
	public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
		System.out.println("this is SecurityWebFilterChain of securityConfig class");
		return http.csrf(ServerHttpSecurity.CsrfSpec::disable)
				.authorizeExchange(auth -> auth.pathMatchers("/public/**").permitAll()
						.pathMatchers("/kyc/callback").permitAll()
						.pathMatchers("/api/admin/**").hasRole("ADMIN").anyExchange().authenticated())
				 .oauth2ResourceServer(oauth -> oauth.jwt(jwt ->
	               jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))) // ye add kar
	          .build();
	}

    @Bean
    public ReactiveJwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter grantedAuthoritiesConverter = new JwtGrantedAuthoritiesConverter();
        grantedAuthoritiesConverter.setAuthorityPrefix(""); // ROLE_ already hai token me
        grantedAuthoritiesConverter.setAuthoritiesClaimName("authorities"); // ye important

        ReactiveJwtAuthenticationConverter jwtAuthenticationConverter = new ReactiveJwtAuthenticationConverter();
        jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(
            new ReactiveJwtGrantedAuthoritiesConverterAdapter(grantedAuthoritiesConverter));
        return jwtAuthenticationConverter;
    }
}

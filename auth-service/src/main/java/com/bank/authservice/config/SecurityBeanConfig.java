package com.bank.authservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;


import com.bank.authservice.service.CustomUserDetailService;

import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
public class SecurityBeanConfig {
	private final CustomUserDetailService userDetailsService;
	@Bean
	PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}
	@Bean
	JwtAuthenticationConverter jwtAuthenticationConverter() {
		JwtGrantedAuthoritiesConverter grantedAuthoritiesConverter = new JwtGrantedAuthoritiesConverter();
		grantedAuthoritiesConverter.setAuthorityPrefix(""); // ROLE_ prefix already hai token me
		grantedAuthoritiesConverter.setAuthoritiesClaimName("authorities"); // ye line important hai
		
		JwtAuthenticationConverter jwtAuthenticationConverter = new JwtAuthenticationConverter();
		jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(grantedAuthoritiesConverter);
		return jwtAuthenticationConverter;
	}
	@Bean
	AuthenticationManager authenticationManager(
	        HttpSecurity http,
	        PasswordEncoder passwordEncoder) throws Exception {

	    AuthenticationManagerBuilder builder =
	            http.getSharedObject(AuthenticationManagerBuilder.class);

	    builder
	        .userDetailsService(userDetailsService)
	        .passwordEncoder(passwordEncoder);

	    return builder.build();
	}

	@Bean
	@Order(2)
	SecurityFilterChain defaultSecurityFilterChain(HttpSecurity http) throws Exception {

		http
		.csrf(csrf -> csrf.disable())
		  .authorizeHttpRequests(auth -> auth
		      .requestMatchers("/user/register", "/auth/**").permitAll()
		      .requestMatchers("/admin/**").hasRole("ADMIN")
		      .anyRequest().authenticated()
		  )
		  .oauth2ResourceServer(oauth2 -> oauth2
		           .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
		           );
	    return http.build();
	}
}

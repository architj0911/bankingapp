package com.bank.authservice.config;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.OAuth2Token;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configuration.OAuth2AuthorizationServerConfiguration;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configurers.OAuth2AuthorizationServerConfigurer;
import org.springframework.security.oauth2.server.authorization.token.DelegatingOAuth2TokenGenerator;
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext;
import org.springframework.security.oauth2.server.authorization.token.JwtGenerator;
import org.springframework.security.oauth2.server.authorization.token.OAuth2AccessTokenGenerator;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenCustomizer;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenGenerator;
import org.springframework.security.web.SecurityFilterChain;

import com.bank.authservice.domain.User;
import com.bank.authservice.repository.UserRepository;
import com.bank.authservice.security.OtpAuthenticationConverter;
import com.bank.authservice.security.OtpAuthenticationProvider;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;

import lombok.RequiredArgsConstructor;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class AuthorizationServerConfig {
private final UserRepository userRepo;
	@Bean
	@Order(-1)
	SecurityFilterChain authServerSecurityFilterchain(HttpSecurity http,
			OtpAuthenticationConverter otpConverter,
			OtpAuthenticationProvider otpProvider) throws Exception{
		OAuth2AuthorizationServerConfigurer configurer = new OAuth2AuthorizationServerConfigurer();
		http.securityMatcher(configurer.getEndpointsMatcher()).with(configurer, authServer -> authServer
				.tokenEndpoint(tokenEndpoint -> tokenEndpoint.accessTokenRequestConverter(otpConverter)
						.authenticationProvider(otpProvider))).csrf(csrf -> csrf.disable())
		.authorizeHttpRequests(auth -> auth.anyRequest().authenticated());
		
		return http.build();
	}
	@Bean
	public OAuth2TokenGenerator<? extends OAuth2Token> tokenGenerator(JWKSource<SecurityContext> jwkSource,
			OAuth2TokenCustomizer<JwtEncodingContext> customizer){
		JwtGenerator jwtGenerator = new JwtGenerator(new NimbusJwtEncoder(jwkSource));
		jwtGenerator.setJwtCustomizer(customizer);
		return new DelegatingOAuth2TokenGenerator(jwtGenerator, new OAuth2AccessTokenGenerator());
	}
	@Bean
	public OAuth2TokenCustomizer<JwtEncodingContext> tokenCostomizer(){
		return context -> {
			if(OAuth2TokenType.ACCESS_TOKEN.equals(context.getTokenType())) {
				Authentication principal =  context.getPrincipal();
				if(principal.getPrincipal() instanceof UserDetails user) {
					User dbUser =  userRepo.findByUserName(user.getUsername()).orElseThrow(()-> new RuntimeException("User not found"));
				context.getClaims().claim("userId", dbUser.getId());
				List<String> authorities = user.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList();
				context.getClaims().claim("authorities", authorities);
				}
			}
		};
	}
}









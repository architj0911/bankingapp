package com.bank.authservice.config;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.UUID;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configuration.OAuth2AuthorizationServerConfiguration;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;

@Configuration
public class JwkConfig {
	@Bean
	JWKSource<SecurityContext> jwkSource() throws Exception{
		KeyPairGenerator generator =  KeyPairGenerator.getInstance("RSA");
		generator.initialize(2048);
		KeyPair keyPair =  generator.generateKeyPair();
		RSAPublicKey publickey = (RSAPublicKey) keyPair.getPublic();
		RSAPrivateKey privateKey = (RSAPrivateKey) keyPair.getPrivate();
		RSAKey rsakey = new RSAKey.Builder(publickey).privateKey(privateKey).keyID(UUID.randomUUID().toString()).build();
		JWKSet jwkSet = new JWKSet(rsakey);
		return new ImmutableJWKSet<>(jwkSet);
	}
	@Bean 
	public JwtDecoder jwtDecoder(JWKSource<SecurityContext> jwkSource) { 
	    return OAuth2AuthorizationServerConfiguration.jwtDecoder(jwkSource); 
	} 


}

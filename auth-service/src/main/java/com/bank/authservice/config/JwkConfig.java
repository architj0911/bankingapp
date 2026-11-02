package com.bank.authservice.config;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyFactory;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configuration.OAuth2AuthorizationServerConfiguration;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;

@Configuration
public class JwkConfig {
	@Bean
	JWKSource<SecurityContext> jwkSource(
			@Value("${JWT_RSA_PRIVATE_KEY_BASE64:}") String privateKeyBase64,
			@Value("${JWT_RSA_PUBLIC_KEY_BASE64:}") String publicKeyBase64,
			@Value("${JWT_KEY_ID:local-ephemeral}") String keyId) throws Exception {
		RSAPublicKey publickey;
		RSAPrivateKey privateKey;
		if (privateKeyBase64.isBlank() && publicKeyBase64.isBlank()) {
			KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
			generator.initialize(2048);
			KeyPair keyPair = generator.generateKeyPair();
			publickey = (RSAPublicKey) keyPair.getPublic();
			privateKey = (RSAPrivateKey) keyPair.getPrivate();
		} else {
			if (privateKeyBase64.isBlank() || publicKeyBase64.isBlank()) {
				throw new IllegalStateException("Both JWT RSA key environment variables must be configured together");
			}
			KeyFactory keyFactory = KeyFactory.getInstance("RSA");
			privateKey = (RSAPrivateKey) keyFactory.generatePrivate(new PKCS8EncodedKeySpec(Base64.getDecoder().decode(privateKeyBase64)));
			publickey = (RSAPublicKey) keyFactory.generatePublic(new X509EncodedKeySpec(Base64.getDecoder().decode(publicKeyBase64)));
		}
		RSAKey rsakey = new RSAKey.Builder(publickey).privateKey(privateKey).keyID(keyId.isBlank() ? UUID.randomUUID().toString() : keyId).build();
		JWKSet jwkSet = new JWKSet(rsakey);
		return new ImmutableJWKSet<>(jwkSet);
	}
	@Bean
	public JwtEncoder jwtEncoder(JWKSource<SecurityContext> jwkSource) {
		return new NimbusJwtEncoder(jwkSource);
	}

	@Bean
	public JwtDecoder jwtDecoder(JWKSource<SecurityContext> jwkSource) {
		return OAuth2AuthorizationServerConfiguration.jwtDecoder(jwkSource);
	} 


}

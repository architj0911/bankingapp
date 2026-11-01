package com.bank.customer.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

public class SecurityUtil {
public static Jwt getJwt() {
	Authentication auth = SecurityContextHolder.getContext().getAuthentication();
	if(auth== null || !(auth instanceof JwtAuthenticationToken)) {
		throw new RuntimeException("Invalid Authentication for Jwt not found");
	}
	JwtAuthenticationToken jwtAuth = (JwtAuthenticationToken) auth;
	return jwtAuth.getToken();
}
public static Long getUserId() {
	Jwt jwt = getJwt();
	Object userId = jwt.getClaim("userId");
	if(userId== null) {
		throw new RuntimeException("user id Claim not found");
	}
	return Long.valueOf(userId.toString());
}
public static String getUserName() {
	Jwt jwt = getJwt();
	return jwt.getSubject();			
}
}

package com.bank.transaction.util;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

public class SecurityUtil {
public static Long getUserId() {
	Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
	if(authentication == null || !(authentication.getPrincipal() instanceof Jwt)) {
		return null;
	}
	Jwt jwt = (Jwt) authentication.getPrincipal();
	return jwt.getClaim("userId");
}
}

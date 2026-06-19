package com.bank.authservice.security;

import java.util.Map;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.AuthenticationConverter;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;

@Component
public class OtpAuthenticationConverter implements AuthenticationConverter{

	@Override
	public Authentication convert(HttpServletRequest request) {
		String grantType = request.getParameter("grant_type");
		String ip = request.getRemoteAddr();
		String deviceId = request.getHeader("X-Device-Id");
		if(!"otp".equals(grantType)) {
			return null;
		}
		String username = request.getParameter("username");
		String otp = request.getParameter("otp");
		Authentication clientPrincipal = SecurityContextHolder.getContext().getAuthentication();
		return new OtpAuthenticationToken(clientPrincipal, null, Map.of("username", username,
				"otp", otp,
				"ip", ip,
				"deviceId", deviceId));
	}

}

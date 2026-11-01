package com.bank.authservice.security;

import java.util.Map;
import java.util.Set;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AuthorizationGrantAuthenticationToken;

public class OtpAuthenticationToken extends OAuth2AuthorizationGrantAuthenticationToken{
	private final String username;
	private final String otp;
	private final String ip;
	private final String deviceId;
	public OtpAuthenticationToken(Authentication clientPrincipal,
			Set<String> scopes,
			Map<String, Object> additionalParametes) {
		super(new AuthorizationGrantType("otp"), clientPrincipal, additionalParametes);
		this.username = (String) additionalParametes.get("username");
		this.otp = (String) additionalParametes.get("otp");
		this.ip = (String) additionalParametes.get("ip");
		this.deviceId = (String) additionalParametes.get("deviceId");
	}
	public String getUsername() {
		return username;
	}
	public String getOtp() {
		return otp;
	}
	public String getIp() {
		return ip;
	}
	public String getDeviceId() {
		return deviceId;
	}
}

package com.bank.authservice.security;

import org.eclipse.angus.mail.iap.BadCommandException;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2Token;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AccessTokenAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2ClientAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.context.AuthorizationServerContextHolder;
import org.springframework.security.oauth2.server.authorization.token.DefaultOAuth2TokenContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenGenerator;
import org.springframework.stereotype.Component;

import com.bank.authservice.service.CustomUserDetailService;
import com.bank.authservice.service.OtpService;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class OtpAuthenticationProvider implements AuthenticationProvider{
	private final OtpService otpService;
	private final CustomUserDetailService userDetailService;
	private final OAuth2TokenGenerator<? extends OAuth2Token> tokenGenerator;
	@Override
	public Authentication authenticate(Authentication authentication) throws AuthenticationException {
		OtpAuthenticationToken otpAuthenticationToken = (OtpAuthenticationToken) authentication;
		String username = otpAuthenticationToken.getUsername();
		String otp = otpAuthenticationToken.getOtp();
		String ip = otpAuthenticationToken.getIp();
		String deviceId = otpAuthenticationToken.getDeviceId();
		if(!otpService.verifuOtp(username, otp, ip, deviceId)) {
			throw new BadCredentialsException("Invalid Otp");
		}
		UserDetails user = userDetailService.loadUserByUsername(username);
		DefaultOAuth2TokenContext tokenContext = DefaultOAuth2TokenContext.builder()
				.registeredClient(((OAuth2ClientAuthenticationToken) otpAuthenticationToken.getPrincipal()).getRegisteredClient())
				.principal(new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities()))
				.authorizationServerContext(AuthorizationServerContextHolder.getContext())
				.tokenType(OAuth2TokenType.ACCESS_TOKEN)
				.authorizationGrantType(new AuthorizationGrantType("otp"))
				.authorizationGrant(otpAuthenticationToken).build();
		OAuth2Token generatedToken = this.tokenGenerator.generate(tokenContext);
		OAuth2AccessToken accessToken  = new OAuth2AccessToken(
				OAuth2AccessToken.TokenType.BEARER,
				generatedToken.getTokenValue(),
				generatedToken.getIssuedAt(),
				generatedToken.getExpiresAt(),
				tokenContext.getAuthorizedScopes());
		return new OAuth2AccessTokenAuthenticationToken(((OAuth2ClientAuthenticationToken) otpAuthenticationToken.getPrincipal()).getRegisteredClient(), (Authentication) otpAuthenticationToken.getPrincipal(), accessToken);
				
		//return new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
	}
	@Override
	public boolean supports(Class<?> authentication) {
		return OtpAuthenticationToken.class.isAssignableFrom(authentication);
		
	}
}

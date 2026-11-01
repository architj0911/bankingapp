package com.bank.authservice.service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import com.bank.authservice.domain.User;
import com.bank.authservice.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AccessTokenService {
	private final JwtEncoder jwtEncoder;
	private final UserRepository userRepo;

	public String issueAccessToken(UserDetails userDetails) {
		String username = userDetails.getUsername();
		User dbUser = userRepo.findByUserNameIgnoreCase(username.trim())
				.orElseGet(() -> userRepo.findByUserName(username.trim())
						.orElseThrow(() -> new IllegalStateException("User not found")));

		List<String> authorities = userDetails.getAuthorities().stream()
				.map(GrantedAuthority::getAuthority)
				.toList();

		Instant now = Instant.now();
		JwtClaimsSet claims = JwtClaimsSet.builder()
				.issuer("http://auth-service:9000")
				.subject(dbUser.getUserName())
				.issuedAt(now)
				.expiresAt(now.plus(3, ChronoUnit.HOURS))
				.claim("userId", dbUser.getId())
				.claim("authorities", authorities)
				.build();

		return jwtEncoder.encode(JwtEncoderParameters.from(
				JwsHeader.with(SignatureAlgorithm.RS256).build(),
				claims)).getTokenValue();
	}
}

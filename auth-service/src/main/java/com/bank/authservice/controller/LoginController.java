package com.bank.authservice.controller;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2Token;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.context.AuthorizationServerContext;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.token.DefaultOAuth2TokenContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenGenerator;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.bank.authservice.domain.User;
import com.bank.authservice.repository.UserRepository;
import com.bank.authservice.service.AuditService;
import com.bank.authservice.service.CustomUserDetailService;
import com.bank.authservice.service.EmailService;
import com.bank.authservice.service.OtpService;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping({"/auth", "/api/auth"})
@RequiredArgsConstructor
@Slf4j
public class LoginController {

	private final AuditService auditService;
	private final UserRepository userRepo;
	private final PasswordEncoder passwordEncoder;
	private final OtpService otpService;
	private final EmailService emailService;
	private final CustomUserDetailService userDetailService;
	private final RegisteredClientRepository registeredClientRepository;
	private final OAuth2TokenGenerator<? extends OAuth2Token> tokenGenerator;

	@PostMapping("/login-success")
	public void loginSuccess(@RequestParam String userName, HttpServletRequest request) {
		auditService.logSuccess(userName, request.getRemoteAddr());
	}

	@PostMapping("/login-failure")
	public void loginFailure(@RequestParam String userName, @RequestParam String reason, HttpServletRequest request) {
		auditService.logFailure(userName, request.getRemoteAddr(), reason);
	}

	// STEP 1: Validate Credentials & Dispatch Real Email OTP
	@PostMapping("/login")
	public ResponseEntity<?> login(
			@RequestBody(required = false) Map<String, String> body,
			@RequestParam(required = false) String userName,
			@RequestParam(required = false) String username,
			@RequestParam(required = false) String password,
			HttpServletRequest request) {
		try {
			String effectiveUser = userName != null ? userName : (username != null ? username : (body != null ? (body.get("userName") != null ? body.get("userName") : body.get("username")) : ""));
			String effectivePass = password != null ? password : (body != null ? body.get("password") : "");
			String cleanUserName = effectiveUser != null ? effectiveUser.trim() : "";

			if (cleanUserName.isEmpty() || effectivePass == null || effectivePass.isEmpty()) {
				return ResponseEntity.badRequest().body(Map.of("message", "Username and password are required"));
			}

			// Find user by username or email
			User user = userRepo.findByUserNameIgnoreCase(cleanUserName)
					.or(() -> userRepo.findByUserName(cleanUserName))
					.or(() -> userRepo.findByEmailIgnoreCase(cleanUserName))
					.orElseThrow(() -> new RuntimeException("Invalid username or password"));

			if (!user.isAccountNonLocked()) {
				if (user.getLockTime() != null && user.getLockTime().plusMinutes(15).isBefore(LocalDateTime.now())) {
					user.setAccountNonLocked(true);
					user.setFailedAttempts(0);
					userRepo.save(user);
				} else {
					auditService.logFailure(cleanUserName, request.getRemoteAddr(), "Account locked");
					return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "Account locked. Try after 15 minutes"));
				}
			}

			if (!passwordEncoder.matches(effectivePass, user.getPassword())) {
				user.setFailedAttempts(user.getFailedAttempts() + 1);
				if (user.getFailedAttempts() >= 5) {
					user.setAccountNonLocked(false);
					user.setLockTime(LocalDateTime.now());
				}
				userRepo.save(user);
				auditService.logFailure(cleanUserName, request.getRemoteAddr(), "Invalid credentials");
				return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Invalid username or password"));
			}

			// Reset failed attempts on valid credentials
			user.setFailedAttempts(0);
			userRepo.save(user);

			if (user.getEmail() == null || user.getEmail().trim().isEmpty()) {
				return ResponseEntity.badRequest().body(Map.of("message", "No registered email address found for this account. Contact bank support."));
			}

			String ip = request.getRemoteAddr();
			String deviceId = request.getHeader("X-Device-Id");
			String challengeId = UUID.randomUUID().toString();

			// Generate and store OTP hash in Redis
			String otp = otpService.createLoginChallenge(challengeId, user.getUserName(), user.getEmail(), ip, deviceId);

			// Real email dispatch via Gmail SMTP
			try {
				emailService.sendLoginOtp(user.getEmail(), otp);
			} catch (Exception mailEx) {
				log.error("Failed to deliver login OTP via SMTP: {}", mailEx.getMessage());
				return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
						.body(Map.of("message", "Failed to deliver OTP to your email inbox. Please check mail server settings."));
			}

			auditService.logEvent("LOGIN_OTP_SENT", user.getUserName(), user.getId(), ip, true, "Login OTP sent to email");

			// Return temporary authentication challenge identifier, NOT a JWT
			return ResponseEntity.ok(Map.of(
				"status", "OTP_REQUIRED",
				"challengeId", challengeId,
				"maskedEmail", maskEmail(user.getEmail()),
				"message", "Verification code sent to your registered email"
			));
		} catch (Exception ex) {
			log.warn("Login failed: {}", ex.getMessage());
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Invalid username or password"));
		}
	}

	// STEP 2: Verify OTP and Issue JWT
	@PostMapping({"/login/verify-otp", "/verify-login-otp"})
	public ResponseEntity<?> verifyOtp(
			@RequestBody(required = false) Map<String, String> body,
			@RequestParam(required = false) String challengeId,
			@RequestParam(required = false) String otp,
			HttpServletRequest request) {
		String effChallengeId = challengeId != null ? challengeId : (body != null ? body.get("challengeId") : null);
		String effOtp = otp != null ? otp : (body != null ? body.get("otp") : null);

		if (effChallengeId == null || effOtp == null || effOtp.trim().isEmpty()) {
			return ResponseEntity.badRequest().body(Map.of("message", "Challenge ID and OTP are required"));
		}

		String username = otpService.getChallengeUsername(effChallengeId);
		if (username == null) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Session expired or invalid. Please sign in again."));
		}

		boolean verified = otpService.verifyLoginChallenge(effChallengeId, effOtp.trim());
		if (!verified) {
			auditService.logFailure(username, request.getRemoteAddr(), "Invalid or expired OTP");
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Invalid or expired verification code"));
		}

		UserDetails userDetails = userDetailService.loadUserByUsername(username);
		User user = userRepo.findByUserNameIgnoreCase(username).orElse(null);
		Long userId = user != null ? user.getId() : null;

		RegisteredClient client = registeredClientRepository.findByClientId("api-gateway");
		if (client == null) {
			client = registeredClientRepository.findById("api-gateway");
		}
		if (client == null) {
			throw new IllegalStateException("OAuth client configuration is unavailable");
		}

		DefaultOAuth2TokenContext tokenContext = DefaultOAuth2TokenContext.builder()
				.registeredClient(client)
				.principal(new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities()))
				.authorizationServerContext(new AuthorizationServerContext() {
					private final AuthorizationServerSettings settings = AuthorizationServerSettings.builder()
							.issuer("http://auth-service:9000").build();
					@Override public String getIssuer() { return settings.getIssuer(); }
					@Override public AuthorizationServerSettings getAuthorizationServerSettings() { return settings; }
				})
				.tokenType(OAuth2TokenType.ACCESS_TOKEN)
				.authorizationGrantType(new AuthorizationGrantType("otp"))
				.authorizationGrant(new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities()))
				.authorizedScopes(client.getScopes())
				.build();

		OAuth2Token generatedToken = this.tokenGenerator.generate(tokenContext);
		if (generatedToken == null) {
			throw new IllegalStateException("Failed to generate access token");
		}

		auditService.logEvent("LOGIN_OTP_VERIFIED", username, userId, request.getRemoteAddr(), true, "Login OTP verified");
		auditService.logSuccess(username, request.getRemoteAddr());

		return ResponseEntity.ok(Map.of(
			"token_type", "Bearer",
			"access_token", generatedToken.getTokenValue(),
			"expires_in", 10800,
			"status", "SUCCESS"
		));
	}

	// Resend Login OTP
	@PostMapping({"/login/resend-otp", "/resend-login-otp"})
	public ResponseEntity<?> resendOtp(
			@RequestBody(required = false) Map<String, String> body,
			@RequestParam(required = false) String challengeId,
			HttpServletRequest request) {
		String effChallengeId = challengeId != null ? challengeId : (body != null ? body.get("challengeId") : null);
		if (effChallengeId == null) {
			return ResponseEntity.badRequest().body(Map.of("message", "Challenge ID is required"));
		}

		try {
			String newOtp = otpService.resendLoginChallenge(effChallengeId);
			String email = otpService.getChallengeEmail(effChallengeId);
			String username = otpService.getChallengeUsername(effChallengeId);

			if (email == null || email.trim().isEmpty()) {
				return ResponseEntity.badRequest().body(Map.of("message", "Registered email not found for this challenge. Please log in again."));
			}

			emailService.sendLoginOtp(email, newOtp);
			auditService.logEvent("LOGIN_OTP_SENT", username, null, request.getRemoteAddr(), true, "Login OTP resent to email");

			return ResponseEntity.ok(Map.of(
				"status", "OTP_RESENT",
				"message", "A new verification code has been sent to your email"
			));
		} catch (IllegalStateException e) {
			return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(Map.of("message", e.getMessage()));
		} catch (Exception e) {
			return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
		}
	}

	// Forgot Password — Step 1: Send OTP to registered email
	@PostMapping("/forgot-password")
	public ResponseEntity<?> forgotPassword(
			@RequestBody(required = false) Map<String, String> body,
			@RequestParam(required = false) String email,
			HttpServletRequest request) {
		String effEmail = email != null ? email : (body != null ? body.get("email") : null);
		if (effEmail == null || effEmail.trim().isEmpty()) {
			return ResponseEntity.badRequest().body(Map.of("message", "Registered email address is required"));
		}

		User user = userRepo.findByEmailIgnoreCase(effEmail.trim()).orElse(null);
		if (user == null) {
			// Do not leak existence, but return consistent message
			return ResponseEntity.ok(Map.of(
				"status", "OTP_REQUIRED",
				"message", "If an account with that email exists, a password reset code has been sent."
			));
		}

		try {
			String otp = otpService.generatePasswordResetOtp(user.getEmail());
			emailService.sendPasswordResetOtp(user.getEmail(), otp);
			auditService.logEvent("PASSWORD_RESET_OTP_SENT", user.getUserName(), user.getId(), request.getRemoteAddr(), true, "Password reset OTP sent");
			return ResponseEntity.ok(Map.of(
				"status", "OTP_REQUIRED",
				"maskedEmail", maskEmail(user.getEmail()),
				"message", "Password reset verification code sent to your registered email"
			));
		} catch (IllegalStateException e) {
			return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(Map.of("message", e.getMessage()));
		} catch (Exception e) {
			log.error("Forgot password OTP dispatch failed: {}", e.getMessage());
			return ResponseEntity.badRequest().body(Map.of("message", "Unable to send password reset code"));
		}
	}

	// Forgot Password — Step 2: Verify OTP
	@PostMapping("/verify-password-reset-otp")
	public ResponseEntity<?> verifyPasswordResetOtp(
			@RequestBody(required = false) Map<String, String> body,
			@RequestParam(required = false) String email,
			@RequestParam(required = false) String otp,
			HttpServletRequest request) {
		String effEmail = email != null ? email : (body != null ? body.get("email") : null);
		String effOtp = otp != null ? otp : (body != null ? body.get("otp") : null);

		if (effEmail == null || effOtp == null) {
			return ResponseEntity.badRequest().body(Map.of("message", "Email and OTP are required"));
		}

		boolean verified = otpService.verifyPasswordResetOtp(effEmail.trim(), effOtp.trim());
		if (!verified) {
			return ResponseEntity.badRequest().body(Map.of("status", "FAILED", "message", "Invalid or expired password reset code"));
		}

		auditService.logEvent("PASSWORD_RESET_OTP_VERIFIED", effEmail.trim(), null, request.getRemoteAddr(), true, "Password reset OTP verified");
		return ResponseEntity.ok(Map.of(
			"status", "VERIFIED",
			"message", "Password reset code verified. You may now update your password."
		));
	}

	private String maskEmail(String email) {
		if (email == null || email.indexOf('@') <= 0) return "***";
		int at = email.indexOf('@');
		String userPart = email.substring(0, at);
		String domain = email.substring(at);
		String maskedUser = userPart.length() <= 2 ? userPart.charAt(0) + "***" : userPart.substring(0, 2) + "****";
		return maskedUser + domain;
	}
}

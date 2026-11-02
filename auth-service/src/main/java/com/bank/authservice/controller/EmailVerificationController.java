package com.bank.authservice.controller;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.bank.authservice.service.AuditService;
import com.bank.authservice.service.EmailService;
import com.bank.authservice.service.OtpService;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping({"/auth", "/api/auth", "/user/email", "/kyc/email"})
@RequiredArgsConstructor
@Slf4j
public class EmailVerificationController {

    private final OtpService otpService;
    private final EmailService emailService;
    private final AuditService auditService;

    // Send Registration Email OTP
    @PostMapping({"/send-email-otp", "/resend-email-otp", "/send-otp"})
    public ResponseEntity<?> sendEmailOtp(
            @RequestBody(required = false) Map<String, String> body,
            @RequestParam(required = false) String email,
            HttpServletRequest request) {
        String effEmail = email != null ? email : (body != null ? body.get("email") : null);
        if (effEmail == null || effEmail.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Email address is required"));
        }

        try {
            String otp = otpService.generateEmailOtp(effEmail.trim());
            emailService.sendRegistrationOtp(effEmail.trim(), otp);
            auditService.logEvent("EMAIL_OTP_SENT", effEmail.trim(), null, request.getRemoteAddr(), true, "Registration Email OTP sent");
            return ResponseEntity.ok(Map.of(
                "status", "OTP_REQUIRED",
                "message", "Verification code sent to your email inbox"
            ));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            log.error("Failed to send registration email OTP: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("message", "Unable to send verification code. Please check email address."));
        }
    }

    // Verify Registration Email OTP
    @PostMapping({"/verify-email-otp", "/verify-otp"})
    public ResponseEntity<?> verifyEmailOtp(
            @RequestBody(required = false) Map<String, String> body,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String otp,
            HttpServletRequest request) {
        String effEmail = email != null ? email : (body != null ? body.get("email") : null);
        String effOtp = otp != null ? otp : (body != null ? body.get("otp") : null);

        if (effEmail == null || effOtp == null || effOtp.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Email and OTP are required"));
        }

        boolean verified = otpService.verifyEmailOtp(effEmail.trim(), effOtp.trim());
        if (!verified) {
            auditService.logEvent("EMAIL_VERIFICATION_FAILED", effEmail.trim(), null, request.getRemoteAddr(), false, "Invalid or expired OTP");
            return ResponseEntity.badRequest().body(Map.of(
                "status", "FAILED",
                "message", "Invalid or expired email verification code"
            ));
        }

        auditService.logEvent("EMAIL_VERIFIED", effEmail.trim(), null, request.getRemoteAddr(), true, "Email verified successfully");
        return ResponseEntity.ok(Map.of(
            "status", "EMAIL_VERIFIED",
            "message", "Email verified successfully"
        ));
    }
}

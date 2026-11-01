package com.bank.authservice.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

public class OtpServiceTest {

    private OtpService otpService;
    private StringRedisTemplate redisTemplate;
    private ValueOperations<String, String> valueOperations;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        redisTemplate = Mockito.mock(StringRedisTemplate.class);
        valueOperations = Mockito.mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        otpService = new OtpService(redisTemplate);
    }

    @Test
    void testCreateLoginChallenge() {
        String otp = otpService.createLoginChallenge("chal-123", "johndoe", "john@example.com", "127.0.0.1", "dev-1");
        assertNotNull(otp);
        assertEquals(6, otp.length());
        assertTrue(otp.matches("\\d{6}"));
        verify(valueOperations, atLeastOnce()).set(eq("otp:login:chal-123"), anyString(), eq(Duration.ofMinutes(5)));
    }

    @Test
    void testVerifyLoginChallengeSuccess() {
        String rawOtp = "123456";
        when(valueOperations.get("otp:login:chal-123")).thenReturn(otpService.sha256(rawOtp));
        when(valueOperations.get("meta:login:chal-123")).thenReturn("johndoe|john@example.com|127.0.0.1|dev-1");
        when(valueOperations.get("attempts:login:chal-123")).thenReturn("0");

        boolean valid = otpService.verifyLoginChallenge("chal-123", rawOtp);
        assertTrue(valid);
        verify(redisTemplate).delete("otp:login:chal-123");
    }

    @Test
    void testVerifyLoginChallengeInvalidOtp() {
        when(valueOperations.get("otp:login:chal-123")).thenReturn(otpService.sha256("123456"));
        when(valueOperations.get("attempts:login:chal-123")).thenReturn("1");

        boolean valid = otpService.verifyLoginChallenge("chal-123", "999999");
        assertFalse(valid);
    }

    @Test
    void testGenerateAndVerifyEmailOtp() {
        String rawOtp = "654321";
        String email = "test@example.com";
        when(valueOperations.get("otp:email:test@example.com")).thenReturn(otpService.sha256(rawOtp));
        when(valueOperations.get("attempts:email:test@example.com")).thenReturn("0");

        boolean verified = otpService.verifyEmailOtp(email, rawOtp);
        assertTrue(verified);
        verify(valueOperations).set(eq("verified:email:test@example.com"), eq("true"), eq(Duration.ofMinutes(30)));
    }

    @Test
    void testEmailOtpMaxAttemptsExceeded() {
        when(valueOperations.get("otp:email:test@example.com")).thenReturn(otpService.sha256("123456"));
        when(valueOperations.get("attempts:email:test@example.com")).thenReturn("5");

        boolean verified = otpService.verifyEmailOtp("test@example.com", "123456");
        assertFalse(verified);
    }

    @Test
    void testPasswordResetFlow() {
        String rawOtp = "888888";
        String email = "user@example.com";
        when(valueOperations.get("otp:pwd:user@example.com")).thenReturn(otpService.sha256(rawOtp));
        when(valueOperations.get("attempts:pwd:user@example.com")).thenReturn("0");

        boolean verified = otpService.verifyPasswordResetOtp(email, rawOtp);
        assertTrue(verified);
        verify(valueOperations).set(eq("verified:pwd:user@example.com"), eq("true"), eq(Duration.ofMinutes(15)));
    }
}

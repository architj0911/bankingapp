package com.bank.authservice.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class OtpService {

    public enum OtpPurpose {
        EMAIL_VERIFICATION,
        LOGIN_VERIFICATION,
        PASSWORD_RESET
    }

    private final StringRedisTemplate redisTemplate;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    // Configuration constants
    public static final int MAX_ATTEMPTS = 5;
    public static final Duration OTP_TTL = Duration.ofMinutes(5);
    public static final Duration COOLDOWN_DURATION = Duration.ofSeconds(60);
    public static final Duration EMAIL_VERIFIED_TTL = Duration.ofMinutes(30);
    public static final Duration PWD_RESET_VERIFIED_TTL = Duration.ofMinutes(15);

    // Purpose-isolated Redis key prefixes
    private static final String LOGIN_OTP_PREFIX = "otp:login:";
    private static final String LOGIN_META_PREFIX = "meta:login:";
    private static final String LOGIN_ATTEMPTS_PREFIX = "attempts:login:";
    private static final String LOGIN_COOLDOWN_PREFIX = "cooldown:login:";

    private static final String EMAIL_OTP_PREFIX = "otp:email:";
    private static final String EMAIL_ATTEMPTS_PREFIX = "attempts:email:";
    private static final String EMAIL_VERIFIED_PREFIX = "verified:email:";
    private static final String EMAIL_COOLDOWN_PREFIX = "cooldown:email:";

    private static final String PWD_OTP_PREFIX = "otp:pwd:";
    private static final String PWD_ATTEMPTS_PREFIX = "attempts:pwd:";
    private static final String PWD_VERIFIED_PREFIX = "verified:pwd:";
    private static final String PWD_COOLDOWN_PREFIX = "cooldown:pwd:";

    // Legacy fallback support
    private static final String LEGACY_OTP_KEY = "otp:";
    private static final String LEGACY_META_KEY = "otp_meta:";
    private static final String LEGACY_ATTEMPTS_KEY = "otp_attempts:";

    // ==========================================
    // 1. LOGIN VERIFICATION (CHALLENGE BASED)
    // ==========================================

    public String createLoginChallenge(String challengeId, String username, String email, String ip, String deviceId) {
        String cleanUsername = normalizeUsername(username);
        String cleanChallenge = challengeId.trim();
        String otp = generateSecure6DigitCode();

        if (redisTemplate != null) {
            try {
                // Invalidate any previous state for this challenge
                redisTemplate.delete(LOGIN_OTP_PREFIX + cleanChallenge);
                redisTemplate.delete(LOGIN_ATTEMPTS_PREFIX + cleanChallenge);

                // Store secure SHA-256 hash
                redisTemplate.opsForValue().set(LOGIN_OTP_PREFIX + cleanChallenge, sha256(otp), OTP_TTL);

                // Store session metadata: username|email|ip|deviceId
                String meta = cleanUsername + "|" + (email != null ? email.trim() : "") + "|" + (ip != null ? ip : "") + "|" + (deviceId != null ? deviceId : "");
                redisTemplate.opsForValue().set(LOGIN_META_PREFIX + cleanChallenge, meta, OTP_TTL);

                // Set 60-second resend cooldown
                redisTemplate.opsForValue().set(LOGIN_COOLDOWN_PREFIX + cleanChallenge, "active", COOLDOWN_DURATION);

                // Maintain backwards-compatible legacy key for OAuth2 grant
                redisTemplate.opsForValue().set(LEGACY_OTP_KEY + cleanUsername, sha256(otp), OTP_TTL);
                redisTemplate.opsForValue().set(LEGACY_META_KEY + cleanUsername, (ip != null ? ip : "") + "|" + (deviceId != null ? deviceId : ""), OTP_TTL);
            } catch (Exception e) {
                log.warn("Redis write failure during createLoginChallenge: {}", e.getMessage());
            }
        }

        log.info("Login challenge [{}] initialized for user '{}'", cleanChallenge, cleanUsername);
        return otp;
    }

    public boolean verifyLoginChallenge(String challengeId, String otp) {
        if (redisTemplate == null || challengeId == null || otp == null) {
            return false;
        }

        String cleanChallenge = challengeId.trim();
        try {
            String storedHash = redisTemplate.opsForValue().get(LOGIN_OTP_PREFIX + cleanChallenge);
            if (storedHash == null) {
                log.warn("Login challenge [{}] not found or expired", cleanChallenge);
                return false;
            }

            long currentAttempts = getAttempts(LOGIN_ATTEMPTS_PREFIX + cleanChallenge);
            if (currentAttempts >= MAX_ATTEMPTS) {
                log.warn("Login challenge [{}] maximum attempts ({}) exceeded. Invaliding OTP.", cleanChallenge, MAX_ATTEMPTS);
                redisTemplate.delete(LOGIN_OTP_PREFIX + cleanChallenge);
                return false;
            }

            incrementAttempts(LOGIN_ATTEMPTS_PREFIX + cleanChallenge);

            boolean matches = MessageDigest.isEqual(
                storedHash.getBytes(StandardCharsets.UTF_8),
                sha256(otp.trim()).getBytes(StandardCharsets.UTF_8)
            );

            if (matches) {
                // Successful verification: delete OTP immediately to prevent replay/reuse
                String username = getChallengeUsername(cleanChallenge);
                redisTemplate.delete(LOGIN_OTP_PREFIX + cleanChallenge);
                redisTemplate.delete(LOGIN_META_PREFIX + cleanChallenge);
                redisTemplate.delete(LOGIN_ATTEMPTS_PREFIX + cleanChallenge);
                redisTemplate.delete(LOGIN_COOLDOWN_PREFIX + cleanChallenge);

                if (username != null) {
                    redisTemplate.delete(LEGACY_OTP_KEY + username);
                    redisTemplate.delete(LEGACY_META_KEY + username);
                    redisTemplate.delete(LEGACY_ATTEMPTS_KEY + username);
                }
                log.info("Login challenge [{}] verified successfully", cleanChallenge);
                return true;
            } else {
                log.warn("Login challenge [{}] verification failed (attempt {}/{})", cleanChallenge, currentAttempts + 1, MAX_ATTEMPTS);
                return false;
            }
        } catch (Exception e) {
            log.error("Redis verify error for challenge [{}]: {}", cleanChallenge, e.getMessage());
            return false;
        }
    }

    public String resendLoginChallenge(String challengeId) {
        if (redisTemplate == null || challengeId == null) {
            throw new IllegalArgumentException("Challenge identifier cannot be null");
        }

        String cleanChallenge = challengeId.trim();
        if (Boolean.TRUE.equals(redisTemplate.hasKey(LOGIN_COOLDOWN_PREFIX + cleanChallenge))) {
            Long ttlSeconds = redisTemplate.getExpire(LOGIN_COOLDOWN_PREFIX + cleanChallenge);
            long waitTime = ttlSeconds != null && ttlSeconds > 0 ? ttlSeconds : 60;
            throw new IllegalStateException("Please wait " + waitTime + " seconds before requesting a new verification code.");
        }

        String meta = redisTemplate.opsForValue().get(LOGIN_META_PREFIX + cleanChallenge);
        if (meta == null) {
            throw new IllegalArgumentException("Login session expired or invalid. Please sign in again.");
        }

        String[] parts = meta.split("\\|", -1);
        String username = parts.length > 0 ? parts[0] : "";
        String email = parts.length > 1 ? parts[1] : "";
        String ip = parts.length > 2 ? parts[2] : "";
        String deviceId = parts.length > 3 ? parts[3] : "";

        return createLoginChallenge(cleanChallenge, username, email, ip, deviceId);
    }

    public String getChallengeUsername(String challengeId) {
        if (redisTemplate == null || challengeId == null) return null;
        try {
            String meta = redisTemplate.opsForValue().get(LOGIN_META_PREFIX + challengeId.trim());
            if (meta != null) {
                String[] parts = meta.split("\\|", -1);
                return parts.length > 0 ? parts[0] : null;
            }
        } catch (Exception e) {
            log.warn("Redis read warning in getChallengeUsername: {}", e.getMessage());
        }
        return null;
    }

    public String getChallengeEmail(String challengeId) {
        if (redisTemplate == null || challengeId == null) return null;
        try {
            String meta = redisTemplate.opsForValue().get(LOGIN_META_PREFIX + challengeId.trim());
            if (meta != null) {
                String[] parts = meta.split("\\|", -1);
                return parts.length > 1 ? parts[1] : null;
            }
        } catch (Exception e) {
            log.warn("Redis read warning in getChallengeEmail: {}", e.getMessage());
        }
        return null;
    }

    // ==========================================
    // 2. EMAIL VERIFICATION (REGISTRATION)
    // ==========================================

    public String generateEmailOtp(String email) {
        String key = normalizeEmail(email);
        if (key.isEmpty()) {
            throw new IllegalArgumentException("Email address cannot be empty");
        }

        if (redisTemplate != null && Boolean.TRUE.equals(redisTemplate.hasKey(EMAIL_COOLDOWN_PREFIX + key))) {
            Long ttlSeconds = redisTemplate.getExpire(EMAIL_COOLDOWN_PREFIX + key);
            long waitTime = ttlSeconds != null && ttlSeconds > 0 ? ttlSeconds : 60;
            throw new IllegalStateException("Please wait " + waitTime + " seconds before requesting a new verification code.");
        }

        String otp = generateSecure6DigitCode();
        if (redisTemplate != null) {
            redisTemplate.delete(EMAIL_OTP_PREFIX + key);
            redisTemplate.delete(EMAIL_ATTEMPTS_PREFIX + key);

            redisTemplate.opsForValue().set(EMAIL_OTP_PREFIX + key, sha256(otp), OTP_TTL);
            redisTemplate.opsForValue().set(EMAIL_COOLDOWN_PREFIX + key, "active", COOLDOWN_DURATION);
        }

        log.info("Registration email OTP generated for normalized address ending with @{}", extractDomain(key));
        return otp;
    }

    public boolean verifyEmailOtp(String email, String otp) {
        String key = normalizeEmail(email);
        if (redisTemplate == null || key.isEmpty() || otp == null) {
            return false;
        }

        String stored = redisTemplate.opsForValue().get(EMAIL_OTP_PREFIX + key);
        if (stored == null) {
            return false;
        }

        long currentAttempts = getAttempts(EMAIL_ATTEMPTS_PREFIX + key);
        if (currentAttempts >= MAX_ATTEMPTS) {
            log.warn("Email verification for {} exceeded maximum attempts. Invalidating OTP.", extractDomain(key));
            redisTemplate.delete(EMAIL_OTP_PREFIX + key);
            return false;
        }

        incrementAttempts(EMAIL_ATTEMPTS_PREFIX + key);

        boolean valid = MessageDigest.isEqual(
            stored.getBytes(StandardCharsets.UTF_8),
            sha256(otp.trim()).getBytes(StandardCharsets.UTF_8)
        );

        if (valid) {
            redisTemplate.delete(EMAIL_OTP_PREFIX + key);
            redisTemplate.delete(EMAIL_ATTEMPTS_PREFIX + key);
            redisTemplate.delete(EMAIL_COOLDOWN_PREFIX + key);
            // Mark email as verified for 30 minutes to allow registration completion
            redisTemplate.opsForValue().set(EMAIL_VERIFIED_PREFIX + key, "true", EMAIL_VERIFIED_TTL);
            log.info("Email address for domain @{} successfully verified", extractDomain(key));
        }

        return valid;
    }

    public boolean isEmailVerified(String email) {
        if (redisTemplate == null || email == null) return false;
        return "true".equals(redisTemplate.opsForValue().get(EMAIL_VERIFIED_PREFIX + normalizeEmail(email)));
    }

    public void clearEmailVerification(String email) {
        if (redisTemplate != null && email != null) {
            redisTemplate.delete(EMAIL_VERIFIED_PREFIX + normalizeEmail(email));
        }
    }

    // ==========================================
    // 3. PASSWORD RESET VERIFICATION
    // ==========================================

    public String generatePasswordResetOtp(String email) {
        String key = normalizeEmail(email);
        if (key.isEmpty()) {
            throw new IllegalArgumentException("Email address cannot be empty");
        }

        if (redisTemplate != null && Boolean.TRUE.equals(redisTemplate.hasKey(PWD_COOLDOWN_PREFIX + key))) {
            Long ttlSeconds = redisTemplate.getExpire(PWD_COOLDOWN_PREFIX + key);
            long waitTime = ttlSeconds != null && ttlSeconds > 0 ? ttlSeconds : 60;
            throw new IllegalStateException("Please wait " + waitTime + " seconds before requesting a new password reset code.");
        }

        String otp = generateSecure6DigitCode();
        if (redisTemplate != null) {
            redisTemplate.delete(PWD_OTP_PREFIX + key);
            redisTemplate.delete(PWD_ATTEMPTS_PREFIX + key);

            redisTemplate.opsForValue().set(PWD_OTP_PREFIX + key, sha256(otp), OTP_TTL);
            redisTemplate.opsForValue().set(PWD_COOLDOWN_PREFIX + key, "active", COOLDOWN_DURATION);
        }

        log.info("Password reset OTP generated for normalized address ending with @{}", extractDomain(key));
        return otp;
    }

    public boolean verifyPasswordResetOtp(String email, String otp) {
        String key = normalizeEmail(email);
        if (redisTemplate == null || key.isEmpty() || otp == null) {
            return false;
        }

        String stored = redisTemplate.opsForValue().get(PWD_OTP_PREFIX + key);
        if (stored == null) {
            return false;
        }

        long currentAttempts = getAttempts(PWD_ATTEMPTS_PREFIX + key);
        if (currentAttempts >= MAX_ATTEMPTS) {
            log.warn("Password reset verification for {} exceeded maximum attempts.", extractDomain(key));
            redisTemplate.delete(PWD_OTP_PREFIX + key);
            return false;
        }

        incrementAttempts(PWD_ATTEMPTS_PREFIX + key);

        boolean valid = MessageDigest.isEqual(
            stored.getBytes(StandardCharsets.UTF_8),
            sha256(otp.trim()).getBytes(StandardCharsets.UTF_8)
        );

        if (valid) {
            redisTemplate.delete(PWD_OTP_PREFIX + key);
            redisTemplate.delete(PWD_ATTEMPTS_PREFIX + key);
            redisTemplate.delete(PWD_COOLDOWN_PREFIX + key);
            redisTemplate.opsForValue().set(PWD_VERIFIED_PREFIX + key, "true", PWD_RESET_VERIFIED_TTL);
        }

        return valid;
    }

    public boolean isPasswordResetVerified(String email) {
        if (redisTemplate == null || email == null) return false;
        return "true".equals(redisTemplate.opsForValue().get(PWD_VERIFIED_PREFIX + normalizeEmail(email)));
    }

    public void clearPasswordResetVerification(String email) {
        if (redisTemplate != null && email != null) {
            redisTemplate.delete(PWD_VERIFIED_PREFIX + normalizeEmail(email));
        }
    }

    // ==========================================
    // 4. LEGACY DIRECT OTP SUPPORT (Backward Compatibility)
    // ==========================================

    public String generateOtp(String userName, String ip, String deviceId) {
        String cleanUsername = normalizeUsername(userName);
        String otp = generateSecure6DigitCode();
        if (redisTemplate != null) {
            redisTemplate.opsForValue().set(LEGACY_OTP_KEY + cleanUsername, sha256(otp), OTP_TTL);
            String meta = (ip != null ? ip : "") + "|" + (deviceId != null ? deviceId : "");
            redisTemplate.opsForValue().set(LEGACY_META_KEY + cleanUsername, meta, OTP_TTL);
            redisTemplate.delete(LEGACY_ATTEMPTS_KEY + cleanUsername);
        }
        return otp;
    }

    public boolean verifyOtp(String userName, String otp, String ip, String deviceId) {
        String cleanUsername = normalizeUsername(userName);
        if (redisTemplate == null) return false;

        String storedOtpHash = redisTemplate.opsForValue().get(LEGACY_OTP_KEY + cleanUsername);
        String meta = redisTemplate.opsForValue().get(LEGACY_META_KEY + cleanUsername);

        if (storedOtpHash == null || meta == null || getAttempts(LEGACY_ATTEMPTS_KEY + cleanUsername) >= MAX_ATTEMPTS) {
            return false;
        }
        incrementAttempts(LEGACY_ATTEMPTS_KEY + cleanUsername);

        boolean otpMatches = MessageDigest.isEqual(
            storedOtpHash.getBytes(StandardCharsets.UTF_8),
            sha256(otp == null ? "" : otp.trim()).getBytes(StandardCharsets.UTF_8)
        );

        if (otpMatches) {
            redisTemplate.delete(LEGACY_OTP_KEY + cleanUsername);
            redisTemplate.delete(LEGACY_META_KEY + cleanUsername);
            redisTemplate.delete(LEGACY_ATTEMPTS_KEY + cleanUsername);
        }

        return otpMatches;
    }

    // ==========================================
    // HELPER & CRYPTO UTILITIES
    // ==========================================

    private String generateSecure6DigitCode() {
        int number = SECURE_RANDOM.nextInt(1_000_000);
        return String.format("%06d", number);
    }

    public String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encodedhash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : encodedhash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 algorithm is not available in the runtime environment", ex);
        }
    }

    private long getAttempts(String key) {
        try {
            String value = redisTemplate.opsForValue().get(key);
            return value == null ? 0 : Long.parseLong(value);
        } catch (Exception e) {
            return 0;
        }
    }

    private void incrementAttempts(String key) {
        try {
            Long value = redisTemplate.opsForValue().increment(key);
            if (value != null && value == 1) {
                redisTemplate.expire(key, OTP_TTL);
            }
        } catch (Exception e) {
            log.warn("Redis attempt increment failed for {}: {}", key, e.getMessage());
        }
    }

    private String normalizeUsername(String username) {
        return username == null ? "" : username.trim().toLowerCase();
    }

    private String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }

    private String extractDomain(String email) {
        int at = email.indexOf('@');
        return at >= 0 && at < email.length() - 1 ? email.substring(at + 1) : "unknown";
    }
}

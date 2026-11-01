package com.bank.customer.service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import com.bank.customer.kyc.FaceLivenessResult;
import com.bank.customer.kyc.FaceVerificationResult;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class FaceVerificationService {

	private final StringRedisTemplate redisTemplate;
	private static final String LIVENESS_SESSION_KEY = "kyc:face:session:";
	private static final String LIVENESS_PASSED_KEY = "kyc:face:live-image:";
	private static final List<String> ALL_CHALLENGES = List.of(
			"TURN_LEFT", "BLINK", "TURN_RIGHT", "LOOK_UP");

	public FaceLivenessResult startLiveness(Long userId) {
		String sessionId = UUID.randomUUID().toString();
		List<String> shuffled = new ArrayList<>(ALL_CHALLENGES);
		Collections.shuffle(shuffled);
		List<String> selectedChallenges = List.copyOf(shuffled.subList(0, 3));

		write(LIVENESS_SESSION_KEY + sessionId, userId + ":" + String.join(",", selectedChallenges), Duration.ofMinutes(5));
		log.info("Started face liveness session {} for userId {}", sessionId, userId);
		return new FaceLivenessResult(false, sessionId, selectedChallenges,
				"DEMO FACE LIVENESS session started. Complete the challenges.");
	}

	public FaceLivenessResult completeLiveness(Long userId, String sessionId, String liveImageBase64, String outcome) {
		if (sessionId == null || sessionId.isBlank()) {
			return new FaceLivenessResult(false, sessionId, List.of(), "Liveness session is required.");
		}
		String stored = read(LIVENESS_SESSION_KEY + sessionId);
		if (stored == null) {
			return new FaceLivenessResult(false, sessionId, List.of(), "Liveness session expired or invalid.");
		}
		String[] parts = stored.split(":", 2);
		if (parts.length < 1 || !String.valueOf(userId).equals(parts[0])) {
			return new FaceLivenessResult(false, sessionId, List.of(), "Liveness session does not belong to this user.");
		}

		String normalizedOutcome = outcome == null ? "PASSED" : outcome.trim().toUpperCase(Locale.ROOT);
		if ("TIMEOUT".equals(normalizedOutcome)) {
			delete(LIVENESS_SESSION_KEY + sessionId);
			return new FaceLivenessResult(false, sessionId, List.of(), "Liveness timed out.");
		}
		if ("NO_FACE".equals(normalizedOutcome)) {
			return new FaceLivenessResult(false, sessionId, List.of(), "No face detected in the frame.");
		}
		if ("MULTIPLE_FACES".equals(normalizedOutcome)) {
			return new FaceLivenessResult(false, sessionId, List.of(), "Multiple faces detected.");
		}
		if ("WRONG_MOVEMENT".equals(normalizedOutcome)) {
			return new FaceLivenessResult(false, sessionId, List.of(), "Incorrect movement for the challenge.");
		}
		if (liveImageBase64 == null || liveImageBase64.trim().isEmpty()) {
			return new FaceLivenessResult(false, sessionId, List.of(), "Live face image frame is required.");
		}

		delete(LIVENESS_SESSION_KEY + sessionId);
		write(LIVENESS_PASSED_KEY + userId, "CAPTURED", Duration.ofMinutes(15));
		log.info("Face liveness passed for userId {}", userId);
		return new FaceLivenessResult(true, sessionId, List.of(), "DEMO FACE LIVENESS passed.");
	}

	public FaceVerificationResult matchFace(Long userId, String liveImageBase64) {
		boolean hadLiveSession = hasKey(LIVENESS_PASSED_KEY + userId);
		if (!hadLiveSession) {
			return new FaceVerificationResult(false, 0.0, "Complete face liveness before matching.");
		}
		if (liveImageBase64 != null && liveImageBase64.toUpperCase(Locale.ROOT).contains("MISMATCH")) {
			return new FaceVerificationResult(false, 0.41, "Face matching failed.");
		}
		delete(LIVENESS_PASSED_KEY + userId);
		log.info("Face matching verified for userId {}", userId);
		return new FaceVerificationResult(true, 0.94, "DEMO face matching passed.");
	}

	private void write(String key, String value, Duration ttl) {
		if (redisTemplate == null) return;
		try {
			redisTemplate.opsForValue().set(key, value, ttl);
		} catch (Exception e) {
			log.warn("Redis write warning: {}", e.getMessage());
		}
	}

	private String read(String key) {
		if (redisTemplate == null) return null;
		try {
			return redisTemplate.opsForValue().get(key);
		} catch (Exception e) {
			log.warn("Redis read warning: {}", e.getMessage());
			return null;
		}
	}

	private boolean hasKey(String key) {
		if (redisTemplate == null) return false;
		try {
			return Boolean.TRUE.equals(redisTemplate.hasKey(key));
		} catch (Exception e) {
			return false;
		}
	}

	private void delete(String key) {
		if (redisTemplate == null) return;
		try {
			redisTemplate.delete(key);
		} catch (Exception ignored) {
		}
	}
}

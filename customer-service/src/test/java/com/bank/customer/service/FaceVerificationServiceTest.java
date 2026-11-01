package com.bank.customer.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import com.bank.customer.kyc.FaceLivenessResult;
import com.bank.customer.kyc.FaceVerificationResult;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

public class FaceVerificationServiceTest {

    private FaceVerificationService faceService;
    private StringRedisTemplate redisTemplate;
    private ValueOperations<String, String> valueOperations;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        redisTemplate = Mockito.mock(StringRedisTemplate.class);
        valueOperations = Mockito.mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        faceService = new FaceVerificationService(redisTemplate);
    }

    @Test
    void testStartLivenessGeneratesChallenges() {
        FaceLivenessResult result = faceService.startLiveness(101L);
        assertNotNull(result.sessionId());
        assertEquals(3, result.challenges().size());
        assertFalse(result.passed());
    }

    @Test
    void testCompleteLivenessWithValidImage() {
        FaceLivenessResult result = faceService.completeLiveness(101L, "sess-123", "data:image/jpeg;base64,TEST_LIVE_FRAME", "PASSED");
        assertTrue(result.passed());
        assertEquals("sess-123", result.sessionId());
    }

    @Test
    void testCompleteLivenessWithEmptyImage() {
        FaceLivenessResult result = faceService.completeLiveness(101L, "sess-123", "", "PASSED");
        assertFalse(result.passed());
    }

    @Test
    void testMatchFaceWithImages() {
        FaceVerificationResult result = faceService.matchFace(101L, "data:image/jpeg;base64,LIVE");
        assertTrue(result.matched());
        assertTrue(result.similarityScore() >= 0.85);
    }
}

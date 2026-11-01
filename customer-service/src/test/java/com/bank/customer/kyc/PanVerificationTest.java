package com.bank.customer.kyc;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class PanVerificationTest {

    private DemoPanVerificationProvider provider;

    @BeforeEach
    void setUp() {
        provider = new DemoPanVerificationProvider();
    }

    @Test
    void testValidPanFormat() {
        PanVerificationResult result = provider.verify("ABCDE1234F", "John Doe");
        assertTrue(result.valid());
        assertEquals("ABCDE1234F", result.normalizedPan());
        assertEquals("DEMO", result.mode());
    }

    @Test
    void testValidPanWithLowercaseAndSpaces() {
        PanVerificationResult result = provider.verify(" abcde1234f ", "John Doe");
        assertTrue(result.valid());
        assertEquals("ABCDE1234F", result.normalizedPan());
    }

    @Test
    void testInvalidPanShortLength() {
        PanVerificationResult result = provider.verify("ABCD1234", "John Doe");
        assertFalse(result.valid());
    }

    @Test
    void testInvalidPanBadCharacters() {
        PanVerificationResult result = provider.verify("ABCDE12345", "John Doe");
        assertFalse(result.valid());
    }

    @Test
    void testNullPan() {
        PanVerificationResult result = provider.verify(null, "John Doe");
        assertFalse(result.valid());
    }
}

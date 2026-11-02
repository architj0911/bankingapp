package com.bank.customer.kyc;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class AadhaarVerificationTest {

    private DemoAadhaarVerificationProvider provider;

    @BeforeEach
    void setUp() {
        provider = new DemoAadhaarVerificationProvider();
    }

    @Test
    void testValidXmlPackage() {
        AadhaarVerificationResult result = provider.verify("aadhaar_offline.xml", 1024 * 50);
        assertTrue(result.verified());
        assertNotNull(result.referenceId());
        assertTrue(result.referenceId().startsWith("DEMO-OFFLINE-"));
        assertEquals("DEMO/OFFLINE", result.mode());
    }

    @Test
    void testValidZipPackage() {
        AadhaarVerificationResult result = provider.verify("offline_kyc.zip", 1024 * 200);
        assertTrue(result.verified());
        assertNotNull(result.referenceId());
    }

    @Test
    void testInvalidFileType() {
        AadhaarVerificationResult result = provider.verify("document.pdf", 1024 * 50);
        assertFalse(result.verified());
        assertNull(result.referenceId());
    }

    @Test
    void testOversizedFile() {
        AadhaarVerificationResult result = provider.verify("aadhaar_offline.xml", 10_000_000);
        assertFalse(result.verified());
    }

    @Test
    void testZeroByteFile() {
        AadhaarVerificationResult result = provider.verify("aadhaar_offline.xml", 0);
        assertFalse(result.verified());
    }
}

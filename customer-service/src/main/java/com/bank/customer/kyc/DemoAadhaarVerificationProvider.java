package com.bank.customer.kyc;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.UUID;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "kyc.aadhaar.provider", havingValue = "demo", matchIfMissing = true)
public class DemoAadhaarVerificationProvider implements AadhaarVerificationProvider {
	private static final long MAX_BYTES = 5_000_000;

	@Override
	public AadhaarVerificationResult verify(String fileName, long fileSize, byte[] content) {
		if (fileName == null || fileName.trim().isEmpty()) {
			return fail("Aadhaar Offline e-KYC package file is required");
		}
		String name = fileName.toLowerCase(Locale.ROOT);
		boolean supportedType = name.endsWith(".xml") || name.endsWith(".zip");
		long size = content != null ? content.length : fileSize;
		if (!supportedType) {
			return fail("Upload an XML or ZIP official Offline e-KYC package");
		}
		if (size <= 0) {
			return fail("Offline e-KYC package is empty");
		}
		if (size > MAX_BYTES) {
			return fail("Offline e-KYC package exceeds the 5 MB size limit");
		}
		if (content != null && content.length > 0) {
			if (name.endsWith(".zip") && !isZip(content)) {
				return fail("Invalid ZIP package");
			}
			if (name.endsWith(".xml") && !isXml(content)) {
				return fail("Invalid Offline e-KYC XML package");
			}
		}
		return new AadhaarVerificationResult(
				true,
				"DEMO-OFFLINE-" + UUID.randomUUID(),
				"Offline e-KYC package accepted in demo mode. UIDAI digital signature is not verified.",
				"DEMO/OFFLINE");
	}

	private AadhaarVerificationResult fail(String reason) {
		return new AadhaarVerificationResult(false, null, reason, "DEMO/OFFLINE");
	}

	private boolean isZip(byte[] content) {
		return content.length >= 4 && content[0] == 0x50 && content[1] == 0x4B;
	}

	private boolean isXml(byte[] content) {
		String text = new String(content, StandardCharsets.UTF_8).trim();
		return text.startsWith("<") && !text.toUpperCase(Locale.ROOT).contains("<!DOCTYPE HTML");
	}
}

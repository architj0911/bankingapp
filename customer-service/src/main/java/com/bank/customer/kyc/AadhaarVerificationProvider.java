package com.bank.customer.kyc;

public interface AadhaarVerificationProvider {
	AadhaarVerificationResult verify(String fileName, long fileSize, byte[] content);

	default AadhaarVerificationResult verify(String fileName, long fileSize) {
		return verify(fileName, fileSize, null);
	}
}

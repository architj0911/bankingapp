package com.bank.customer.kyc;

public record AadhaarVerificationResult(boolean verified, String referenceId, String reason, String mode) { }
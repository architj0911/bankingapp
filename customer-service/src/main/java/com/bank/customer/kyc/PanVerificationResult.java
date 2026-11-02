package com.bank.customer.kyc;

public record PanVerificationResult(boolean valid, String normalizedPan, String reason, String mode) { }
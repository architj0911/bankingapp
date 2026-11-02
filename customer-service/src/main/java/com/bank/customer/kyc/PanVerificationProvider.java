package com.bank.customer.kyc;

public interface PanVerificationProvider {
    PanVerificationResult verify(String pan, String customerName);
}
package com.bank.customer.kyc;

import java.util.Locale;
import java.util.regex.Pattern;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "kyc.pan.provider", havingValue = "demo", matchIfMissing = true)
public class DemoPanVerificationProvider implements PanVerificationProvider {
    private static final Pattern PAN = Pattern.compile("^[A-Z]{5}[0-9]{4}[A-Z]$");

    @Override
    public PanVerificationResult verify(String pan, String customerName) {
        String normalized = pan == null ? "" : pan.replaceAll("\\s+", "").toUpperCase(Locale.ROOT);
        boolean valid = PAN.matcher(normalized).matches();
        return new PanVerificationResult(valid, normalized, valid ? "PAN format is valid; government existence is not checked in demo mode" : "PAN format is invalid", "DEMO");
    }
}
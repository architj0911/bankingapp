package com.bank.customer.kyc;

import java.util.List;

public record FaceLivenessResult(
    boolean passed,
    String sessionId,
    List<String> challenges,
    String message
) { }

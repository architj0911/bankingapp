package com.bank.customer.kyc;

import com.fasterxml.jackson.annotation.JsonIgnore;

public record FaceVerificationResult(
		boolean matched,
		@JsonIgnore double similarityScore,
		String message
) { }

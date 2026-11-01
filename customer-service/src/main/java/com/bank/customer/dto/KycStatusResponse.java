package com.bank.customer.dto;

import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class KycStatusResponse {
	private Long id;
	private Long userId;
	private String panMasked;
	private String panStatus;
	private String aadhaarReferenceId;
	private String aadhaarStatus;
	private String emailStatus;
	private String faceStatus;
	private String livenessStatus;
	private String overallStatus;
	private String status;
	private String failureReason;
	private String verificationMode;
	private LocalDateTime verifiedAt;
	private LocalDateTime createdAt;
	private LocalDateTime updatedAt;
}

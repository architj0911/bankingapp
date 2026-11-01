package com.bank.customer.entity;

import java.time.LocalDateTime;

import com.bank.customer.enums.KycStatus;
import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "kyc_details")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class KycDetails {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "user_id")
	private Long userId;

	@Column(name = "pan_masked")
	private String panMasked;

	@Column(name = "pan_status", nullable = false)
	private String panStatus;

	@Column(name = "aadhaar_reference_id")
	private String aadhaarReferenceId;

	@Column(name = "aadhaar_status", nullable = false)
	private String aadhaarStatus;

	@Column(name = "email_status", nullable = false)
	private String emailStatus;

	@Column(name = "face_status", nullable = false)
	private String faceStatus;

	@Column(name = "liveness_status", nullable = false)
	private String livenessStatus;

	@Column(name = "overall_status", nullable = false)
	private String overallStatus;

	@Column(name = "failure_reason")
	private String failureReason;

	@Column(name = "verified_at")
	private LocalDateTime verifiedAt;

	@Column(name = "created_at", nullable = false)
	private LocalDateTime createdAt;

	@Column(name = "updated_at", nullable = false)
	private LocalDateTime updatedAt;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private KycStatus status;

	@JsonIgnore
	@ManyToOne
	@JoinColumn(name = "customer_id")
	private Customer customer;

	@JsonIgnore
	@Deprecated
	private String panNumber;

	@JsonIgnore
	@Deprecated
	private String aadharNumber;
}

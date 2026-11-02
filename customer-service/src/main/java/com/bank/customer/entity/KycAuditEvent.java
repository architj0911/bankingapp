package com.bank.customer.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "kyc_audit")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KycAuditEvent {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "user_id")
	private Long userId;

	@Column(name = "event_type", nullable = false)
	private String eventType;

	@Column(name = "ip_address")
	private String ipAddress;

	private boolean success;

	@Column(name = "failure_reason")
	private String failureReason;

	@Column(nullable = false)
	private LocalDateTime timestamp;
}

package com.bank.customer.service;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;

import com.bank.customer.entity.KycAuditEvent;
import com.bank.customer.repository.KycAuditRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class KycAuditService {
	private final KycAuditRepository auditRepository;

	public void log(Long userId, String eventType, String ip, boolean success, String failureReason) {
		try {
			auditRepository.save(KycAuditEvent.builder()
					.userId(userId)
					.eventType(eventType)
					.ipAddress(ip)
					.success(success)
					.failureReason(failureReason)
					.timestamp(LocalDateTime.now())
					.build());
			log.info("KYC AUDIT [{}] userId={} success={}", eventType, userId, success);
		} catch (Exception ex) {
			log.warn("Failed to persist KYC audit event [{}]: {}", eventType, ex.getMessage());
		}
	}
}

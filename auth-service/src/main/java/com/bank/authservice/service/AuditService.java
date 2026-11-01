package com.bank.authservice.service;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;

import com.bank.authservice.audit.LoginAuditEntity;
import com.bank.authservice.repository.LoginAuditRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuditService {
	private final LoginAuditRepository auditRepo;

	public void logEvent(String eventType, String userName, Long userId, String ip, boolean success, String reason) {
		try {
			auditRepo.save(LoginAuditEntity.builder()
					.eventType(eventType)
					.userName(userName)
					.userId(userId)
					.ipAddress(ip)
					.success(success)
					.reason(reason)
					.loginTime(LocalDateTime.now())
					.build());
			log.info("AUDIT EVENT [{}]: user={}, success={}, ip={}", eventType, userName, success, ip);
		} catch (Exception e) {
			log.warn("Failed to persist audit event [{}]: {}", eventType, e.getMessage());
		}
	}

	public void logSuccess(String userName, String ip) {
		logEvent("LOGIN_SUCCESS", userName, null, ip, true, "Authenticated successfully");
	}

	public void logFailure(String userName, String ip, String reason) {
		logEvent("LOGIN_FAILED", userName, null, ip, false, reason);
	}
}

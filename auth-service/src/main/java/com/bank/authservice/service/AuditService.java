package com.bank.authservice.service;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;

import com.bank.authservice.audit.LoginAuditEntity;
import com.bank.authservice.repository.LoginAuditRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuditService {
	private final LoginAuditRepository auditRepo;
	public void logSuccess(String userName, String ip) {
		auditRepo.save(LoginAuditEntity.builder()
				.userName(userName)
				.success(true)
				.ipAddress(ip)
				.loginTime(LocalDateTime.now())
				.build());
	}
	public void logFailure(String userName, String ip, String reason) {
		auditRepo.save(LoginAuditEntity.builder().userName(userName)
				.success(false)
				.ipAddress(ip)
				.loginTime(LocalDateTime.now())
				.build());
	}
}

package com.bank.transaction.audit;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;

import com.bank.transaction.entity.TransactionAudit;
import com.bank.transaction.repository.TransactionAuditRepository;
import com.bank.transaction.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TransactionAuditService {

private final TransactionAuditRepository repo;

public void log(String txnId, Long userId, String action, String ip, String deviceId) {
	repo.save(TransactionAudit.builder()
			.txnId(txnId)
			.userId(userId)
			.action(action)
			.ip(ip)
			.deviceId(deviceId)
			.createdAt(LocalDateTime.now()).build());
}
}

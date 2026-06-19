package com.bank.transaction.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import com.bank.transaction.audit.TransactionAuditService;
import com.bank.transaction.client.AccountClient;
import com.bank.transaction.dto.AccountDto;
import com.bank.transaction.dto.BalanceUpdateRequest;
import com.bank.transaction.dto.TransactionRequest;
import com.bank.transaction.dto.TransactionResponse;
import com.bank.transaction.entity.Transaction;
import com.bank.transaction.enumrator.TransactionStatus;
import com.bank.transaction.enumrator.TransactionType;
import com.bank.transaction.repository.TransactionAuditRepository;
import com.bank.transaction.repository.TransactionRepository;
import com.bank.transaction.resilience.AccountProtectionService;
import com.bank.transaction.util.SecurityUtil;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor

public class TransactionService {

    private final TransactionAuditRepository transactionAuditRepository;
private final TransactionRepository repo;
private final AccountClient accountClient;
private final KafkaTemplate<String, Object> kafkaTemplate;
private final TransactionAuditService auditService;
private final AccountProtectionService accountProtection;

  @Transactional
public TransactionResponse process(TransactionRequest request, String ip, String deviceId) {
	Long userId  = SecurityUtil.getUserId();
	Optional<Transaction> existingTxn = repo.findByReferenceId(request.getReferenceId());
	if(existingTxn.isPresent()) {
		Transaction existing = existingTxn.get();
		return map(existing);
	}
	String txnId = UUID.randomUUID().toString();
	Transaction txn  =Transaction.builder()
			.txnId(txnId)
			.referenceId(request.getReferenceId())
			.accountNumber(request.getAccountNumber())
			.type(request.getType())
			.amount(request.getAmount())
			.status(TransactionStatus.PENDING)
			.createAt(LocalDateTime.now())
			.updatedAt(LocalDateTime.now()).build();
	repo.save(txn);
	try {
		AccountDto account =accountProtection.getAccountWithProtection(request.getAccountNumber()).join();
		
		if("BLOCKED".equals(account.getStatus())) {
			throw new RuntimeException("Account blocked");
		}
		if(request.getAmount().compareTo(BigDecimal.valueOf(50000))>0) {
			throw new RuntimeException("Limit exceeded");
		}
		if(request.getType() == TransactionType.DEBIT && account.getBalance().compareTo(request.getAmount())<0) {
			throw new RuntimeException("insufficient balance");
		}
		String transactionType = request.getType().name();
		BigDecimal newBalance = request.getType() == TransactionType.DEBIT ? account.getBalance().subtract(request.getAmount()): account.getBalance().add(request.getAmount());
	accountClient.updateBalance(new BalanceUpdateRequest(request.getAccountNumber(), newBalance, transactionType ));
	txn.setStatus(TransactionStatus.SUCCESS);
	txn.setUpdatedAt(LocalDateTime.now());
	repo.save(txn);
	auditService.log(txnId, userId, request.getType().name(), ip, deviceId);
	kafkaTemplate.send("transaction-event", txn);
	return map(txn);
	} catch (Exception e) {
		txn.setStatus(TransactionStatus.FAILED);
        txn.setUpdatedAt(LocalDateTime.now());
        repo.save(txn);
        
        auditService.log(txnId, userId, "FAILED: " + e.getMessage(), ip, deviceId);
        throw new RuntimeException("Transaction failed: " + e.getMessage());
	}
			}
 
private TransactionResponse map(Transaction t) {
	return TransactionResponse.builder().txnId(t.getTxnId())
			.status(t.getStatus())
			.amount(t.getAmount()).build();
}
}

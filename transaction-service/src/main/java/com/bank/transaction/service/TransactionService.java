package com.bank.transaction.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import com.bank.transaction.audit.TransactionAuditService;
import com.bank.transaction.client.AccountClient;
import com.bank.transaction.client.CustomerClient;
import com.bank.transaction.dto.AccountDto;
import com.bank.transaction.dto.CustomerDto;
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

import com.bank.transaction.dto.TransferRequest;
import com.bank.transaction.dto.TransferResponse;
import com.bank.transaction.dto.ConversionResult;
import com.bank.transaction.enumrator.CurrencyCode;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionService {

    private final TransactionAuditRepository transactionAuditRepository;
private final TransactionRepository repo;
private final AccountClient accountClient;
private final CustomerClient customerClient;
private final KafkaTemplate<String, Object> kafkaTemplate;
private final TransactionAuditService auditService;
private final AccountProtectionService accountProtection;
private final CurrencyConversionService currencyConversionService;

	@Value("${transfer.fee:0.00}")
	private BigDecimal transferFee;

	public TransferResponse preview(TransferRequest request) {
		Long userId = SecurityUtil.getUserId();
		if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
			throw new IllegalArgumentException("Transfer amount must be greater than zero");
		}
		String fromAcc = requireAccount(request.getFromAccountNumber());
		String toAcc = requireAccount(request.getToAccountNumber());
		if (fromAcc.equalsIgnoreCase(toAcc)) {
			throw new IllegalArgumentException("Sender and receiver accounts cannot be the same");
		}
		AccountDto fromAccount = accountClient.getAccount(fromAcc);
		AccountDto toAccount = accountClient.getAccount(toAcc);
		CustomerDto customer = customerClient.getCustomerByUserId(userId);
		if (fromAccount == null || customer == null || !fromAccount.getCustomerId().equals(customer.getId())) {
			throw new SecurityException("Unauthorized: Sender account does not belong to the authenticated user");
		}
		if (toAccount == null || "BLOCKED".equalsIgnoreCase(toAccount.getStatus())) {
			throw new IllegalStateException("Receiver account is invalid or blocked");
		}
		CurrencyCode senderCurrency = fromAccount.getCurrency() == null ? CurrencyCode.INR : fromAccount.getCurrency();
		CurrencyCode receiverCurrency = toAccount.getCurrency() == null ? CurrencyCode.INR : toAccount.getCurrency();
		ConversionResult conversion = currencyConversionService.convert(request.getAmount(), senderCurrency, receiverCurrency);
		BigDecimal fee = transferFee == null ? BigDecimal.ZERO : transferFee.max(BigDecimal.ZERO);
		return TransferResponse.builder()
				.fromAccountNumber(fromAcc)
				.toAccountNumber(toAcc)
				.amount(request.getAmount())
				.senderAmount(request.getAmount())
				.senderCurrency(senderCurrency)
				.receiverAmount(conversion.getAmount())
				.receiverCurrency(receiverCurrency)
				.exchangeRate(conversion.getRate())
				.exchangeRateTimestamp(conversion.getRateTimestamp())
				.fee(fee)
				.status(TransactionStatus.PENDING)
				.build();
	}

	private String requireAccount(String accountNumber) {
		if (accountNumber == null || accountNumber.trim().isEmpty()) {
			throw new IllegalArgumentException("Account number is required");
		}
		return accountNumber.trim();
	}

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

	@Transactional
	public TransferResponse transfer(TransferRequest request, String ip, String deviceId, String idempotencyKey) {
		Long userId = SecurityUtil.getUserId();
		String referenceId = idempotencyKey == null || idempotencyKey.isBlank()
				? UUID.randomUUID().toString() : idempotencyKey.trim();
		Optional<Transaction> existing = repo.findByReferenceId(referenceId + "-D");
		if (existing.isPresent()) {
			return mapTransfer(existing.get());
		}
		if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
			throw new IllegalArgumentException("Transfer amount must be greater than zero");
		}
		if (request.getFromAccountNumber() == null || request.getToAccountNumber() == null ||
		    request.getFromAccountNumber().trim().equalsIgnoreCase(request.getToAccountNumber().trim())) {
			throw new IllegalArgumentException("Invalid sender or receiver account number");
		}

		String fromAcc = request.getFromAccountNumber().trim();
		String toAcc = request.getToAccountNumber().trim();

		AccountDto fromAccount = accountClient.getAccount(fromAcc);
		if (fromAccount == null || "BLOCKED".equalsIgnoreCase(fromAccount.getStatus())) {
			throw new IllegalStateException("Sender account is invalid or blocked");
		}

		CustomerDto senderCustomer = customerClient.getCustomerByUserId(userId);
		if (senderCustomer == null || fromAccount.getCustomerId() == null || !fromAccount.getCustomerId().equals(senderCustomer.getId())) {
			throw new SecurityException("Unauthorized: Sender account does not belong to the authenticated user");
		}
		if (fromAccount.getBalance() == null || fromAccount.getBalance().compareTo(request.getAmount()) < 0) {
			throw new IllegalStateException("Insufficient balance in sender account");
		}

		AccountDto toAccount = accountClient.getAccount(toAcc);
		if (toAccount == null || "BLOCKED".equalsIgnoreCase(toAccount.getStatus())) {
			throw new IllegalStateException("Receiver account is invalid or blocked");
		}

		CurrencyCode senderCurrency = fromAccount.getCurrency() == null ? CurrencyCode.INR : fromAccount.getCurrency();
		CurrencyCode receiverCurrency = toAccount.getCurrency() == null ? CurrencyCode.INR : toAccount.getCurrency();
		ConversionResult conversion = currencyConversionService.convert(request.getAmount(), senderCurrency, receiverCurrency);
		BigDecimal fee = transferFee == null ? BigDecimal.ZERO : transferFee.max(BigDecimal.ZERO);
		BigDecimal totalDebit = request.getAmount().add(fee);
		if (fromAccount.getBalance().compareTo(totalDebit) < 0) {
			throw new IllegalStateException("Insufficient balance in sender account including transfer fee");
		}

		String txnId = UUID.randomUUID().toString();

		BigDecimal newSenderBalance = fromAccount.getBalance().subtract(totalDebit);
		BigDecimal newReceiverBalance = toAccount.getBalance().add(conversion.getAmount());

		try {
			accountClient.updateBalance(new BalanceUpdateRequest(fromAcc, newSenderBalance, "DEBIT"));
		} catch (Exception e) {
			log.error("Failed to debit sender account {}: {}", fromAcc, e.getMessage());
			throw new RuntimeException("Debit failed: " + e.getMessage());
		}

		try {
			accountClient.updateBalance(new BalanceUpdateRequest(toAcc, newReceiverBalance, "CREDIT"));
		} catch (Exception e) {
			log.error("Failed to credit receiver account {}. Compensating debit on {}: {}", toAcc, fromAcc, e.getMessage());
			try {
				accountClient.updateBalance(new BalanceUpdateRequest(fromAcc, fromAccount.getBalance(), "CREDIT"));
			} catch (Exception compException) {
				log.error("CRITICAL: Compensation credit failed on sender {}: {}", fromAcc, compException.getMessage());
			}
			throw new RuntimeException("Credit to receiver failed, transfer rolled back: " + e.getMessage());
		}

		Transaction debitTxn = Transaction.builder()
				.txnId(txnId + "-D")
				.referenceId(referenceId + "-D")
				.accountNumber(fromAcc)
				.senderAccountNumber(fromAcc)
				.receiverAccountNumber(toAcc)
				.type(TransactionType.DEBIT)
				.amount(totalDebit)
				.senderAmount(request.getAmount())
				.senderCurrency(senderCurrency)
				.receiverAmount(conversion.getAmount())
				.receiverCurrency(receiverCurrency)
				.exchangeRate(conversion.getRate())
				.exchangeRateTimestamp(conversion.getRateTimestamp())
				.transferFee(fee)
				.status(TransactionStatus.SUCCESS)
				.createAt(LocalDateTime.now())
				.updatedAt(LocalDateTime.now())
				.build();
		repo.save(debitTxn);

		Transaction creditTxn = Transaction.builder()
				.txnId(txnId + "-C")
				.referenceId(referenceId + "-C")
				.accountNumber(toAcc)
				.senderAccountNumber(fromAcc)
				.receiverAccountNumber(toAcc)
				.type(TransactionType.CREDIT)
				.amount(conversion.getAmount())
				.senderAmount(request.getAmount())
				.senderCurrency(senderCurrency)
				.receiverAmount(conversion.getAmount())
				.receiverCurrency(receiverCurrency)
				.exchangeRate(conversion.getRate())
				.exchangeRateTimestamp(conversion.getRateTimestamp())
				.transferFee(fee)
				.status(TransactionStatus.SUCCESS)
				.createAt(LocalDateTime.now())
				.updatedAt(LocalDateTime.now())
				.build();
		repo.save(creditTxn);

		auditService.log(txnId, userId, "TRANSFER: " + fromAcc + " -> " + toAcc, ip, deviceId);
		try {
			kafkaTemplate.send("transaction-event", debitTxn);
		} catch (Exception kfEx) {
			log.warn("Failed to publish transaction-event to Kafka: {}", kfEx.getMessage());
		}

		return TransferResponse.builder()
				.txnId(txnId)
				.fromAccountNumber(fromAcc)
				.toAccountNumber(toAcc)
				.amount(request.getAmount())
				.senderAmount(request.getAmount())
				.senderCurrency(senderCurrency)
				.receiverAmount(conversion.getAmount())
				.receiverCurrency(receiverCurrency)
				.exchangeRate(conversion.getRate())
				.exchangeRateTimestamp(conversion.getRateTimestamp())
				.fee(fee)
				.status(TransactionStatus.SUCCESS)
				.description(request.getDescription() != null ? request.getDescription() : "Transfer to " + toAcc)
				.createdAt(LocalDateTime.now())
				.build();
	}

	private TransferResponse mapTransfer(Transaction transaction) {
		return TransferResponse.builder()
				.txnId(transaction.getTxnId().replace("-D", ""))
				.fromAccountNumber(transaction.getSenderAccountNumber())
				.toAccountNumber(transaction.getReceiverAccountNumber())
				.amount(transaction.getSenderAmount())
				.senderAmount(transaction.getSenderAmount())
				.senderCurrency(transaction.getSenderCurrency())
				.receiverAmount(transaction.getReceiverAmount())
				.receiverCurrency(transaction.getReceiverCurrency())
				.exchangeRate(transaction.getExchangeRate())
				.exchangeRateTimestamp(transaction.getExchangeRateTimestamp())
				.fee(transaction.getTransferFee())
				.status(transaction.getStatus())
				.createdAt(transaction.getCreateAt())
				.build();
	}

	public java.util.List<Transaction> getTransactionsForAccounts(java.util.List<String> accountNumbers) {
		if (accountNumbers == null || accountNumbers.isEmpty()) {
			return java.util.Collections.emptyList();
		}
		return repo.findByAccountNumberInOrderByCreateAtDesc(accountNumbers);
	}
 
private TransactionResponse map(Transaction t) {
	return TransactionResponse.builder().txnId(t.getTxnId())
			.status(t.getStatus())
			.amount(t.getAmount()).build();
}
}

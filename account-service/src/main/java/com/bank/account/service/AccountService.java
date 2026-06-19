package com.bank.account.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import com.bank.account.client.CustomerClient;
import com.bank.account.dto.AccountResponse;
import com.bank.account.dto.BalanceUpdateRequest;
import com.bank.account.dto.CreateAccountRequest;
import com.bank.account.dto.CustomerDto;
import com.bank.account.entity.Account;
import com.bank.account.enumrate.AccountStatus;
import com.bank.account.repository.AccountAuditRepository;
import com.bank.account.repository.AccountRepository;
import com.bank.account.security.SecurityUtil;
import com.bank.account.util.AccountNumberGenerator;
import com.bank.account.validator.AccountValidator;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class AccountService {

    private final AccountAuditRepository accountAuditRepository;
private final AccountRepository repo;
private final KafkaTemplate<String, Object> kafkaTemplate;
private final CustomerClient feignClient;
private final AccountSequenceService accountSequenceService;
private final AccountNumberGenerator generator;
private final DistributedLockService lockService;
public AccountResponse accountCreate(CreateAccountRequest request, String ip, String deviceId) {	
	Long userId = SecurityUtil.getUserId();
	CustomerDto customer = feignClient.getCustomerByUserId(userId);
	Long customerId = customer.getId();
	long seq = accountSequenceService.getNext();
	String accountNumber = generator.generator("4201", "10", seq);
	AccountValidator.validateInitialDeposit(request.getIntialDeposit());
	AccountValidator.validateCustomer(customer);
	log.info(
		    "Creating account customerId={} type={}",
		    customerId,
		    request.getAccountType()
		);
	Account account = Account.builder()
			.accountNumber(accountNumber)
			.customerId(customerId)
			.accountType(request.getAccountType())
			.balance(request.getIntialDeposit())
			.status(AccountStatus.ACTIVE)
			.createdAt(LocalDateTime.now())
			.updatedAt(LocalDateTime.now())
			.build();
	Account saved =  repo.save(account);
	kafkaTemplate.send("account-created-topic", account.getId().toString(), account);
	return map(saved);
}
@Transactional
public void updateBalance(BalanceUpdateRequest request) {
	log.info(
		    "Balance update started account={} transactionType={} amount={}",
		    request.getAccountNumber(),
		    request.getTransactionType(),
		    request.getNewBalance()
		);
	lockService.executeWithLock("account-lock:"+request.getAccountNumber(), () ->{
	Account account = repo.findByAccountNumberForUpdate(request.getAccountNumber()).orElseThrow(()-> new RuntimeException("Account not found"));
	
	if(account.getStatus() ==AccountStatus.BLOCKED) {
	throw new RuntimeException("Account Blocked");
	}
	BigDecimal currentBalance = account.getBalance();
	BigDecimal updatedBalance;
	if("DEBIT".equals(request.getTransactionType())) {
		if(currentBalance.compareTo(request.getNewBalance())<0) {
			throw new RuntimeException("insufficient balance");
			
		}
		updatedBalance = currentBalance.subtract(request.getNewBalance());
	}else {
		updatedBalance = currentBalance.add(request.getNewBalance());
		
	}
	account.setBalance(updatedBalance);
	repo.save(account);
	return null;
	
});
}
private AccountResponse map(Account a) {
	return AccountResponse.builder()
			.accountNumber(a.getAccountNumber())
			.type(a.getAccountType())
			.balance(a.getBalance())
			.status(a.getStatus())
			.build();
}
@Cacheable(value = "account-cache", key = "#accountNumber")
public AccountResponse getAccount(String accountNumber) {
	Account account = repo.findByAccountNumber(accountNumber).orElseThrow(()-> new RuntimeException("Account number not found"));
	return map(account);
}
}

package com.bank.transaction.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.bank.transaction.dto.TransactionRequest;
import com.bank.transaction.dto.TransactionResponse;
import com.bank.transaction.entity.Transaction;
import com.bank.transaction.enumrator.TransactionStatus;
import com.bank.transaction.enumrator.TransactionType;
import com.bank.transaction.repository.TransactionRepository;
import com.bank.transaction.service.TransactionService;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import com.bank.transaction.client.AccountClient;
import com.bank.transaction.dto.TransferRequest;
import com.bank.transaction.dto.TransferResponse;

@RestController
@RequestMapping("/transactions")
@RequiredArgsConstructor
public class TransactionController {
private final TransactionService service;
private final TransactionRepository repo;
private final AccountClient accountClient;

@PreAuthorize("hasAuthority('TRANSFER_MONEY')")
@PostMapping
public TransactionResponse transact(@RequestBody TransactionRequest request, HttpServletRequest httpRequest) {
	return service.process(request, httpRequest.getRemoteAddr(), httpRequest.getHeader("X-Device-Id"));
}

@PreAuthorize("hasAuthority('TRANSFER_MONEY')")
@PostMapping("/transfer")
 public TransferResponse transfer(@RequestBody TransferRequest request, HttpServletRequest httpRequest,
	 	@org.springframework.web.bind.annotation.RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
	return service.transfer(request, httpRequest.getRemoteAddr(), httpRequest.getHeader("X-Device-Id"), idempotencyKey);
}

@PreAuthorize("hasAuthority('TRANSFER_MONEY')")
@PostMapping("/transfer/preview")
public TransferResponse preview(@RequestBody TransferRequest request) {
	return service.preview(request);
}


@GetMapping("/me")
public List<Transaction> getMyTransactions(@RequestParam(required = false) String accountNumber) {
	List<com.bank.transaction.dto.AccountDto> myAccounts = accountClient.getMyAccounts();
	if (myAccounts == null || myAccounts.isEmpty()) {
		return java.util.Collections.emptyList();
	}
	List<String> myAccountNumbers = myAccounts.stream()
			.map(com.bank.transaction.dto.AccountDto::getAccountNumber)
			.filter(acc -> acc != null)
			.collect(java.util.stream.Collectors.toList());

	if (accountNumber != null && !accountNumber.trim().isEmpty()) {
		String targetAcc = accountNumber.trim();
		if (!myAccountNumbers.contains(targetAcc)) {
			throw new SecurityException("Unauthorized: Account does not belong to user");
		}
		return repo.findByAccountNumberOrderByCreateAtDesc(targetAcc);
	}
	return repo.findByAccountNumberInOrderByCreateAtDesc(myAccountNumbers);
}

@GetMapping("/account/{accountNumber}")
public List<Transaction> getTransactionsByAccount(@PathVariable String accountNumber) {
	List<com.bank.transaction.dto.AccountDto> myAccounts = accountClient.getMyAccounts();
	if (myAccounts == null || myAccounts.isEmpty()) {
		return java.util.Collections.emptyList();
	}
	List<String> myAccountNumbers = myAccounts.stream()
			.map(com.bank.transaction.dto.AccountDto::getAccountNumber)
			.collect(java.util.stream.Collectors.toList());
	if (!myAccountNumbers.contains(accountNumber.trim())) {
		throw new SecurityException("Unauthorized: Account does not belong to user");
	}
	return repo.findByAccountNumberOrderByCreateAtDesc(accountNumber.trim());
}

@PostMapping("/reverse/{txnId}")
public ResponseEntity<?> reverse(@PathVariable String txnId){
	Transaction txn = repo.findByTxnId(txnId).orElseThrow(()-> new RuntimeException("txn not found"));
	if(txn.getStatus() != TransactionStatus.SUCCESS) {
		throw new RuntimeException("only successful transaction can be reversed");
	}
	TransactionRequest reverseReq = new TransactionRequest();
	reverseReq.setAccountNumber(txn.getAccountNumber());
	reverseReq.setAmount(txn.getAmount());
	reverseReq.setType(txn.getType() == TransactionType.DEBIT ? TransactionType.CREDIT : TransactionType.DEBIT);
	return ResponseEntity.ok(service.process(reverseReq, "System", "reversal"));
}
}

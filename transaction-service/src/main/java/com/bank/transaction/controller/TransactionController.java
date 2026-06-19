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

@RestController
@RequestMapping("/transactions")
@RequiredArgsConstructor
public class TransactionController {
private final TransactionService service;
private final TransactionRepository repo;
@PreAuthorize("hasAuthority('TRANSFER_MONEY')")
@PostMapping
public TransactionResponse transact(@RequestBody TransactionRequest request, HttpServletRequest httpRequest) {
	return service.process(request, httpRequest.getRemoteAddr(), httpRequest.getHeader("X-Device-Id"));
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

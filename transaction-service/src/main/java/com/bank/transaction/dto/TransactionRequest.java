package com.bank.transaction.dto;

import java.math.BigDecimal;

import com.bank.transaction.enumrator.TransactionType;

import lombok.Data;

@Data
public class TransactionRequest {
	private String referenceId;
private String accountNumber;
private BigDecimal amount;
private TransactionType type;
}

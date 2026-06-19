package com.bank.transaction.dto;

import java.math.BigDecimal;

import com.bank.transaction.enumrator.TransactionStatus;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TransactionResponse {
private String txnId;
private TransactionStatus status;
private BigDecimal amount;
}

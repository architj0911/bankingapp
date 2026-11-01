package com.bank.transaction.dto;

import java.math.BigDecimal;

import lombok.Data;
import com.bank.transaction.enumrator.CurrencyCode;

@Data
public class AccountDto {
private String accountNumber;
private Long customerId;
private BigDecimal balance;
private String status;
private CurrencyCode currency;
}

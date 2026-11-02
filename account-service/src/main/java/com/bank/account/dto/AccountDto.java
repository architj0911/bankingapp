package com.bank.account.dto;

import java.math.BigDecimal;

import com.bank.account.enumrate.AccountStatus;
import com.bank.account.enumrate.CurrencyCode;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AccountDto {
	private String accountNumber;
	private Long customerId;
	private BigDecimal balance;
	private AccountStatus status;
	private CurrencyCode currency;
}

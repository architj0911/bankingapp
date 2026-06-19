package com.bank.account.dto;

import java.math.BigDecimal;

import com.bank.account.enumrate.AccountStatus;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AccountDto {
	private String accountNumber;
	private BigDecimal balance;
	private AccountStatus status;
}

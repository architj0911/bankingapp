package com.bank.account.dto;

import java.math.BigDecimal;

import com.bank.account.enumrate.AccountStatus;
import com.bank.account.enumrate.AccountType;

import lombok.Builder;
import lombok.Data;
@Data
@Builder
public class AccountResponse {
private String accountNumber;
private AccountType type;
private BigDecimal balance;
private AccountStatus status;
}

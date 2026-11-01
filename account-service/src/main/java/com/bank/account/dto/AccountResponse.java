package com.bank.account.dto;

import java.io.Serializable;
import java.math.BigDecimal;

import com.bank.account.enumrate.AccountStatus;
import com.bank.account.enumrate.AccountType;
import com.bank.account.enumrate.CurrencyCode;

import lombok.Builder;
import lombok.Data;
@Data
@Builder
public class AccountResponse implements Serializable {
private static final long serialVersionUID = 1L;
private String accountNumber;
private AccountType type;
private BigDecimal balance;
private AccountStatus status;
private CurrencyCode currency;
}


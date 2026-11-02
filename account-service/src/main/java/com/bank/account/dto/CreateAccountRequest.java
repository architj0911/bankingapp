package com.bank.account.dto;

import java.math.BigDecimal;

import com.bank.account.enumrate.AccountType;
import com.bank.account.enumrate.CurrencyCode;

import lombok.Data;
@Data
public class CreateAccountRequest {
private AccountType accountType;
private CurrencyCode currency = CurrencyCode.INR;
@com.fasterxml.jackson.annotation.JsonAlias({"initialDeposit", "intialDeposit"})
private BigDecimal intialDeposit;
}

package com.bank.transaction.dto;

import java.math.BigDecimal;

import lombok.Data;

@Data
public class AccountDto {
private String accountNumber;
private BigDecimal balance;
private String status;
}

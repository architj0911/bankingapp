package com.bank.account.validator;

import java.math.BigDecimal;

import com.bank.account.dto.CustomerDto;

public class AccountValidator {
public static void validateInitialDeposit(BigDecimal amount) {
	if(amount == null || amount.compareTo(BigDecimal.valueOf(1000))<0) {
		throw new RuntimeException(" minimum amount should be 1000");
	}
}
public static void validateCustomer(CustomerDto customer) {
	if(!"ACTIVE".equals(customer.getStatus())) {
		throw new RuntimeException("customer is not ACTIVE");
	}
	if(!"VERIFIED".equals(customer.getKycStatus())) {
		throw new RuntimeException("kyc not verified");
	}
}
}

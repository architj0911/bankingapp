package com.bank.account.validator;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import com.bank.account.dto.CustomerDto;

import static org.junit.jupiter.api.Assertions.*;

public class AccountValidatorTest {

    @Test
    void testValidateInitialDepositValid() {
        assertDoesNotThrow(() -> AccountValidator.validateInitialDeposit(BigDecimal.valueOf(1000)));
        assertDoesNotThrow(() -> AccountValidator.validateInitialDeposit(BigDecimal.valueOf(5000)));
    }

    @Test
    void testValidateInitialDepositInvalid() {
        assertThrows(RuntimeException.class, () -> AccountValidator.validateInitialDeposit(BigDecimal.valueOf(999)));
        assertThrows(RuntimeException.class, () -> AccountValidator.validateInitialDeposit(null));
    }

    @Test
    void testValidateCustomerKycVerified() {
        CustomerDto customer = new CustomerDto();
        customer.setStatus("ACTIVE");
        customer.setKycStatus("KYC_VERIFIED");
        assertDoesNotThrow(() -> AccountValidator.validateCustomer(customer));
    }

    @Test
    void testValidateCustomerLegacyVerified() {
        CustomerDto customer = new CustomerDto();
        customer.setStatus("ACTIVE");
        customer.setKycStatus("VERIFIED");
        assertDoesNotThrow(() -> AccountValidator.validateCustomer(customer));
    }

    @Test
    void testValidateCustomerInactive() {
        CustomerDto customer = new CustomerDto();
        customer.setStatus("PENDING");
        customer.setKycStatus("KYC_VERIFIED");
        assertThrows(RuntimeException.class, () -> AccountValidator.validateCustomer(customer));
    }

    @Test
    void testValidateCustomerKycPending() {
        CustomerDto customer = new CustomerDto();
        customer.setStatus("ACTIVE");
        customer.setKycStatus("PENDING");
        assertThrows(RuntimeException.class, () -> AccountValidator.validateCustomer(customer));
    }
}

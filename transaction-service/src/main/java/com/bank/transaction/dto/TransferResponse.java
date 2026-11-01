package com.bank.transaction.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import com.bank.transaction.enumrator.TransactionStatus;
import com.bank.transaction.enumrator.CurrencyCode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransferResponse {
    private String txnId;
    private String fromAccountNumber;
    private String toAccountNumber;
    private BigDecimal amount;
    private BigDecimal senderAmount;
    private CurrencyCode senderCurrency;
    private BigDecimal receiverAmount;
    private CurrencyCode receiverCurrency;
    private BigDecimal exchangeRate;
    private LocalDateTime exchangeRateTimestamp;
    private BigDecimal fee;
    private TransactionStatus status;
    private String description;
    private LocalDateTime createdAt;
}

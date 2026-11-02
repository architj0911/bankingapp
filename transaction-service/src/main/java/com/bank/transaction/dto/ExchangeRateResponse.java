package com.bank.transaction.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.bank.transaction.enumrator.CurrencyCode;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExchangeRateResponse {
    private CurrencyCode fromCurrency;
    private CurrencyCode toCurrency;
    private BigDecimal rate;
    private LocalDateTime timestamp;
}
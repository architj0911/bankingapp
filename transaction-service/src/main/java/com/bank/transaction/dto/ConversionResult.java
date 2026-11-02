package com.bank.transaction.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ConversionResult {
    private final BigDecimal amount;
    private final BigDecimal rate;
    private final LocalDateTime rateTimestamp;
}
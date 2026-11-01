package com.bank.transaction.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.springframework.stereotype.Service;

import com.bank.transaction.dto.ConversionResult;
import com.bank.transaction.dto.ExchangeRateResponse;
import com.bank.transaction.enumrator.CurrencyCode;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CurrencyConversionService {
    private final ExchangeRateService exchangeRateService;

    public ConversionResult convert(BigDecimal amount, CurrencyCode from, CurrencyCode to) {
        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException("Transfer amount must be greater than zero");
        }
        ExchangeRateResponse quote = exchangeRateService.getRate(from, to);
        int scale = to == CurrencyCode.JPY ? 0 : 2;
        BigDecimal converted = amount.multiply(quote.getRate()).setScale(scale, RoundingMode.HALF_UP);
        return new ConversionResult(converted, quote.getRate(), quote.getTimestamp());
    }
}
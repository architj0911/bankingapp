package com.bank.transaction.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.bank.transaction.dto.ExchangeRateResponse;
import com.bank.transaction.enumrator.CurrencyCode;
import com.bank.transaction.service.ExchangeRateService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/exchange-rates")
@RequiredArgsConstructor
public class ExchangeRateController {
    private final ExchangeRateService exchangeRateService;

    @GetMapping("/{from}/{to}")
    public ExchangeRateResponse getRate(@PathVariable CurrencyCode from, @PathVariable CurrencyCode to) {
        return exchangeRateService.getRate(from, to);
    }
}
package com.bank.transaction.service;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import com.bank.transaction.dto.ExchangeRateResponse;
import com.bank.transaction.enumrator.CurrencyCode;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExchangeRateService {
    private final StringRedisTemplate redisTemplate;
    private final RestClient restClient;

    @Value("${exchange-rate.cache-ttl:15m}")
    private Duration cacheTtl;

    @Value("${exchange-rate.max-stale:15m}")
    private Duration maxStale;

    public ExchangeRateResponse getRate(CurrencyCode from, CurrencyCode to) {
        if (from == null || to == null) {
            throw new IllegalArgumentException("Unsupported currency");
        }
        if (from == to) {
            return ExchangeRateResponse.builder()
                    .fromCurrency(from)
                    .toCurrency(to)
                    .rate(BigDecimal.ONE)
                    .timestamp(LocalDateTime.now())
                    .build();
        }

        String key = "exchange-rate:" + from + ":" + to;
        String cached = null;
        try {
            cached = redisTemplate.opsForValue().get(key);
        } catch (RuntimeException ex) {
            log.warn("Redis unavailable while reading FX quote for {} -> {}", from, to);
        }
        ExchangeRateResponse cachedRate = parseCached(cached, from, to);
        if (cachedRate != null && isFresh(cachedRate)) {
            return cachedRate;
        }

        try {
            RatePayload payload = restClient.get()
                    .uri("/{from}", from.name())
                    .retrieve()
                    .body(RatePayload.class);
            BigDecimal rate = payload == null || payload.rates() == null ? null : payload.rates().get(to.name());
            if (rate == null || rate.signum() <= 0) {
                throw new IllegalStateException("Exchange rate not returned for " + from + "/" + to);
            }
            ExchangeRateResponse fresh = ExchangeRateResponse.builder()
                    .fromCurrency(from)
                    .toCurrency(to)
                    .rate(rate)
                    .timestamp(LocalDateTime.now())
                    .build();
            try {
                redisTemplate.opsForValue().set(key, fresh.getRate() + "|" + fresh.getTimestamp(), cacheTtl.plus(maxStale));
            } catch (RuntimeException ex) {
                log.warn("Redis unavailable while caching FX quote for {} -> {}", from, to);
            }
            return fresh;
        } catch (Exception ex) {
            if (cachedRate != null && isWithinSafetyWindow(cachedRate)) {
                log.warn("FX provider unavailable; using recent cached rate for {} -> {}", from, to);
                return cachedRate;
            }
            throw new IllegalStateException("EXCHANGE_RATE_UNAVAILABLE: Currency conversion is temporarily unavailable. Please try again.", ex);
        }
    }

    private ExchangeRateResponse parseCached(String value, CurrencyCode from, CurrencyCode to) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            String[] parts = value.split("\\|", 2);
            return ExchangeRateResponse.builder()
                    .fromCurrency(from)
                    .toCurrency(to)
                    .rate(new BigDecimal(parts[0]))
                    .timestamp(LocalDateTime.parse(parts[1]))
                    .build();
        } catch (RuntimeException ex) {
            log.warn("Ignoring malformed cached FX quote for {} -> {}", from, to);
            return null;
        }
    }

    private boolean isFresh(ExchangeRateResponse quote) {
        return quote.getTimestamp().plus(cacheTtl).isAfter(LocalDateTime.now());
    }

    private boolean isWithinSafetyWindow(ExchangeRateResponse quote) {
        return quote.getTimestamp().plus(maxStale).isAfter(LocalDateTime.now());
    }

    private record RatePayload(String base_code, java.util.Map<String, BigDecimal> rates) { }
}
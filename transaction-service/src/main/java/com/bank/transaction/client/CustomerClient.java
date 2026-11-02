package com.bank.transaction.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.bank.transaction.config.FeignSecurityConfig;
import com.bank.transaction.dto.CustomerDto;

@FeignClient(name = "customer-service", url = "${customer-service.url:http://localhost:8082}", configuration = FeignSecurityConfig.class)
public interface CustomerClient {
    @GetMapping("/customers/internal")
    CustomerDto getCustomerByUserId(@RequestParam("userId") Long userId);
}

package com.bank.account.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.bank.account.config.FeignSecurityConfig;
import com.bank.account.dto.CustomerDto;

@FeignClient(name = "customer-service", url = "${customer-service.url:http://localhost:8082}", configuration = FeignSecurityConfig.class)
public interface CustomerClient {
    @GetMapping("/customers/internal")
    CustomerDto getCustomerByUserId(@RequestParam Long userId);
}

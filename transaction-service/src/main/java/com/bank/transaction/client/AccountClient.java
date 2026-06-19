package com.bank.transaction.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.bank.transaction.config.FeignSecurityConfig;
import com.bank.transaction.dto.AccountDto;
import com.bank.transaction.dto.BalanceUpdateRequest;

@FeignClient(name = "account-service", configuration = FeignSecurityConfig.class)
public interface AccountClient {
	@GetMapping("/accounts/internal")
	AccountDto getAccount(@RequestParam String accountNumber);
	@PostMapping("/accounts/internal/update-balance")
	void updateBalance(@RequestBody BalanceUpdateRequest request);

}

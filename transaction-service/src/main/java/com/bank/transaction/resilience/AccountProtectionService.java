package com.bank.transaction.resilience;

import java.util.concurrent.CompletableFuture;

import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

import com.bank.transaction.client.AccountClient;
import com.bank.transaction.dto.AccountDto;

import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AccountProtectionService {
	private final AccountClient accountClient;
	@Retry(name = "accountService", fallbackMethod = "accountFallBack")
	@Bulkhead(name = "accountService", type = Bulkhead.Type.SEMAPHORE, fallbackMethod = "accountFallBack")
	
	 @CircuitBreaker(name = "accountService", fallbackMethod = "accountFallBack")
	@TimeLimiter(name = "accountService")
	  public CompletableFuture<AccountDto> getAccountWithProtection(String accountNumber) {
		  RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
		  return CompletableFuture.supplyAsync(() -> {
			  try {
				  if (attributes != null) {
					  RequestContextHolder.setRequestAttributes(attributes);
				  }
				  return accountClient.getAccount(accountNumber);
			  } finally {
				  RequestContextHolder.resetRequestAttributes();
			  }
		  });
	  }
	  public CompletableFuture<AccountDto> accountFallBack(String accountNumber, Exception ex) {
		  throw new RuntimeException("Account service unavailable, Please try again later", ex);
	  }

}


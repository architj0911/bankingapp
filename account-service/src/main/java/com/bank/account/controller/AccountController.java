package com.bank.account.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.bank.account.dto.AccountDto;
import com.bank.account.dto.AccountResponse;
import com.bank.account.dto.BalanceUpdateRequest;
import com.bank.account.dto.CreateAccountRequest;
import com.bank.account.entity.Account;
import com.bank.account.repository.AccountRepository;
import com.bank.account.service.AccountService;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/accounts")
@RequiredArgsConstructor
public class AccountController {
private final AccountService service;
private final AccountRepository accountRepository;
@PreAuthorize("hasAuthority('CREATE_ACCOUNT')")
@PostMapping
public AccountResponse create(@RequestBody CreateAccountRequest request, HttpServletRequest httpRequest) {
	String ip = httpRequest.getRemoteAddr();
	String deviceId = httpRequest.getHeader("X-Device-Id");
	return service.accountCreate(request, ip, deviceId);
}
@GetMapping("/internal")
public AccountDto getAccountInternal(@RequestParam String accountNumber) {
    Account account = accountRepository.findByAccountNumber(accountNumber)
        .orElseThrow(() -> new RuntimeException("Account not found"));
    return AccountDto.builder()
        .accountNumber(account.getAccountNumber())
        .balance(account.getBalance())
        .status(account.getStatus())
        .build();
}

@PostMapping("/internal/update-balance")
public void updateBalance(@RequestBody BalanceUpdateRequest request) {
    Account account = accountRepository.findByAccountNumber(request.getAccountNumber())
        .orElseThrow(() -> new RuntimeException("Account not found"));
    account.setBalance(request.getNewBalance());
    accountRepository.save(account);
}
}

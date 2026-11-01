package com.bank.customer.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.bank.customer.dto.CustomerDto;
import com.bank.customer.dto.CustomerRequest;
import com.bank.customer.dto.CustomerResponse;
import com.bank.customer.service.CustomerService;

import lombok.RequiredArgsConstructor;

import com.bank.customer.security.SecurityUtil;

@RestController
@RequestMapping("/customers")
@RequiredArgsConstructor
public class CustomerController {
private final CustomerService service;

@PostMapping
public CustomerResponse create(@RequestBody CustomerRequest request) {
	return service.create(request);
}

@GetMapping("/me")
public CustomerResponse getMe() {
	Long userId = SecurityUtil.getUserId();
	return service.getProfileByUserId(userId);
}

@GetMapping("/internal")
public CustomerDto getByUserId(@RequestParam Long userId) {
	return service.getByUserId(userId);
}
}

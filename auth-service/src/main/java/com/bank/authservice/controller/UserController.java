package com.bank.authservice.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.bank.authservice.domain.User;
import com.bank.authservice.dto.RegisterRequest;
import com.bank.authservice.service.UserService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {
private final UserService userService;
@PostMapping("/register")
public User register(@RequestBody RegisterRequest request) {
	return userService.register(request);
}
}

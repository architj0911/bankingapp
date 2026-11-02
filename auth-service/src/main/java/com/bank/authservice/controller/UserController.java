package com.bank.authservice.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.bank.authservice.domain.User;
import com.bank.authservice.dto.RegisterRequest;
import com.bank.authservice.service.UserService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
@Slf4j
public class UserController {
	private final UserService userService;

	@PostMapping("/register")
	public ResponseEntity<?> register(@RequestBody RegisterRequest request) {
		try {
			User user = userService.register(request);
			return ResponseEntity.ok(Map.of(
				"message", "User registered successfully",
				"username", user.getUserName(),
				"id", user.getId()
			));
		} catch (Exception e) {
			log.error("Registration failed: {}", e.getMessage(), e);
			return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
		}
	}
}

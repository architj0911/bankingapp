package com.bank.authservice.service;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;

import com.bank.authservice.domain.User;
import com.bank.authservice.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthenticationService {
	private final UserRepository userRepo;
	public User authenticate(String userName) {
		return userRepo.findByUserName(userName).filter(User::isEnabled).orElseThrow(()-> new BadCredentialsException("Invalid user or disabled user"));
	}
}

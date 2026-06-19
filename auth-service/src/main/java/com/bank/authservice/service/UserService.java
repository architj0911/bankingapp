package com.bank.authservice.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.bank.authservice.domain.User;
import com.bank.authservice.dto.RegisterRequest;
import com.bank.authservice.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserService {
	private final UserRepository userRepo;
	private final PasswordEncoder passwordEncoder;
	public User register(RegisterRequest request) {
		User user = User.builder().userName(request.getUsername()).password(passwordEncoder.encode(request.getPassword()))
				.branchId(request.getBranchId()).employeeCode(request.getEmployeeCode()).enabled(true)
				 .accountNonLocked(true)
				    .failedAttempts(0)
				    .lockTime(null) 
				.email(request.getEmail()).phoneNumber(request.getPhoneNumber())
				.build();
		return userRepo.save(user);
	}
	public User findByUserName(String userName) {
		User user = userRepo.findByUserName(userName).orElseThrow(()-> new RuntimeException("User not fount: "+userName));
	return user;
	}
}

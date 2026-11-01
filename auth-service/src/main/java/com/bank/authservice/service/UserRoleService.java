package com.bank.authservice.service;

import org.springframework.stereotype.Service;

import com.bank.authservice.domain.Role;
import com.bank.authservice.domain.User;
import com.bank.authservice.repository.RoleRepository;
import com.bank.authservice.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor

public class UserRoleService {
private final UserRepository userRepo;
private final RoleRepository roleRepo;
public void assignRoleToUser(Long userId, Long roleId) {
	User user = userRepo.findById(userId).orElseThrow(()-> new RuntimeException("User not found"));
	Role role = roleRepo.findById(roleId).orElseThrow(()-> new RuntimeException("Role not found"));
	user.getRoles().add(role);
	userRepo.save(user);
}
public void removeRoleFromUser(Long userId, Long roleId) {
	User user = userRepo.findById(userId).orElseThrow(()-> new RuntimeException("User not found"));
	user.getRoles().removeIf(role -> role.getId().equals(roleId));
	userRepo.save(user);
}
}

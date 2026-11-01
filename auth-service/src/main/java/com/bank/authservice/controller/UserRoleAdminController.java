package com.bank.authservice.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.bank.authservice.service.UserRoleService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/admin/user-role")
@RequiredArgsConstructor
public class UserRoleAdminController {
private final UserRoleService service;
@PostMapping("/user/{userId}/role/{roleId}")
public ResponseEntity<?> assignRole(@PathVariable Long userId, @PathVariable Long roleId){
	service.assignRoleToUser(userId, roleId);
	return ResponseEntity.ok("role assigned successfully");
}
@DeleteMapping("/user/{userId}/role/{roleId}")
public ResponseEntity<?> removeRole(@PathVariable Long userId, @PathVariable Long roleId){
	service.removeRoleFromUser(userId, roleId);
	return ResponseEntity.ok("Role Removed successfully");
}
}

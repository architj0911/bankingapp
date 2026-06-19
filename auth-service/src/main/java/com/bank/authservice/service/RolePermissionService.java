package com.bank.authservice.service;

import org.springframework.stereotype.Service;

import com.bank.authservice.domain.Permission;
import com.bank.authservice.domain.Role;
import com.bank.authservice.repository.PermissionRepository;
import com.bank.authservice.repository.RoleRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RolePermissionService {
private final PermissionRepository permissionRepo;
private final RoleRepository roleRepo;
public void assignPermissionToRole(Long roleId, Long permissionId) {
	Role role = roleRepo.findById(roleId).orElseThrow(() -> new RuntimeException("Role Not Found"));
	Permission permission = permissionRepo.findById(permissionId).orElseThrow(()-> new RuntimeException("Permisstion not found"));
	role.getPermissions().add(permission);
	roleRepo.save(role);
}
public void removePermissionFromRole(Long roleId, Long permissionId) {
	Role role = roleRepo.findById(roleId).orElseThrow(() -> new RuntimeException("Role Not Found"));
	role.getPermissions().removeIf(permission -> permission.getId().equals(permissionId));
	roleRepo.save(role);
}
}

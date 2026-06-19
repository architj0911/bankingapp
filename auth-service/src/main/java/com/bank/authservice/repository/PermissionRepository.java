package com.bank.authservice.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.bank.authservice.domain.Permission;

public interface PermissionRepository extends JpaRepository<Permission, Long> {
Optional<Permission> findByName(String name);
}

package com.bank.authservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.bank.authservice.audit.LoginAuditEntity;

public interface LoginAuditRepository extends JpaRepository<LoginAuditEntity, Long>{

}

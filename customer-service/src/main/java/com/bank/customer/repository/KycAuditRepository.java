package com.bank.customer.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.bank.customer.entity.KycAuditEvent;

public interface KycAuditRepository extends JpaRepository<KycAuditEvent, Long> {
}

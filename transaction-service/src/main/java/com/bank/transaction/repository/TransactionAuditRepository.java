package com.bank.transaction.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.bank.transaction.entity.TransactionAudit;

public interface TransactionAuditRepository extends JpaRepository<TransactionAudit, Long> {

}

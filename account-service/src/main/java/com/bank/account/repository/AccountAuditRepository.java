package com.bank.account.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.bank.account.entity.AccountAudit;

public interface AccountAuditRepository extends JpaRepository<AccountAudit, Long> {

}

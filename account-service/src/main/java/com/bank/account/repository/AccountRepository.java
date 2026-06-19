package com.bank.account.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import com.bank.account.entity.Account;

import feign.Param;
import jakarta.persistence.LockModeType;

public interface AccountRepository extends JpaRepository<Account, Long> {
Optional<Account> findByAccountNumber(String accountNumber);
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("""
		SELECT a FROM Account a WHERE a.accountNumber = :accountNumber
""")
Optional<Account> findByAccountNumberForUpdate(@Param("accountNumber") String accountNumber);
}

package com.bank.account.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import com.bank.account.entity.Account;

import feign.Param;
import jakarta.persistence.LockModeType;

public interface AccountRepository extends JpaRepository<Account, Long> {
Optional<Account> findByAccountNumber(String accountNumber);
List<Account> findByCustomerId(Long customerId);
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("""
		SELECT a FROM Account a WHERE a.accountNumber = :accountNumber
""")
Optional<Account> findByAccountNumberForUpdate(@Param("accountNumber") String accountNumber);
}

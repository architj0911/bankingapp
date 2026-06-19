package com.bank.transaction.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.bank.transaction.entity.Transaction;

public interface TransactionRepository extends JpaRepository<Transaction, Long>{
	Optional<Transaction> findByTxnId(String txnId);
	Optional<Transaction> findByReferenceId(String referenceId);

}

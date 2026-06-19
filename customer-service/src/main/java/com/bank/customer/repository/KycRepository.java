package com.bank.customer.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.bank.customer.entity.KycDetails;

public interface KycRepository extends JpaRepository<KycDetails, Long>{
Optional<KycDetails> findByCustomerId(Long customerId);
}

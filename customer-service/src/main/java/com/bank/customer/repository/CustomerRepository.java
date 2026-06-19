package com.bank.customer.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.bank.customer.entity.Customer;

public interface CustomerRepository extends JpaRepository<Customer, Long>{
Optional<Customer> findByUserIdAndIsDeletedFalse(Long userId);
}

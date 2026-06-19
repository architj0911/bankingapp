package com.bank.authservice.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.bank.authservice.domain.Client;


public interface ClientRepository extends JpaRepository<Client, Long>{
Optional<Client> findByClientId(String clientId);
}

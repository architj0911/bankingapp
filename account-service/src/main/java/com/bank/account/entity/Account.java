package com.bank.account.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.bank.account.enumrate.AccountStatus;
import com.bank.account.enumrate.AccountType;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "accounts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Account {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
private Long id;
private String accountNumber;
private Long customerId;
@Enumerated(EnumType.STRING)
private AccountType accountType;
@Enumerated(EnumType.STRING)
private AccountStatus status;

private BigDecimal balance;
private LocalDateTime createdAt;
private LocalDateTime updatedAt;
@Version
private Long version;
}

package com.bank.account.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
@Entity
@Table(name = "account_audit")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AccountAudit {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
private Long id;
private String accountNumber;
private Long userId;
private String action;
private String ip;
private String deviceId;
private LocalDateTime createdAt;
}

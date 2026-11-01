package com.bank.transaction.entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.ValueGenerationType;

import jakarta.persistence.Column;
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
@Table(name = "transactionAudit")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionAudit {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
private Long id;
private String txnId;
private Long userId;
private String action;
private String ip;
private String deviceId;
@Column(name = "created_at")
private LocalDateTime createdAt;
}

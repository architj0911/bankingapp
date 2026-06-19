package com.bank.transaction.entity;

import java.math.BigDecimal;
import java.time.DateTimeException;
import java.time.LocalDateTime;

import com.bank.transaction.enumrator.TransactionStatus;
import com.bank.transaction.enumrator.TransactionType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
@Table(name = "transactions")
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Transaction {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
private Long id;
	private String txnId;
	private String accountNumber;
	@Enumerated(EnumType.STRING)
	private TransactionType type;
		@Enumerated(EnumType.STRING)
		private TransactionStatus status;
		private BigDecimal amount;
		@Column(name = "reference_id", unique = true)
		private String referenceId;
		@Column(name = "created_at")
		private LocalDateTime createAt;
		@Column(name = "updated_at")
		private LocalDateTime updatedAt;
}

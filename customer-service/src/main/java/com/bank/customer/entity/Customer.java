package com.bank.customer.entity;

import java.time.LocalDateTime;

import com.bank.customer.enums.CustomerStatus;

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
@Table(name = "customers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Customer {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
private Long id;
private String name;
private String phoneNumber;
private String email;
private Long userId;
@Enumerated(EnumType.STRING)
private CustomerStatus status;
private LocalDateTime createdAt;
private LocalDateTime updatedAt;
private String createdBy;
private boolean isDeleted = false;
}

package com.bank.customer.entity;

import com.bank.customer.enums.KycStatus;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "kyc_details")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class KycDetails {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
private Long id;
private String panNumber;
private String aadharNumber;
@Enumerated(EnumType.STRING)
private KycStatus status;
@ManyToOne
@JoinColumn(name = "customer_id")
private Customer customer;
}

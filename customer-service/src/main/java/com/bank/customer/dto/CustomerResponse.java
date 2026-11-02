package com.bank.customer.dto;

import java.io.Serializable;
import com.bank.customer.enums.CustomerStatus;

import lombok.Builder;
import lombok.Data;
@Data
@Builder
public class CustomerResponse implements Serializable {
private static final long serialVersionUID = 1L;
private Long id;
private Long userId;
private String name;
private String email;
private String phone;
private CustomerStatus status;
private String kycStatus;
}


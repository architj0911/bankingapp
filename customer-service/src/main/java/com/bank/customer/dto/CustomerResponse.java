package com.bank.customer.dto;

import com.bank.customer.enums.CustomerStatus;

import lombok.Builder;
import lombok.Data;
@Data
@Builder
public class CustomerResponse {
private Long id;
private String name;
private String email;
private String phone;
private CustomerStatus status;
}

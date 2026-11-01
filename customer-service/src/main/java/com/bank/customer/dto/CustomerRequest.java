package com.bank.customer.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CustomerRequest {
@NotBlank(message = "name is required")
	private String name;
@NotBlank(message = "phone is required")
private String phone;
@Email(message = "invalid email")
private String email;
}

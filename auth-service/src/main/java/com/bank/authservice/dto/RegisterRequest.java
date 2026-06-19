package com.bank.authservice.dto;

import lombok.Data;

@Data
public class RegisterRequest {
	private String username;
	private String password;
	private String branchId;
	private String employeeCode;
	private String phoneNumber;
	private String email;
}

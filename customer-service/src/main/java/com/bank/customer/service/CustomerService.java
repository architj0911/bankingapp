package com.bank.customer.service;

import java.time.LocalDateTime;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import com.bank.customer.dto.CustomerDto;
import com.bank.customer.dto.CustomerRequest;
import com.bank.customer.dto.CustomerResponse;
import com.bank.customer.entity.Customer;
import com.bank.customer.entity.KycDetails;
import com.bank.customer.enums.CustomerStatus;
import com.bank.customer.exception.ResourceNotFoundException;
import com.bank.customer.repository.CustomerRepository;
import com.bank.customer.repository.KycRepository;
import com.bank.customer.security.SecurityUtil;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CustomerService {
private final CustomerRepository customerRepo;
private final KycRepository kycRepo;

public CustomerResponse create(CustomerRequest request) {
	Long userId = SecurityUtil.getUserId();
	customerRepo.findByUserIdAndIsDeletedFalse(userId).ifPresent(c -> {throw new RuntimeException("CUstomer already exist");});
	 Customer customer = Customer.builder().name(request.getName())
	.email(request.getEmail())
	.phoneNumber(request.getPhone())
	.userId(userId)
	.createdBy(SecurityUtil.getUserName())
	.createdAt(LocalDateTime.now())
	.updatedAt(LocalDateTime.now())
	.status(CustomerStatus.PENDING).build();
	 return mapToResponse(customerRepo.save(customer));
}
@Cacheable( 
value="customer-cache", 
key="#id" 
)
public CustomerResponse get(Long id) {
	Customer customer = customerRepo.findById(id).orElseThrow(() -> new ResourceNotFoundException("Customer not found"));
	return mapToResponse(customer);
}

private CustomerResponse mapToResponse(Customer c) {
	return CustomerResponse.builder().id(c.getId())
			.name(c.getName())
			.email(c.getEmail())
			.phone(c.getPhoneNumber())
			.status(c.getStatus()).build();
}
@Cacheable( 
value="customer-user-cache", 
key="#userId" 
)
public CustomerDto getByUserId(Long userId) {
	Customer customer =  customerRepo.findByUserIdAndIsDeletedFalse(userId).orElseThrow(()-> new RuntimeException("Customer not found"));
KycDetails kyc = kycRepo.findByCustomerId(customer.getId()).orElse(null);
return mapToDto(customer, kyc);
}
private CustomerDto mapToDto(Customer c, KycDetails kyc) {
	CustomerDto dto = new CustomerDto();
	dto.setId(c.getId());
	dto.setStatus(c.getStatus().name());
	dto.setKycStatus(kyc.getStatus().name());
	return dto;
}
}

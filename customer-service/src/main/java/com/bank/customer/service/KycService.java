package com.bank.customer.service;

import com.bank.customer.config.SetuConfig;
import com.bank.customer.entity.*;
import com.bank.customer.enums.CustomerStatus;
import com.bank.customer.enums.KycStatus;
import com.bank.customer.repository.CustomerRepository;
import com.bank.customer.repository.KycRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class KycService {

    private final KycRepository kycRepo;
    private final CustomerRepository customerRepo;
    private final SetuConfig setuClient;

    public KycDetails submitKyc(Long customerId, KycDetails request) {

        Customer customer = customerRepo.findById(customerId)
                .orElseThrow(() -> new RuntimeException("Customer not found"));

        request.setCustomer(customer);

        boolean panVerified = false;
        boolean aadhaarVerified = false;

        try {
        //    panVerified = setuClient.verifyPan(request.getPanNumber()).block();
        //    aadhaarVerified = setuClient.verifyAadhaar(request.getAadharNumber()).block();
        } catch (Exception ex) {
            request.setStatus(KycStatus.PENDING);
            return kycRepo.save(request);
        }

        if (panVerified && aadhaarVerified) {
            request.setStatus(KycStatus.VERIFIED);
            customer.setStatus(CustomerStatus.ACTIVE);
        } else {
            request.setStatus(KycStatus.REJECTED);
        }

        customerRepo.save(customer);
        return kycRepo.save(request);
    }
}
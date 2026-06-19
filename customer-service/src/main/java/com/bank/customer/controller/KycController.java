package com.bank.customer.controller;

import com.bank.customer.entity.KycDetails;
import com.bank.customer.service.KycService;
import com.bank.customer.service.SetuService;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/kyc")
@RequiredArgsConstructor
public class KycController {

    private final KycService kycService;
    private final SetuService setuService;

    @PostMapping("/{customerId}")
    public KycDetails submitKyc(@PathVariable Long customerId,
                                @RequestBody KycDetails request) {
        return kycService.submitKyc(customerId, request);
    }
    @PostMapping("/verify_pan")
    public ResponseEntity<?> verifyPan(@RequestParam String pan){
    	 String response = setuService.verifyPan(pan);
    	 return ResponseEntity.ok(response);
    }
    @GetMapping("/digilocker")
    public ResponseEntity<?> digilocaker(){
    	String response =  setuService.createDigilockerRequest();
    	return ResponseEntity.ok(response);
    }
    @GetMapping("/callback")
    public ResponseEntity<String> callback(@RequestParam("id") String requestId,
    		@RequestParam boolean success,
    		@RequestParam String scope){
    	System.out.println("this is call back function");
    	if(!success) {
    		return ResponseEntity.ok("user denied consent");
    	}
    if(scope!=null && scope.contains("ADHAR")) {
    	String aadharData =  setuService.fetchAadhar(requestId);
    	 System.out.println("Aahar data "+aadharData);
    }
   
    return ResponseEntity.ok("callback received");
    }
    @GetMapping("/status/{id}")
    public ResponseEntity<?> checkStatus(@PathVariable String id) {
        String status = setuService.checkStatus(id);
        return ResponseEntity.ok(status);
    }
}
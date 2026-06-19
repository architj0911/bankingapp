package com.bank.customer.service;

import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import lombok.RequiredArgsConstructor;


@Service
@RequiredArgsConstructor
public class SetuService {
private final WebClient webClient;
@Value("${setu.base-url}")
private String baseUrl;
@Value("${setu.client-id}")
private String clientId;
@Value("${setu.client-secret}")
private String clientSecret;
@Value("${setu.pan-product-id}")
private String panProductId;
@Value("${setu.digilocker-product-key}")
private String digilockerProductId;
public String verifyPan(String pan) {
	Map<String, Object> body = Map.of(
			"pan",pan,
			"consent","Y",
			"reason","KYC verification");
	return webClient.post().uri(baseUrl+"/api/verify/pan")
			.header("x-client-id", clientId)
			.header("x-client-secret", clientSecret)
			.header("x-product-instance-id", panProductId)
			.contentType(MediaType.APPLICATION_JSON)
			.bodyValue(body)
			.retrieve().bodyToMono(String.class).block();
}
public String createDigilockerRequest() {
	Map<String, Object> body = Map.of("redirectUrl","http://localhost:8080/api/kyc/callback");
	return webClient.post()
			.uri(baseUrl+"/api/digilocker")
			.header("x-client-id", clientId)
			.header("x-client-secret", clientSecret)
			.header("x-product-instance-id", digilockerProductId)
			.contentType(MediaType.APPLICATION_JSON)
			.bodyValue(body)
			.retrieve().bodyToMono(String.class).block();
}
public String checkStatus(String requestId) {
	return webClient.get()
			.uri(baseUrl+"/api/digilocker/"+requestId +"/status")
			.header("x-client-id", clientId)
			.header("x-client-secret", clientSecret)
			.header("x-product-instance-id", digilockerProductId)
			.retrieve().bodyToMono(String.class).block();
}
public String fetchAadhar(String requestId) {
	return webClient.get()
			.uri(baseUrl+"/api/digilocker/"+requestId + "/aadhaar")
			.header("x-client-id", clientId)
			.header("x-client-secret", clientSecret)
			.header("x-product-instance-id", digilockerProductId)
			.retrieve().bodyToMono(String.class).block();
}
}

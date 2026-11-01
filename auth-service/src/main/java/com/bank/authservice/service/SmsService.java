package com.bank.authservice.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class SmsService {

	@Value("${twilio.sid:}")
	private String accountId;
	@Value("${twilio.token:}")
	private String authToken;
	
	@Value("${twilio.number:}")
	private String fromNumber;

	@PostConstruct
	public void init() {
		try {
			if (isRealTwilioConfig()) {
				Twilio.init(accountId.trim(), authToken.trim());
				log.info("Twilio client initialized successfully.");
			} else {
				log.info("[LOCAL DEV] Twilio external SMS integration disabled or placeholder credentials detected.");
			}
		} catch (Exception e) {
			log.warn("Failed to initialize Twilio client: {}", e.getMessage());
		}
	}

	private boolean isRealTwilioConfig() {
		if (accountId == null || accountId.trim().isEmpty() ||
		    authToken == null || authToken.trim().isEmpty() ||
		    fromNumber == null || fromNumber.trim().isEmpty()) {
			return false;
		}
		String cleanSid = accountId.trim();
		if (!cleanSid.startsWith("AC")) return false;
		if (cleanSid.contains("your_") || cleanSid.contains("placeholder") || cleanSid.length() < 30) return false;
		return true;
	}

	public void sendOtp(String phone, String otp) {
		if (phone == null || phone.trim().isEmpty()) {
			log.info("[LOCAL DEV] SMS OTP dispatch skipped because no phone number is configured.");
			return;
		}
		try {
			if (isRealTwilioConfig()) {
				Message.creator(new PhoneNumber(phone.trim()), new PhoneNumber(fromNumber.trim()), "Your Banking OTP: " + otp).create();
				log.info("SMS OTP sent to {} via Twilio.", phone);
			} else {
				log.info("[LOCAL DEV] SMS OTP delivery is disabled; no code was logged.");
			}
		} catch (Exception e) {
			log.warn("[LOCAL DEV] SMS delivery failed: {}", e.getMessage());
		}
	}
}

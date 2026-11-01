package com.bank.authservice.event;

import java.io.IOException;

import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import com.bank.authservice.domain.User;
import com.bank.authservice.repository.UserRepository;
import com.bank.authservice.service.EmailService;
import com.bank.authservice.service.OtpService;
import com.bank.authservice.service.SmsService;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class OtpLoginSuccessHandler implements AuthenticationSuccessHandler {
	 private final SmsService smsService;
	 private final OtpService otpService;
	 private final EmailService emailService;
	 private final UserRepository userRepo;
	 @Override
	 public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
			Authentication authentication) throws IOException, ServletException {
		String userName = authentication.getName();
		User user = userRepo.findByUserName(userName).orElseThrow(()-> new RuntimeException("User not found"));
		String otp =  otpService.generateOtp(userName,"dss","sds");
		if(user.getPhoneNumber() != null) {
			smsService.sendOtp(user.getPhoneNumber(), otp);
		}
		if(user.getEmail() != null) {
			emailService.sendOtp(user.getEmail(), otp);
		}
	 }
}

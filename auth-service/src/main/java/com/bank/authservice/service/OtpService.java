package com.bank.authservice.service;

import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OtpService {
	private final StringRedisTemplate redisTemplate;
	private static final String OTP_KEY = "otp";
	private static final String META_KEY = "otp_meta";
	public String generateOtp(String userName, String ip, String deviceId) {
		String otp = String.valueOf(ThreadLocalRandom.current().nextInt(100000, 999999));
		redisTemplate.opsForValue().set(OTP_KEY+userName, otp, Duration.ofMinutes(30));
		String meta = ip+"|"+deviceId;
		redisTemplate.opsForValue().set(META_KEY+userName, meta, Duration.ofMinutes(30));
		return otp;
	}
	public boolean verifuOtp(String userName, String otp, String ip, String deviceId) {
		String storedOtp = redisTemplate.opsForValue().get(OTP_KEY+userName);
		String meta = redisTemplate.opsForValue().get(META_KEY+userName);
		if(storedOtp== null || meta== null) {
			return false;
		}
		String []parts = meta.split("\\|");
		String storedIp = parts[0];
		String storedDeviceId = parts[1];
		boolean valid = storedOtp.equals(otp) && storedIp.equals(ip) && storedDeviceId.equals(deviceId);
		redisTemplate.delete(OTP_KEY+userName);
		redisTemplate.delete(META_KEY+userName);
		return valid;
	}
}

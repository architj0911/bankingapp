package com.bank.customer.exception;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

public final class ApiErrors {
	private ApiErrors() {}

	public static Map<String, Object> body(int status, String code, String message) {
		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("timestamp", Instant.now().toString());
		payload.put("status", status);
		payload.put("code", code);
		payload.put("message", message);
		return payload;
	}
}

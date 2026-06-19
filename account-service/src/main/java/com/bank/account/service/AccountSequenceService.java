package com.bank.account.service;


import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AccountSequenceService {
private final JdbcTemplate jdbcTemplate;
public long getNext() {
	return jdbcTemplate.queryForObject("SELECT nextval('account_seq')", Long.class);
}
}

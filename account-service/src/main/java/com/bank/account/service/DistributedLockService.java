package com.bank.account.service;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DistributedLockService {
private final RedissonClient redissonClient;
public <T> T executeWithLock(String redisKey, Supplier<T> action) {
	RLock lock = redissonClient.getLock(redisKey);
	try {
		boolean acquired = lock.tryLock(5,30, TimeUnit.SECONDS);
		if(!acquired) {
			throw new RuntimeException("Resource is busy");
		}
		return action.get();
	}catch (InterruptedException e) {
		Thread.currentThread().interrupt();
		throw new RuntimeException("Lock interrupted");
		}finally {
			if(lock.isHeldByCurrentThread()) {
				lock.unlock();
			}
		}
}
}

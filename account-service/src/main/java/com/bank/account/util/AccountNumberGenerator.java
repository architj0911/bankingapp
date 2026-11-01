package com.bank.account.util;

import org.springframework.stereotype.Component;

@Component
public class AccountNumberGenerator {
public String generator(String branchCode, String typeCode, long sequence) {
	String seq = String.format("%010d", sequence);
	String base = branchCode + typeCode  + seq;
	int checksum = calculateCheckDigit(base);
	return base+checksum;
}
private int calculateCheckDigit(String number) {
	int sum =0;
			for(int i=0; i<number.length();i++) {
				int digit = Character.getNumericValue(number.charAt(i));
				if(i%2==0) {
					digit*=2;
					if(digit>9)
						digit-=9;
				}
				sum+=digit;
			}
	return (10- (sum%10))%10;
}
}

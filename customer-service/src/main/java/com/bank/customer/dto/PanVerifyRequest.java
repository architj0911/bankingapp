package com.bank.customer.dto;

import lombok.Data;

@Data
public class PanVerifyRequest {
private String pan;
private String consent;
private String reason;
}

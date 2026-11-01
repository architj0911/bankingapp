package com.bank.transaction.dto;

import java.math.BigDecimal;
import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Data;

@Data
public class TransferRequest {
    @JsonAlias("senderAccountNumber")
    private String fromAccountNumber;
    @JsonAlias("receiverAccountNumber")
    private String toAccountNumber;
    private BigDecimal amount;
    private String description;
}

package com.bank.transaction.dto;

import java.io.Serializable;
import lombok.Data;

@Data
public class CustomerDto implements Serializable {
    private static final long serialVersionUID = 1L;
    private Long id;
    private String status;
    private String kycStatus;
}

package com.example.agentic_ai.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class PaymentRequest {
    private Long orderId;
    private String customerId;
    private BigDecimal amount;
    private String paymentMethod;
    private String notes;
}

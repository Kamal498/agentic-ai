package com.example.agentic_ai.dto;

import com.example.agentic_ai.domain.Payment;
import com.example.agentic_ai.enums.PaymentStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class PaymentResponse {
    private Long id;
    private String paymentId;
    private Long orderId;
    private String customerId;
    private PaymentStatus status;
    private BigDecimal amount;
    private String paymentMethod;
    private LocalDateTime paymentDate;
    private String transactionId;
    private String notes;

    public static PaymentResponse from(Payment p) {
        PaymentResponse r = new PaymentResponse();
        r.setId(p.getId());
        r.setPaymentId(p.getPaymentId());
        r.setOrderId(p.getOrderId());
        r.setCustomerId(p.getCustomerId());
        r.setStatus(p.getStatus());
        r.setAmount(p.getAmount());
        r.setPaymentMethod(p.getPaymentMethod());
        r.setPaymentDate(p.getPaymentDate());
        r.setTransactionId(p.getTransactionId());
        r.setNotes(p.getNotes());
        return r;
    }
}

package com.example.agentic_ai.domain;

import com.example.agentic_ai.enums.PaymentStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "payments")
@Getter
@Setter
@NoArgsConstructor
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String paymentId;

    @Column(nullable = false)
    private Long orderId;

    @Column(nullable = false)
    private String customerId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false)
    private String paymentMethod;

    @Column(nullable = false)
    private LocalDateTime paymentDate;

    private String transactionId;

    @Column(length = 500)
    private String notes;

    @PrePersist
    protected void onCreate() {
        if (paymentDate == null) paymentDate = LocalDateTime.now();
        if (status == null) status = PaymentStatus.PENDING;
    }

    public String toVectorText() {
        StringBuilder sb = new StringBuilder();
        sb.append("Payment ID: ").append(paymentId).append(". ");
        sb.append("Order ID: ").append(orderId).append(". ");
        sb.append("Customer ID: ").append(customerId).append(". ");
        sb.append("Status: ").append(status).append(". ");
        sb.append("Amount: $").append(amount).append(". ");
        sb.append("Payment Method: ").append(paymentMethod).append(". ");
        sb.append("Payment Date: ").append(paymentDate).append(". ");
        if (transactionId != null) sb.append("Transaction ID: ").append(transactionId).append(". ");
        if (notes != null && !notes.isBlank()) sb.append("Notes: ").append(notes).append(". ");
        return sb.toString().trim();
    }
}

package com.example.agentic_ai.repository;

import com.example.agentic_ai.domain.Payment;
import com.example.agentic_ai.enums.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByPaymentId(String paymentId);

    List<Payment> findByOrderId(Long orderId);

    List<Payment> findByCustomerId(String customerId);

    List<Payment> findByStatus(PaymentStatus status);

    Optional<Payment> findByTransactionId(String transactionId);
}

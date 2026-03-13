package com.example.agentic_ai.service;

import com.example.agentic_ai.domain.Payment;
import com.example.agentic_ai.dto.PaymentRequest;
import com.example.agentic_ai.dto.PaymentResponse;
import com.example.agentic_ai.enums.PaymentStatus;
import com.example.agentic_ai.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final VectorStoreService vectorStoreService;

    @Transactional
    public PaymentResponse createPayment(PaymentRequest request) {
        Payment payment = new Payment();
        payment.setPaymentId("PAY-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        payment.setOrderId(request.getOrderId());
        payment.setCustomerId(request.getCustomerId());
        payment.setAmount(request.getAmount());
        payment.setPaymentMethod(request.getPaymentMethod());
        payment.setStatus(PaymentStatus.COMPLETED);
        payment.setTransactionId("TXN-" + UUID.randomUUID().toString().substring(0, 12).toUpperCase());
        payment.setNotes(request.getNotes());

        Payment saved = paymentRepository.save(payment);
        vectorStoreService.indexPayment(saved);
        log.info("Created payment: {}", saved.getPaymentId());
        return PaymentResponse.from(saved);
    }

    public PaymentResponse getByPaymentId(String paymentId) {
        return paymentRepository.findByPaymentId(paymentId)
                .map(PaymentResponse::from)
                .orElseThrow(() -> new RuntimeException("Payment not found: " + paymentId));
    }

    public List<PaymentResponse> getPaymentsByOrder(Long orderId) {
        return paymentRepository.findByOrderId(orderId).stream()
                .map(PaymentResponse::from)
                .collect(Collectors.toList());
    }

    public List<PaymentResponse> getAllPayments() {
        return paymentRepository.findAll().stream()
                .map(PaymentResponse::from)
                .collect(Collectors.toList());
    }

    @Transactional
    public PaymentResponse refundPayment(String paymentId) {
        Payment payment = paymentRepository.findByPaymentId(paymentId)
                .orElseThrow(() -> new RuntimeException("Payment not found: " + paymentId));
        payment.setStatus(PaymentStatus.REFUNDED);
        Payment updated = paymentRepository.save(payment);
        vectorStoreService.indexPayment(updated);
        log.info("Refunded payment: {}", paymentId);
        return PaymentResponse.from(updated);
    }
}

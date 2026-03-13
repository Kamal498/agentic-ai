package com.example.agentic_ai.repository;

import com.example.agentic_ai.domain.Order;
import com.example.agentic_ai.enums.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    Optional<Order> findByOrderNumber(String orderNumber);

    List<Order> findByCustomerId(String customerId);

    List<Order> findByStatus(OrderStatus status);

    List<Order> findByCustomerNameContainingIgnoreCase(String customerName);

    List<Order> findByOrderDateBetween(LocalDateTime start, LocalDateTime end);

    boolean existsByOrderNumber(String orderNumber);
}

package com.example.agentic_ai.service;

import com.example.agentic_ai.domain.Order;
import com.example.agentic_ai.domain.OrderItem;
import com.example.agentic_ai.dto.OrderRequest;
import com.example.agentic_ai.dto.OrderResponse;
import com.example.agentic_ai.enums.OrderStatus;
import com.example.agentic_ai.repository.OrderRepository;
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
public class OrderService {

    private final OrderRepository orderRepository;
    private final VectorStoreService vectorStoreService;

    @Transactional
    public OrderResponse createOrder(OrderRequest request) {
        Order order = new Order();
        order.setOrderNumber("ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        order.setCustomerId(request.getCustomerId());
        order.setCustomerName(request.getCustomerName());
        order.setCustomerEmail(request.getCustomerEmail());
        order.setDescription(request.getDescription());
        order.setShippingAddress(request.getShippingAddress());
        order.setStatus(OrderStatus.PENDING);

        request.getItems().forEach(itemReq -> {
            OrderItem item = new OrderItem(
                    itemReq.getProductId(),
                    itemReq.getProductName(),
                    itemReq.getQuantity(),
                    itemReq.getUnitPrice());
            order.addItem(item);
        });

        Order saved = orderRepository.save(order);
        log.info("Created order: {}", saved.getOrderNumber());

        vectorStoreService.indexOrder(saved);

        return OrderResponse.from(saved);
    }

    public OrderResponse getOrderById(Long id) {
        return orderRepository.findById(id)
                .map(OrderResponse::from)
                .orElseThrow(() -> new RuntimeException("Order not found: " + id));
    }

    public OrderResponse getOrderByNumber(String orderNumber) {
        return orderRepository.findByOrderNumber(orderNumber)
                .map(OrderResponse::from)
                .orElseThrow(() -> new RuntimeException("Order not found: " + orderNumber));
    }

    public List<OrderResponse> getAllOrders() {
        return orderRepository.findAll().stream()
                .map(OrderResponse::from)
                .collect(Collectors.toList());
    }

    public List<OrderResponse> getOrdersByCustomer(String customerId) {
        return orderRepository.findByCustomerId(customerId).stream()
                .map(OrderResponse::from)
                .collect(Collectors.toList());
    }

    public List<OrderResponse> getOrdersByStatus(OrderStatus status) {
        return orderRepository.findByStatus(status).stream()
                .map(OrderResponse::from)
                .collect(Collectors.toList());
    }

    @Transactional
    public OrderResponse updateOrderStatus(Long id, OrderStatus newStatus) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Order not found: " + id));
        order.setStatus(newStatus);
        Order updated = orderRepository.save(order);
        vectorStoreService.indexOrder(updated);
        log.info("Updated order {} status to {}", updated.getOrderNumber(), newStatus);
        return OrderResponse.from(updated);
    }

    @Transactional
    public void deleteOrder(Long id) {
        orderRepository.deleteById(id);
        log.info("Deleted order: {}", id);
    }
}

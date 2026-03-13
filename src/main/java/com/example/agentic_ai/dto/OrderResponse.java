package com.example.agentic_ai.dto;

import com.example.agentic_ai.domain.Order;
import com.example.agentic_ai.domain.OrderItem;
import com.example.agentic_ai.enums.OrderStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Data
public class OrderResponse {

    private Long id;
    private String orderNumber;
    private String customerId;
    private String customerName;
    private String customerEmail;
    private OrderStatus status;
    private LocalDateTime orderDate;
    private LocalDateTime updatedAt;
    private BigDecimal totalAmount;
    private String description;
    private String shippingAddress;
    private List<OrderItemResponse> items;

    public static OrderResponse from(Order order) {
        OrderResponse resp = new OrderResponse();
        resp.setId(order.getId());
        resp.setOrderNumber(order.getOrderNumber());
        resp.setCustomerId(order.getCustomerId());
        resp.setCustomerName(order.getCustomerName());
        resp.setCustomerEmail(order.getCustomerEmail());
        resp.setStatus(order.getStatus());
        resp.setOrderDate(order.getOrderDate());
        resp.setUpdatedAt(order.getUpdatedAt());
        resp.setTotalAmount(order.getTotalAmount());
        resp.setDescription(order.getDescription());
        resp.setShippingAddress(order.getShippingAddress());
        resp.setItems(order.getItems().stream().map(OrderItemResponse::from).collect(Collectors.toList()));
        return resp;
    }

    @Data
    public static class OrderItemResponse {
        private Long id;
        private String productId;
        private String productName;
        private Integer quantity;
        private BigDecimal unitPrice;
        private BigDecimal totalPrice;

        public static OrderItemResponse from(OrderItem item) {
            OrderItemResponse r = new OrderItemResponse();
            r.setId(item.getId());
            r.setProductId(item.getProductId());
            r.setProductName(item.getProductName());
            r.setQuantity(item.getQuantity());
            r.setUnitPrice(item.getUnitPrice());
            r.setTotalPrice(item.getTotalPrice());
            return r;
        }
    }
}

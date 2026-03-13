package com.example.agentic_ai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class OrderRequest {

    @NotBlank
    private String customerId;

    @NotBlank
    private String customerName;

    @NotBlank
    private String customerEmail;

    private String description;

    private String shippingAddress;

    @NotEmpty
    private List<OrderItemRequest> items;

    @Data
    public static class OrderItemRequest {
        @NotBlank
        private String productId;
        @NotBlank
        private String productName;
        private Integer quantity;
        private java.math.BigDecimal unitPrice;
    }
}

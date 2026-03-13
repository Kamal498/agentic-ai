package com.example.agentic_ai.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class InventoryRequest {
    private String productId;
    private String productName;
    private String category;
    private Integer quantityAvailable;
    private Integer reorderThreshold;
    private BigDecimal unitPrice;
    private String description;
    private String supplier;
}

package com.example.agentic_ai.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "inventory")
@Getter
@Setter
@NoArgsConstructor
public class Inventory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String productId;

    @Column(nullable = false)
    private String productName;

    @Column(nullable = false)
    private String category;

    @Column(nullable = false)
    private Integer quantityAvailable;

    private Integer reorderThreshold;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal unitPrice;

    @Column(length = 1000)
    private String description;

    private String supplier;

    @Column(nullable = false)
    private LocalDateTime lastUpdated;

    @PrePersist
    @PreUpdate
    protected void onSave() {
        lastUpdated = LocalDateTime.now();
    }

    public boolean isLowStock() {
        return reorderThreshold != null && quantityAvailable <= reorderThreshold;
    }

    public String toVectorText() {
        StringBuilder sb = new StringBuilder();
        sb.append("Product ID: ").append(productId).append(". ");
        sb.append("Product Name: ").append(productName).append(". ");
        sb.append("Category: ").append(category).append(". ");
        sb.append("Available Quantity: ").append(quantityAvailable).append(" units. ");
        sb.append("Unit Price: $").append(unitPrice).append(". ");
        sb.append("Supplier: ").append(supplier).append(". ");
        sb.append("Low Stock: ").append(isLowStock() ? "Yes (reorder needed)" : "No").append(". ");
        if (description != null && !description.isBlank()) {
            sb.append("Description: ").append(description).append(". ");
        }
        return sb.toString().trim();
    }
}

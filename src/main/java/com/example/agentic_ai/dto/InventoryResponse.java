package com.example.agentic_ai.dto;

import com.example.agentic_ai.domain.Inventory;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class InventoryResponse {
    private Long id;
    private String productId;
    private String productName;
    private String category;
    private Integer quantityAvailable;
    private Integer reorderThreshold;
    private BigDecimal unitPrice;
    private String description;
    private String supplier;
    private boolean lowStock;
    private LocalDateTime lastUpdated;

    public static InventoryResponse from(Inventory inv) {
        InventoryResponse r = new InventoryResponse();
        r.setId(inv.getId());
        r.setProductId(inv.getProductId());
        r.setProductName(inv.getProductName());
        r.setCategory(inv.getCategory());
        r.setQuantityAvailable(inv.getQuantityAvailable());
        r.setReorderThreshold(inv.getReorderThreshold());
        r.setUnitPrice(inv.getUnitPrice());
        r.setDescription(inv.getDescription());
        r.setSupplier(inv.getSupplier());
        r.setLowStock(inv.isLowStock());
        r.setLastUpdated(inv.getLastUpdated());
        return r;
    }
}

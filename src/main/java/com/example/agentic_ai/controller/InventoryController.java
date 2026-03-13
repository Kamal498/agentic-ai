package com.example.agentic_ai.controller;

import com.example.agentic_ai.dto.InventoryRequest;
import com.example.agentic_ai.dto.InventoryResponse;
import com.example.agentic_ai.service.InventoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;

    @PostMapping
    public ResponseEntity<InventoryResponse> createItem(@RequestBody InventoryRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(inventoryService.createInventoryItem(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<InventoryResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(inventoryService.getById(id));
    }

    @GetMapping
    public ResponseEntity<List<InventoryResponse>> getAllInventory() {
        return ResponseEntity.ok(inventoryService.getAllInventory());
    }

    @GetMapping("/low-stock")
    public ResponseEntity<List<InventoryResponse>> getLowStockItems() {
        return ResponseEntity.ok(inventoryService.getLowStockItems());
    }

    @PatchMapping("/{id}/quantity")
    public ResponseEntity<InventoryResponse> updateQuantity(@PathVariable Long id,
                                                            @RequestParam Integer quantity) {
        return ResponseEntity.ok(inventoryService.updateQuantity(id, quantity));
    }
}

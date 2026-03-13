package com.example.agentic_ai.service;

import com.example.agentic_ai.domain.Inventory;
import com.example.agentic_ai.dto.InventoryRequest;
import com.example.agentic_ai.dto.InventoryResponse;
import com.example.agentic_ai.repository.InventoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class InventoryService {

    private final InventoryRepository inventoryRepository;
    private final VectorStoreService vectorStoreService;

    @Transactional
    public InventoryResponse createInventoryItem(InventoryRequest request) {
        Inventory inventory = new Inventory();
        inventory.setProductId(request.getProductId());
        inventory.setProductName(request.getProductName());
        inventory.setCategory(request.getCategory());
        inventory.setQuantityAvailable(request.getQuantityAvailable());
        inventory.setReorderThreshold(request.getReorderThreshold());
        inventory.setUnitPrice(request.getUnitPrice());
        inventory.setDescription(request.getDescription());
        inventory.setSupplier(request.getSupplier());

        Inventory saved = inventoryRepository.save(inventory);
        vectorStoreService.indexInventory(saved);
        log.info("Created inventory item: {}", saved.getProductId());
        return InventoryResponse.from(saved);
    }

    public InventoryResponse getById(Long id) {
        return inventoryRepository.findById(id)
                .map(InventoryResponse::from)
                .orElseThrow(() -> new RuntimeException("Inventory item not found: " + id));
    }

    public List<InventoryResponse> getAllInventory() {
        return inventoryRepository.findAll().stream()
                .map(InventoryResponse::from)
                .collect(Collectors.toList());
    }

    public List<InventoryResponse> getLowStockItems() {
        return inventoryRepository.findLowStockItems().stream()
                .map(InventoryResponse::from)
                .collect(Collectors.toList());
    }

    @Transactional
    public InventoryResponse updateQuantity(Long id, Integer newQuantity) {
        Inventory inventory = inventoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Inventory item not found: " + id));
        inventory.setQuantityAvailable(newQuantity);
        Inventory updated = inventoryRepository.save(inventory);
        vectorStoreService.indexInventory(updated);
        return InventoryResponse.from(updated);
    }
}

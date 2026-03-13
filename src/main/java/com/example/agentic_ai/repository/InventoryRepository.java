package com.example.agentic_ai.repository;

import com.example.agentic_ai.domain.Inventory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InventoryRepository extends JpaRepository<Inventory, Long> {

    Optional<Inventory> findByProductId(String productId);

    List<Inventory> findByCategory(String category);

    List<Inventory> findByProductNameContainingIgnoreCase(String productName);

    @Query("SELECT i FROM Inventory i WHERE i.quantityAvailable <= i.reorderThreshold")
    List<Inventory> findLowStockItems();

    boolean existsByProductId(String productId);
}

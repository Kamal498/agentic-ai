package com.example.agentic_ai.domain;

import com.example.agentic_ai.enums.OrderStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orders")
@Getter
@Setter
@NoArgsConstructor
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String orderNumber;

    @Column(nullable = false)
    private String customerId;

    @Column(nullable = false)
    private String customerName;

    @Column(nullable = false)
    private String customerEmail;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status;

    @Column(nullable = false)
    private LocalDateTime orderDate;

    private LocalDateTime updatedAt;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmount;

    @Column(length = 1000)
    private String description;

    @Column(length = 500)
    private String shippingAddress;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, fetch = FetchType.EAGER, orphanRemoval = true)
    private List<OrderItem> items = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        if (orderDate == null) orderDate = LocalDateTime.now();
        if (status == null) status = OrderStatus.PENDING;
        recalculateTotal();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
        recalculateTotal();
    }

    public void addItem(OrderItem item) {
        items.add(item);
        item.setOrder(this);
        recalculateTotal();
    }

    public void recalculateTotal() {
        this.totalAmount = items.stream()
                .map(OrderItem::getTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public String toVectorText() {
        StringBuilder sb = new StringBuilder();
        sb.append("Order Number: ").append(orderNumber).append(". ");
        sb.append("Customer: ").append(customerName).append(" (").append(customerId).append("). ");
        sb.append("Status: ").append(status).append(". ");
        sb.append("Order Date: ").append(orderDate).append(". ");
        sb.append("Total Amount: $").append(totalAmount).append(". ");
        sb.append("Shipping Address: ").append(shippingAddress).append(". ");
        if (description != null && !description.isBlank()) {
            sb.append("Description: ").append(description).append(". ");
        }
        sb.append("Items: ");
        items.forEach(item -> sb.append(item.getQuantity()).append("x ")
                .append(item.getProductName()).append(" at $")
                .append(item.getUnitPrice()).append(" each; "));
        return sb.toString().trim();
    }
}

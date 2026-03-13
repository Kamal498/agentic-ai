package com.example.agentic_ai.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "audit_records")
@Getter
@Setter
@NoArgsConstructor
public class AuditRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 2000)
    private String userQuery;

    @Column(nullable = false, length = 5000)
    private String llmResponse;

    @Column(nullable = false)
    private String modelUsed;

    @Column(nullable = false)
    private String requestType;

    private Long responseTimeMs;

    private Integer contextDocumentsUsed;

    @Column(length = 2000)
    private String retrievedContext;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    private String clientIp;

    @PrePersist
    protected void onCreate() {
        timestamp = LocalDateTime.now();
    }

    public AuditRecord(String userQuery, String llmResponse, String modelUsed,
                       String requestType, Long responseTimeMs,
                       Integer contextDocumentsUsed, String retrievedContext) {
        this.userQuery = userQuery;
        this.llmResponse = llmResponse;
        this.modelUsed = modelUsed;
        this.requestType = requestType;
        this.responseTimeMs = responseTimeMs;
        this.contextDocumentsUsed = contextDocumentsUsed;
        this.retrievedContext = retrievedContext;
    }
}

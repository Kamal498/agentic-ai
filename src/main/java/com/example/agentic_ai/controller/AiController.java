package com.example.agentic_ai.controller;

import com.example.agentic_ai.domain.AuditRecord;
import com.example.agentic_ai.dto.ChatResponse;
import com.example.agentic_ai.dto.QueryRequest;
import com.example.agentic_ai.dto.SimilarOrdersResponse;
import com.example.agentic_ai.service.AuditService;
import com.example.agentic_ai.service.RagService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiController {

    private final RagService ragService;
    private final AuditService auditService;

    /**
     * AI Use Case 1: Get Similar Orders (like Twitter's similar tweets feature)
     * Uses cosine similarity on vector embeddings in Milvus to find semantically similar orders.
     */
    @PostMapping("/similar-orders")
    public ResponseEntity<SimilarOrdersResponse> findSimilarOrders(@RequestBody QueryRequest request) {
        int topK = request.getTopK() != null ? request.getTopK() : 5;
        double threshold = request.getSimilarityThreshold() != null ? request.getSimilarityThreshold() : 0.6;
        SimilarOrdersResponse response = ragService.findSimilarOrders(request.getQuery(), topK, threshold);
        return ResponseEntity.ok(response);
    }

    /**
     * AI Use Case 2: Chat Agent with Order, Inventory, Payments context
     * RAG pipeline: embeds query → retrieves context from Milvus → augments prompt → LLM response.
     */
    @PostMapping("/chat")
    public ResponseEntity<ChatResponse> chat(@RequestBody QueryRequest request) {
        ChatResponse response = ragService.chat(request.getQuery());
        return ResponseEntity.ok(response);
    }

    /**
     * Governance: Retrieve audit trail of all AI interactions
     */
    @GetMapping("/audit")
    public ResponseEntity<List<AuditRecord>> getAuditLogs() {
        return ResponseEntity.ok(auditService.getRecentAuditLogs());
    }

    /**
     * Governance: Retrieve audit trail filtered by request type (CHAT or SIMILAR_ORDERS)
     */
    @GetMapping("/audit/{requestType}")
    public ResponseEntity<List<AuditRecord>> getAuditLogsByType(@PathVariable String requestType) {
        return ResponseEntity.ok(auditService.getAuditLogsByType(requestType.toUpperCase()));
    }
}

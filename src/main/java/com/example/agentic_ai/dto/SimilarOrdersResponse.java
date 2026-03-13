package com.example.agentic_ai.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
public class SimilarOrdersResponse {
    private String queryText;
    private Integer totalFound;
    private List<SimilarOrder> similarOrders;
    private LocalDateTime timestamp = LocalDateTime.now();

    @Data
    @NoArgsConstructor
    public static class SimilarOrder {
        private String documentId;
        private String content;
        private Double similarityScore;
        private Map<String, Object> metadata;
    }
}

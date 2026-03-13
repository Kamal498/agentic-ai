package com.example.agentic_ai.dto;

import lombok.Data;

@Data
public class QueryRequest {
    private String query;
    private Integer topK = 5;
    private Double similarityThreshold = 0.6;
}

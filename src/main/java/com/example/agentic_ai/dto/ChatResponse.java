package com.example.agentic_ai.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatResponse {
    private String answer;
    private Long responseTimeMs;
    private String modelUsed;
    private Integer contextDocumentsUsed;
    private List<String> retrievedContextSnippets;
    private LocalDateTime timestamp = LocalDateTime.now();

    public ChatResponse(String answer, Long responseTimeMs) {
        this.answer = answer;
        this.responseTimeMs = responseTimeMs;
        this.modelUsed = "llama3.2:3b";
        this.timestamp = LocalDateTime.now();
    }
}

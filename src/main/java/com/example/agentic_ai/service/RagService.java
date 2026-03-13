package com.example.agentic_ai.service;

import com.example.agentic_ai.dto.ChatResponse;
import com.example.agentic_ai.dto.SimilarOrdersResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RagService {

    private final ChatClient chatClient;
    private final VectorStoreService vectorStoreService;
    private final AuditService auditService;

    @Value("${app.rag.top-k:5}")
    private int defaultTopK;

    @Value("${app.rag.similarity-threshold:0.6}")
    private double defaultThreshold;

    public ChatResponse chat(String userQuery) {
        long startTime = System.currentTimeMillis();
        log.info("[RAG] Processing chat query: {}", userQuery);

        List<Document> contextDocs = vectorStoreService.searchContext(userQuery, defaultTopK, defaultThreshold);
        String context = contextDocs.stream()
                .map(Document::getText)
                .collect(Collectors.joining("\n\n---\n\n"));

        List<String> snippets = contextDocs.stream()
                .map(d -> d.getText().substring(0, Math.min(200, d.getText().length())) + "...")
                .collect(Collectors.toList());

        String augmentedPrompt = buildRagPrompt(userQuery, context);

        String answer = chatClient.prompt()
                .user(augmentedPrompt)
                .call()
                .content();

        long responseTime = System.currentTimeMillis() - startTime;

        auditService.log(userQuery, answer, "llama3.2:3b", "CHAT",
                responseTime, contextDocs.size(), context.substring(0, Math.min(500, context.length())));

        ChatResponse response = new ChatResponse();
        response.setAnswer(answer);
        response.setResponseTimeMs(responseTime);
        response.setModelUsed("llama3.2:3b");
        response.setContextDocumentsUsed(contextDocs.size());
        response.setRetrievedContextSnippets(snippets);

        log.info("[RAG] Chat completed in {}ms, context docs used: {}", responseTime, contextDocs.size());
        return response;
    }

    public SimilarOrdersResponse findSimilarOrders(String queryText, int topK, double threshold) {
        long startTime = System.currentTimeMillis();
        log.info("[RAG] Finding similar orders for: {}", queryText);

        SimilarOrdersResponse response = vectorStoreService.findSimilarOrders(queryText, topK, threshold);

        long responseTime = System.currentTimeMillis() - startTime;
        auditService.log(queryText, "Similar orders: " + response.getTotalFound(), "vector-search",
                "SIMILAR_ORDERS", responseTime, response.getTotalFound(), null);

        log.info("[RAG] Found {} similar orders in {}ms", response.getTotalFound(), responseTime);
        return response;
    }

    private String buildRagPrompt(String userQuery, String context) {
        if (context == null || context.isBlank()) {
            return """
                    You are an expert order management assistant.
                    Answer the following question. If you don't have enough information, say so clearly.
                    
                    Question: %s
                    """.formatted(userQuery);
        }
        return """
                You are an expert order management assistant with access to order, inventory, and payment data.
                Use ONLY the context below to answer the question.
                If the answer is not in the context, say "I don't have enough information to answer that."
                
                === CONTEXT ===
                %s
                === END CONTEXT ===
                
                Question: %s
                
                Answer:
                """.formatted(context, userQuery);
    }
}

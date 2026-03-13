package com.example.agentic_ai.service;

import com.example.agentic_ai.domain.Inventory;
import com.example.agentic_ai.domain.Order;
import com.example.agentic_ai.domain.Payment;
import com.example.agentic_ai.dto.SimilarOrdersResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class VectorStoreService {

    private final VectorStore vectorStore;

    public void indexOrder(Order order) {
        try {
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("document_type", "ORDER");
            metadata.put("order_id", order.getId().toString());
            metadata.put("order_number", order.getOrderNumber());
            metadata.put("customer_id", order.getCustomerId());
            metadata.put("status", order.getStatus().name());

            Document doc = new Document(order.toVectorText(), metadata);
            vectorStore.add(List.of(doc));
            log.info("Indexed order {} in vector store", order.getOrderNumber());
        } catch (Exception e) {
            log.error("Failed to index order {}: {}", order.getOrderNumber(), e.getMessage());
        }
    }

    public void indexInventory(Inventory inventory) {
        try {
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("document_type", "INVENTORY");
            metadata.put("product_id", inventory.getProductId());
            metadata.put("category", inventory.getCategory());

            Document doc = new Document(inventory.toVectorText(), metadata);
            vectorStore.add(List.of(doc));
            log.info("Indexed inventory product {} in vector store", inventory.getProductId());
        } catch (Exception e) {
            log.error("Failed to index inventory {}: {}", inventory.getProductId(), e.getMessage());
        }
    }

    public void indexPayment(Payment payment) {
        try {
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("document_type", "PAYMENT");
            metadata.put("payment_id", payment.getPaymentId());
            metadata.put("order_id", payment.getOrderId().toString());
            metadata.put("status", payment.getStatus().name());

            Document doc = new Document(payment.toVectorText(), metadata);
            vectorStore.add(List.of(doc));
            log.info("Indexed payment {} in vector store", payment.getPaymentId());
        } catch (Exception e) {
            log.error("Failed to index payment {}: {}", payment.getPaymentId(), e.getMessage());
        }
    }

    public SimilarOrdersResponse findSimilarOrders(String queryText, int topK, double threshold) {
        FilterExpressionBuilder b = new FilterExpressionBuilder();

        List<Document> docs = vectorStore.similaritySearch(
                SearchRequest.builder()
                        .query(queryText)
                        .topK(topK)
                        .similarityThreshold(threshold)
                        .filterExpression(b.eq("document_type", "ORDER").build())
                        .build()
        );

        SimilarOrdersResponse response = new SimilarOrdersResponse();
        response.setQueryText(queryText);
        response.setTotalFound(docs.size());
        response.setSimilarOrders(docs.stream().map(doc -> {
            SimilarOrdersResponse.SimilarOrder so = new SimilarOrdersResponse.SimilarOrder();
            so.setDocumentId(doc.getId());
            so.setContent(doc.getText());
            so.setMetadata(doc.getMetadata());
            return so;
        }).collect(Collectors.toList()));

        return response;
    }

    public List<Document> searchContext(String query, int topK, double threshold) {
        return vectorStore.similaritySearch(
                SearchRequest.builder()
                        .query(query)
                        .topK(topK)
                        .similarityThreshold(threshold)
                        .build()
        );
    }
}

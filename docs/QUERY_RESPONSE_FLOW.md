# User Query Response Flow - Spring Boot RAG System

## Overview

The query response system is a Spring Boot-based Retrieval-Augmented Generation (RAG) application that processes user queries by retrieving relevant context from a Milvus vector database and generating intelligent responses using a local Ollama LLM. The system provides two primary AI capabilities: semantic chat and similar order search.

## Architecture Flow

```
User Query → REST Controller → RAG Service → 
Vector Store Search → Context Retrieval → 
LLM Prompt Augmentation → Ollama LLM → Response + Audit Log
```

## System Architecture

### Technology Stack

| Component | Technology | Purpose |
|-----------|-----------|---------|
| **Framework** | Spring Boot 3.x | REST API & dependency injection |
| **AI Integration** | Spring AI | Unified AI abstraction layer |
| **LLM** | Ollama (llama3.2:3b) | Local language model for generation |
| **Embeddings** | Ollama (all-minilm) | Query embedding (384-dim) |
| **Vector DB** | Milvus | Similarity search & retrieval |
| **Database** | H2 (in-memory) | Order/inventory/payment storage |
| **Logging** | SLF4J + Logback | Structured logging |

---

## Configuration Layer (`application.yml`)

### Spring AI - Ollama Configuration

```yaml
spring:
  ai:
    ollama:
      base-url: http://localhost:11434
      chat:
        options:
          model: llama3.2:3b        # 3 billion parameter model
          temperature: 0.7          # Creativity vs. accuracy (0.0-1.0)
          num-ctx: 4096             # Context window size (tokens)
      embedding:
        options:
          model: all-minilm         # 384-dimensional vectors
```

**Key Settings**:
- **Temperature 0.7**: Balanced creativity - not too random, not too deterministic
- **Context Window 4096**: Can fit ~3000 words of context + prompt
- **Local Ollama**: No API keys, no rate limits, complete data privacy

### Milvus Vector Store Configuration

```yaml
spring:
  ai:
    vectorstore:
      milvus:
        client:
          host: localhost
          port: 19530
          username: root
          password: Milvus
        database-name: default
        collection-name: order_embeddings
        embedding-dimension: 384
        index-type: HNSW              # Hierarchical Navigable Small World
        metric-type: COSINE           # Cosine similarity scoring
        initialize-schema: true       # Auto-create collection if missing
```

**Why HNSW + COSINE?**:
- HNSW provides O(log n) search complexity - fast even with millions of vectors
- COSINE metric is scale-invariant - works well for text embeddings

### Application RAG Configuration

```yaml
app:
  rag:
    top-k: 5                          # Retrieve top 5 most similar documents
    similarity-threshold: 0.6         # Minimum similarity score (60%)
    chat-system-prompt: |
      You are an intelligent assistant for an order management system.
      Use ONLY the provided context to answer questions accurately.
      If information is not in the context, clearly state that.
```

**Parameter Tuning**:
- **top-k: 5**: Balance between context richness and noise
- **threshold: 0.6**: Filters out loosely related results
- **System Prompt**: Sets LLM behavior and constraints

---

## Application Components

### 1. Configuration Bean (`AppConfig.java`)

**Purpose**: Initialize Spring AI components for LLM interaction.

#### Bean Definitions

##### `ollamaApi(baseUrl) → OllamaApi`

Creates connection to local Ollama server:

```java
@Bean
public OllamaApi ollamaApi(@Value("${spring.ai.ollama.base-url}") String baseUrl) {
    return OllamaApi.builder()
            .baseUrl(baseUrl)  // http://localhost:11434
            .build();
}
```

##### `chatModel(ollamaApi, model, temperature) → ChatModel`

Configures the LLM with specific parameters:

```java
@Bean
public ChatModel chatModel(OllamaApi ollamaApi,
                            @Value("${spring.ai.ollama.chat.options.model}") String model,
                            @Value("${spring.ai.ollama.chat.options.temperature}") Double temperature) {
    return OllamaChatModel.builder()
            .ollamaApi(ollamaApi)
            .defaultOptions(OllamaChatOptions.builder()
                    .model(model)           // llama3.2:3b
                    .temperature(temperature) // 0.7
                    .build())
            .build();
}
```

**Model Selection**: `llama3.2:3b`
- Small enough to run on consumer hardware
- Fast inference (~50 tokens/second on CPU)
- Good performance on business queries

##### `chatClient(chatModel) → ChatClient`

High-level client with system prompt:

```java
@Bean
public ChatClient chatClient(ChatModel chatModel) {
    return ChatClient.builder(chatModel)
            .defaultSystem("""
                You are an expert assistant for an order management system.
                You have deep knowledge of orders, inventory, and payment processes.
                Provide accurate, concise answers based only on the context provided.
                When referencing order or payment details, be specific.
                If information is not in the provided context, say so clearly.
                """)
            .build();
}
```

**System Prompt Benefits**:
- Reduces hallucinations
- Ensures domain-specific responses
- Sets tone and expertise level

---

### 2. Vector Store Service (`VectorStoreService.java`)

**Purpose**: Interface between application and Milvus vector database.

#### Core Methods

##### `indexOrder(Order order)`

Indexes a single order in real-time:

```java
public void indexOrder(Order order) {
    Map<String, Object> metadata = new HashMap<>();
    metadata.put("document_type", "ORDER");
    metadata.put("order_id", order.getId().toString());
    metadata.put("order_number", order.getOrderNumber());
    metadata.put("customer_id", order.getCustomerId());
    metadata.put("status", order.getStatus().name());

    Document doc = new Document(order.toVectorText(), metadata);
    vectorStore.add(List.of(doc));
    log.info("Indexed order {} in vector store", order.getOrderNumber());
}
```

**Flow**:
1. Extract metadata from order entity
2. Convert order to searchable text via `toVectorText()`
3. _Create Spring AI Document with text + metadata_
4. `vectorStore.add()` automatically:
   - Embeds text using Ollama all-minilm (384-dim)
   - Stores vector + metadata in Milvus
   - Updates HNSW index

**Real-Time Indexing**: New orders are immediately searchable (typically <100ms)

##### `searchContext(query, topK, threshold) → List<Document>`

Generic context retrieval for any query:

```java
public List<Document> searchContext(String query, int topK, double threshold) {
    return vectorStore.similaritySearch(
            SearchRequest.builder()
                    .query(query)
                    .topK(topK)
                    .similarityThreshold(threshold)
                    .build()
    );
}
```

**Search Process**:
```
1. User Query: "Show me pending orders"
   ↓
2. Spring AI embeds query → [0.445, -0.223, 0.667, ...] (384-dim)
   ↓
3. Milvus performs HNSW cosine similarity search
   ↓
4. Returns top-K documents with similarity ≥ threshold
   ↓
5. Example Result:
   - Doc 1 (similarity: 0.89): "Order Number: ORD-123 | Status: PENDING..."
   - Doc 2 (similarity: 0.82): "Order Number: ORD-456 | Status: PENDING..."
   - Doc 3 (similarity: 0.75): "Payment ID: PAY-789 | Status: PENDING..."
```

##### `findSimilarOrders(queryText, topK, threshold) → SimilarOrdersResponse`

Specialized search filtered to ORDER documents only:

```java
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

    // Convert to response DTO...
}
```

**Filter Expression**: `document_type == "ORDER"`
- Searches only order vectors
- Ignores inventory and payment documents
- Faster and more precise

**Use Case**: "Find orders similar to this description" (like Twitter's similar tweets feature)

---

### 3. RAG Service (`RagService.java`)

**Purpose**: Orchestrates the complete RAG pipeline from query to answer.

#### Method 1: `chat(userQuery) → ChatResponse`

**Full RAG Pipeline for Conversational AI**

##### Step-by-Step Execution

**Step 1: Context Retrieval**

```java
List<Document> contextDocs = vectorStoreService.searchContext(
    userQuery,
    defaultTopK,      // 5 documents
    defaultThreshold  // 0.6 similarity
);
```

Example User Query: "What orders did Alice Johnson place last month?"

Retrieved Context Documents:
```
Doc 1 (similarity: 0.91):
"Order Number: ORD-2024-001 | Customer: Alice Johnson (CUST-456) | 
Email: alice@example.com | Status: DELIVERED | Total: $1299.99 | 
Date: 2024-03-01..."

Doc 2 (similarity: 0.85):
"Order Number: ORD-2024-015 | Customer: Alice Johnson (CUST-456) | 
Email: alice@example.com | Status: SHIPPED | Total: $499.99 | 
Date: 2024-03-15..."

Doc 3 (similarity: 0.78):
"Payment ID: PAY-001 | Order ID: 1 | Customer: CUST-456 | 
Amount: $1299.99 | Status: COMPLETED | Date: 2024-03-01..."
```

**Step 2: Context Aggregation**

```java
String context = contextDocs.stream()
        .map(Document::getText)
        .collect(Collectors.joining("\n\n---\n\n"));
```

Result:
```
Order Number: ORD-2024-001 | Customer: Alice Johnson...

---

Order Number: ORD-2024-015 | Customer: Alice Johnson...

---

Payment ID: PAY-001 | Order ID: 1 | Customer: CUST-456...
```

**Step 3: Snippet Extraction** (for response metadata)

```java
List<String> snippets = contextDocs.stream()
        .map(d -> d.getText().substring(0, Math.min(200, d.getText().length())) + "...")
        .collect(Collectors.toList());
```

Used to show user what documents informed the answer.

**Step 4: Prompt Augmentation**

```java
String augmentedPrompt = buildRagPrompt(userQuery, context);
```

Generated Prompt:
```
You are an expert order management assistant with access to order, inventory, and payment data.
Use ONLY the context below to answer the question.
If the answer is not in the context, say "I don't have enough information to answer that."

=== CONTEXT ===
Order Number: ORD-2024-001 | Customer: Alice Johnson (CUST-456) | 
Email: alice@example.com | Status: DELIVERED | Total: $1299.99 | 
Date: 2024-03-01...

---

Order Number: ORD-2024-015 | Customer: Alice Johnson (CUST-456) | 
Email: alice@example.com | Status: SHIPPED | Total: $499.99 | 
Date: 2024-03-15...

---

Payment ID: PAY-001 | Order ID: 1 | Customer: CUST-456 | 
Amount: $1299.99 | Status: COMPLETED | Date: 2024-03-01...
=== END CONTEXT ===

Question: What orders did Alice Johnson place last month?

Answer:
```

**Why This Format?**:
- Clear separation between instructions, context, and question
- Explicit instruction to use ONLY provided context (reduces hallucination)
- Fallback instruction if answer not found

**Step 5: LLM Generation**

```java
String answer = chatClient.prompt()
        .user(augmentedPrompt)
        .call()
        .content();
```

Ollama llama3.2:3b generates response:

```
Based on the provided context, Alice Johnson placed two orders last month (March 2024):

1. **Order ORD-2024-001** (March 1st)
   - Status: Delivered
   - Total: $1,299.99
   - Payment completed

2. **Order ORD-2024-015** (March 15th)
   - Status: Shipped
   - Total: $499.99

Both orders are associated with customer ID CUST-456 and email alice@example.com.
```

**LLM Characteristics**:
- Inference time: ~2-3 seconds on CPU
- Grounded in provided context
- Structured, professional response

**Step 6: Response Construction & Audit**

```java
long responseTime = System.currentTimeMillis() - startTime;

auditService.log(userQuery, answer, "llama3.2:3b", "CHAT",
        responseTime, contextDocs.size(), context.substring(0, Math.min(500, context.length())));

ChatResponse response = new ChatResponse();
response.setAnswer(answer);
response.setResponseTimeMs(responseTime);
response.setModelUsed("llama3.2:3b");
response.setContextDocumentsUsed(contextDocs.size());
response.setRetrievedContextSnippets(snippets);

return response;
```

**Final Response DTO**:
```json
{
  "answer": "Based on the provided context, Alice Johnson placed two orders...",
  "responseTimeMs": 2847,
  "modelUsed": "llama3.2:3b",
  "contextDocumentsUsed": 3,
  "retrievedContextSnippets": [
    "Order Number: ORD-2024-001 | Customer: Alice Johnson...",
    "Order Number: ORD-2024-015 | Customer: Alice Johnson...",
    "Payment ID: PAY-001 | Order ID: 1 | Customer: CUST-456..."
  ]
}
```

**Audit Log Entry**:
```
User Query: "What orders did Alice Johnson place last month?"
Response: "Based on the provided context, Alice Johnson placed two orders..."
Model: llama3.2:3b
Type: CHAT
Response Time: 2847ms
Context Docs: 3
Context Preview: "Order Number: ORD-2024-001 | Customer: Alice Johnson..."
Timestamp: 2024-03-13 10:27:45
```

#### Method 2: `findSimilarOrders(queryText, topK, threshold) → SimilarOrdersResponse`

**Semantic Order Search Without LLM**

```java
public SimilarOrdersResponse findSimilarOrders(String queryText, int topK, double threshold) {
    long startTime = System.currentTimeMillis();
    log.info("[RAG] Finding similar orders for: {}", queryText);

    SimilarOrdersResponse response = vectorStoreService.findSimilarOrders(queryText, topK, threshold);

    long responseTime = System.currentTimeMillis() - startTime;
    auditService.log(queryText, "Similar orders: " + response.getTotalFound(), "vector-search",
            "SIMILAR_ORDERS", responseTime, response.getTotalFound(), null);

    return response;
}
```

**Use Case Example**:

Query: "Laptop order with express shipping to California"

Response:
```json
{
  "queryText": "Laptop order with express shipping to California",
  "totalFound": 3,
  "similarOrders": [
    {
      "documentId": "442584728472",
      "content": "Order Number: ORD-2024-001 | Customer: Alice Johnson | Items: [1x MacBook Pro 14-inch...] | Shipping: 789 Oak Street, San Francisco, CA 94102",
      "metadata": {
        "document_type": "ORDER",
        "order_id": "1",
        "order_number": "ORD-2024-001",
        "status": "DELIVERED"
      },
      "similarity": 0.89
    },
    {
      "documentId": "442584728489",
      "content": "Order Number: ORD-2024-007 | Customer: Bob Smith | Items: [1x Dell XPS 15...] | Shipping: 456 Main St, Los Angeles, CA 90012",
      "metadata": {
        "document_type": "ORDER",
        "order_id": "7",
        "order_number": "ORD-2024-007",
        "status": "SHIPPED"
      },
      "similarity": 0.84
    }
  ]
}
```

**Response Time**: ~50-100ms (no LLM, just vector search)

---

### 4. REST Controller (`AiController.java`)

**Purpose**: Expose AI capabilities via REST endpoints.

#### Endpoint 1: `/api/ai/chat` (POST)

**Chat with RAG Context**

Request:
```json
POST /api/ai/chat
Content-Type: application/json

{
  "query": "What is the status of order ORD-2024-001?"
}
```

Controller Handler:
```java
@PostMapping("/chat")
public ResponseEntity<ChatResponse> chat(@RequestBody QueryRequest request) {
    ChatResponse response = ragService.chat(request.getQuery());
    return ResponseEntity.ok(response);
}
```

Response:
```json
{
  "answer": "Order ORD-2024-001 is currently in DELIVERED status. It was placed by Alice Johnson on March 1st, 2024, with a total amount of $1,299.99. The order included a MacBook Pro 14-inch and USB-C hubs, and was shipped to 789 Oak Street, San Francisco, CA 94102.",
  "responseTimeMs": 2654,
  "modelUsed": "llama3.2:3b",
  "contextDocumentsUsed": 2,
  "retrievedContextSnippets": [
    "Order Number: ORD-2024-001 | Customer: Alice Johnson...",
    "Payment ID: PAY-001 | Order ID: 1..."
  ]
}
```

#### Endpoint 2: `/api/ai/similar-orders` (POST)

**Find Similar Orders by Semantic Search**

Request:
```json
POST /api/ai/similar-orders
Content-Type: application/json

{
  "query": "high value electronics order",
  "topK": 5,
  "similarityThreshold": 0.7
}
```

Controller Handler:
```java
@PostMapping("/similar-orders")
public ResponseEntity<SimilarOrdersResponse> findSimilarOrders(@RequestBody QueryRequest request) {
    int topK = request.getTopK() != null ? request.getTopK() : 5;
    double threshold = request.getSimilarityThreshold() != null ? request.getSimilarityThreshold() : 0.6;
    SimilarOrdersResponse response = ragService.findSimilarOrders(request.getQuery(), topK, threshold);
    return ResponseEntity.ok(response);
}
```

**Parameter Overrides**:
- `topK`: User can request more/fewer results (default: 5)
- `similarityThreshold`: User can adjust precision/recall (default: 0.6)

#### Endpoint 3: `/api/ai/audit` (GET)

**Retrieve Audit Logs**

```java
@GetMapping("/audit")
public ResponseEntity<List<AuditRecord>> getAuditLogs() {
    return ResponseEntity.ok(auditService.getRecentAuditLogs());
}
```

Response:
```json
[
  {
    "id": 1,
    "userQuery": "What orders did Alice Johnson place last month?",
    "aiResponse": "Based on the provided context, Alice Johnson placed two orders...",
    "modelUsed": "llama3.2:3b",
    "requestType": "CHAT",
    "responseTimeMs": 2847,
    "contextDocumentsUsed": 3,
    "retrievedContextPreview": "Order Number: ORD-2024-001...",
    "timestamp": "2024-03-13T10:27:45.123"
  }
]
```

**Governance Benefits**:
- Track all AI interactions
- Monitor response times
- Audit what context was used
- Debug problematic queries

---

## Complete Request Flow Example

### Scenario: User asks about low stock items

**1. User Request**
```bash
curl -X POST http://localhost:8080/api/ai/chat \
  -H "Content-Type: application/json" \
  -d '{"query": "What products are running low on stock?"}'
```

**2. Controller Reception**
```
[2024-03-13 10:30:15] INFO  AiController - Received chat request: "What products are running low on stock?"
```

**3. RAG Service Processing**
```
[2024-03-13 10:30:15] INFO  RagService - [RAG] Processing chat query: What products are running low on stock?
```

**4. Vector Store Search**
```
[2024-03-13 10:30:15] INFO  VectorStoreService - Searching context with topK=5, threshold=0.6
```

**5. Milvus Query Execution**
```
User Query Embedding: [0.234, -0.567, 0.891, ...]
↓
HNSW Cosine Similarity Search
↓
Top 5 Results:
1. Similarity: 0.88 → "Product ID: PRD-003 | Name: USB-C Hub | Stock Status: LOW STOCK | Available: 8 units..."
2. Similarity: 0.85 → "Product ID: PRD-007 | Name: Laptop Stand | Stock Status: LOW STOCK | Available: 5 units..."
3. Similarity: 0.82 → "Product ID: PRD-012 | Name: Wireless Mouse | Stock Status: LOW STOCK | Available: 12 units..."
4. Similarity: 0.75 → "Product ID: PRD-015 | Name: HDMI Cable | Stock Status: In Stock | Available: 45 units..."
5. Similarity: 0.71 → "Product ID: PRD-001 | Name: MacBook Pro | Stock Status: In Stock | Available: 25 units..."
```

**6. Context Aggregation**
```
Combined Context (3 docs, 2 discarded due to "In Stock"):
Product ID: PRD-003 | Name: USB-C Hub | Category: Accessories | Price: $49.99 | Available: 8 units | Stock Status: LOW STOCK | Reorder Threshold: 10

---

Product ID: PRD-007 | Name: Laptop Stand | Category: Accessories | Price: $79.99 | Available: 5 units | Stock Status: LOW STOCK | Reorder Threshold: 15

---

Product ID: PRD-012 | Name: Wireless Mouse | Category: Peripherals | Price: $29.99 | Available: 12 units | Stock Status: LOW STOCK | Reorder Threshold: 20
```

**7. Prompt Construction**
```
System: You are an expert assistant for an order management system...

User: 
You are an expert order management assistant with access to order, inventory, and payment data.
Use ONLY the context below to answer the question.

=== CONTEXT ===
Product ID: PRD-003 | Name: USB-C Hub | Category: Accessories | Price: $49.99 | Available: 8 units | Stock Status: LOW STOCK | Reorder Threshold: 10

---

Product ID: PRD-007 | Name: Laptop Stand | Category: Accessories | Price: $79.99 | Available: 5 units | Stock Status: LOW STOCK | Reorder Threshold: 15

---

Product ID: PRD-012 | Name: Wireless Mouse | Category: Peripherals | Price: $29.99 | Available: 12 units | Stock Status: LOW STOCK | Reorder Threshold: 20
=== END CONTEXT ===

Question: What products are running low on stock?

Answer:
```

**8. Ollama LLM Generation**
```
[2024-03-13 10:30:17] INFO  ChatModel - Calling Ollama API at http://localhost:11434
[2024-03-13 10:30:17] INFO  ChatModel - Model: llama3.2:3b, Temperature: 0.7
[2024-03-13 10:30:19] INFO  ChatModel - Generated response in 2.1s
```

LLM Response:
```
Based on the inventory data, the following products are currently running low on stock:

1. **USB-C Hub** (PRD-003)
   - Current stock: 8 units
   - Reorder threshold: 10 units
   - Status: LOW STOCK
   - Price: $49.99

2. **Laptop Stand** (PRD-007)
   - Current stock: 5 units
   - Reorder threshold: 15 units
   - Status: LOW STOCK
   - Price: $79.99

3. **Wireless Mouse** (PRD-012)
   - Current stock: 12 units
   - Reorder threshold: 20 units
   - Status: LOW STOCK
   - Price: $29.99

All three products are accessories/peripherals and should be considered for reorder to avoid stockouts.
```

**9. Audit Logging**
```
[2024-03-13 10:30:19] INFO  AuditService - Logged interaction: CHAT, response time: 2847ms, context docs: 3
```

**10. Response Return**
```json
HTTP/1.1 200 OK
Content-Type: application/json

{
  "answer": "Based on the inventory data, the following products are currently running low on stock:\n\n1. **USB-C Hub** (PRD-003)...",
  "responseTimeMs": 2847,
  "modelUsed": "llama3.2:3b",
  "contextDocumentsUsed": 3,
  "retrievedContextSnippets": [
    "Product ID: PRD-003 | Name: USB-C Hub | Category: Accessories...",
    "Product ID: PRD-007 | Name: Laptop Stand | Category: Accessories...",
    "Product ID: PRD-012 | Name: Wireless Mouse | Category: Peripherals..."
  ]
}
```

---

## Performance Characteristics

### Typical Response Times

| Operation | Time | Component |
|-----------|------|-----------|
| **Query Embedding** | ~50ms | Ollama all-minilm |
| **Vector Search** | ~20-50ms | Milvus HNSW |
| **Context Retrieval** | ~70-100ms | Total retrieval |
| **LLM Generation** | ~2-3s | Ollama llama3.2:3b |
| **Total Chat Response** | ~2.5-3.5s | End-to-end |
| **Similar Orders** | ~100-150ms | No LLM |

### Bottlenecks

1. **LLM Inference** (2-3s)
   - Mitigation: Use smaller model or GPU acceleration
   - Alternative: Streaming responses

2. **Ollama Cold Start** (5-10s first request)
   - Mitigation: Keep Ollama loaded via health check endpoint

3. **Large Context** (>3000 tokens)
   - Mitigation: Reduce top-K or implement context compression

---

## Error Handling

### Vector Store Errors

```java
@Service
public class VectorStoreService {
    public void indexOrder(Order order) {
        try {
            // Indexing logic...
        } catch (Exception e) {
            log.error("Failed to index order {}: {}", order.getOrderNumber(), e.getMessage());
            // Order saved to DB, but not indexed - graceful degradation
        }
    }
}
```

**Graceful Degradation**: If vector indexing fails, order is still saved in H2 database.

### LLM Generation Errors

```java
try {
    String answer = chatClient.prompt()
            .user(augmentedPrompt)
            .call()
            .content();
} catch (Exception e) {
    log.error("LLM generation failed: {}", e.getMessage());
    return ChatResponse.error("AI service temporarily unavailable");
}
```

**User-Friendly Errors**: API returns 200 OK with error message instead of 500 Internal Server Error.

### Empty Context Handling

```java
private String buildRagPrompt(String userQuery, String context) {
    if (context == null || context.isBlank()) {
        return """
                You are an expert order management assistant.
                Answer the following question. If you don't have enough information, say so clearly.
                
                Question: %s
                """.formatted(userQuery);
    }
    // RAG prompt with context...
}
```

**Fallback Behavior**: If no relevant documents found (threshold too high), LLM answers without context.

---

## Monitoring & Observability

### Logging Levels

```yaml
logging:
  level:
    com.example.agentic_ai: INFO
    org.springframework.ai: DEBUG
```

**Key Log Events**:
- Query received
- Context documents retrieved
- LLM response time
- Audit log saved

### Structured Logs

```
2024-03-13 10:30:15 [INFO ] RagService - [RAG] Processing chat query: What products are running low on stock?
2024-03-13 10:30:15 [DEBUG] VectorStore - Similarity search: topK=5, threshold=0.6, filters={}
2024-03-13 10:30:15 [DEBUG] VectorStore - Found 5 documents, avg similarity: 0.80
2024-03-13 10:30:19 [INFO ] RagService - [RAG] Chat completed in 2847ms, context docs used: 3
2024-03-13 10:30:19 [INFO ] AuditService - Saved audit record #42: CHAT request
```

### Audit Dashboard Queries

```sql
-- Average response time by request type
SELECT request_type, AVG(response_time_ms) as avg_time, COUNT(*) as total_requests
FROM audit_record
GROUP BY request_type;

-- Slow queries (>5 seconds)
SELECT user_query, response_time_ms, timestamp
FROM audit_record
WHERE response_time_ms > 5000
ORDER BY response_time_ms DESC;

-- Most common queries
SELECT user_query, COUNT(*) as frequency
FROM audit_record
GROUP BY user_query
ORDER BY frequency DESC
LIMIT 10;
```

---

## Advanced Features

### 1. Metadata Filtering

Filter search by order status:

```java
FilterExpressionBuilder b = new FilterExpressionBuilder();
List<Document> docs = vectorStore.similaritySearch(
        SearchRequest.builder()
                .query("laptop orders")
                .topK(5)
                .filterExpression(
                    b.and(
                        b.eq("document_type", "ORDER"),
                        b.eq("status", "PENDING")
                    ).build()
                )
                .build()
);
```

Result: Only returns pending laptop orders.

### 2. Hybrid Search (Coming Soon)

Combine semantic + keyword search:

```java
// Semantic: "laptop computer"
// Keyword: exact match on "ORD-2024-001"
// Merge results with weighted scoring
```

### 3. Context Reranking (Coming Soon)

```java
// After retrieval, rerank by:
// 1. Query-document relevance (semantic)
// 2. Recency (newer orders first)
// 3. Business importance (high-value customers)
```

---

## Configuration Best Practices

### Development

```yaml
app:
  rag:
    top-k: 10              # More context for debugging
    similarity-threshold: 0.5  # Lower threshold to see more results

logging:
  level:
    com.example.agentic_ai: DEBUG
    org.springframework.ai: DEBUG
```

### Production

```yaml
app:
  rag:
    top-k: 5               # Balance context vs. noise
    similarity-threshold: 0.65  # Higher precision

logging:
  level:
    com.example.agentic_ai: INFO
    org.springframework.ai: WARN
```

### High-Traffic Scenarios

- Use GPU for Ollama (10x faster inference)
- Implement response caching (Redis)
- Add rate limiting per user/IP
- Deploy multiple Ollama instances behind load balancer

---

## Testing the System

### 1. Health Check

```bash
curl http://localhost:8080/actuator/health
```

### 2. Simple Chat Query

```bash
curl -X POST http://localhost:8080/api/ai/chat \
  -H "Content-Type: application/json" \
  -d '{"query": "How many orders are in the system?"}'
```

### 3. Similar Orders Search

```bash
curl -X POST http://localhost:8080/api/ai/similar-orders \
  -H "Content-Type: application/json" \
  -d '{"query": "laptop order", "topK": 3, "similarityThreshold": 0.7}'
```

### 4. View Audit Logs

```bash
curl http://localhost:8080/api/ai/audit
```

---

## Summary

The Spring Boot query response system provides intelligent, context-aware answers by combining:

1. **Semantic Search**: Milvus HNSW for fast, accurate retrieval
2. **RAG Pattern**: Grounded LLM responses using retrieved context
3. **Local LLM**: Ollama for privacy and cost-free operation
4. **Audit Trail**: Complete governance and debugging capability

The system handles 2-3 second response times with consumer hardware and can scale to millions of documents with proper infrastructure. All AI interactions are logged, monitored, and traceable for compliance and improvement.

# Order Management RAG Service

An AI-powered Order Management Service combining **Spring Boot**, **Spring AI**, **Milvus vector DB**, and **Llama 3.2 3B** (via Ollama) to enable semantic search over orders, inventory, and payments.

---

## Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                        Client / API Consumer                     │
└──────────────────────────────┬──────────────────────────────────┘
                               │ REST
┌──────────────────────────────▼──────────────────────────────────┐
│                    Spring Boot 3.x Application                   │
│  ┌─────────────┐  ┌──────────────┐  ┌────────────────────────┐  │
│  │OrderController│ │InventoryCtrl│  │    AiController        │  │
│  │PaymentCtrl   │ └──────────────┘  │  POST /api/ai/chat     │  │
│  └──────┬───────┘                   │  POST /api/ai/similar- │  │
│         │ JPA                       │         orders         │  │
│  ┌──────▼──────┐  ┌──────────────┐  └──────────┬─────────────┘  │
│  │  H2 Database│  │  RagService  │◄─────────────┘               │
│  └─────────────┘  └──────┬───────┘                              │
│                          │                                       │
│                   ┌──────▼────────┐   ┌──────────────────────┐  │
│                   │VectorStoreService│  │     AuditService     │  │
│                   └──────┬────────┘   └──────────────────────┘  │
└──────────────────────────┼──────────────────────────────────────┘
                           │ Spring AI VectorStore
          ┌────────────────▼──────────────────┐
          │         Milvus (HNSW/Cosine)       │
          │   Collection: order_management      │
          │   Dimension: 384                    │
          └────────────────────────────────────┘
                           ▲
          ┌────────────────┴──────────────────┐
          │       Python RAG Pipeline          │
          │  extractor → chunker → embedder   │
          │  (all-MiniLM-L6-v2) → milvus_loader│
          │  Observability: LangSmith          │
          └────────────────────────────────────┘

          LLM: Ollama (llama3.2:3b) on localhost:11434
```

---

## Technology Stack

| Component | Technology |
|---|---|
| Backend Framework | Spring Boot 3.5.x + Spring AI 1.0.0 |
| Relational DB | H2 (in-memory, dev) |
| Vector DB | Milvus 2.4 (HNSW, cosine similarity) |
| LLM | Llama 3.2 3B via Ollama |
| Embeddings | `all-MiniLM-L6-v2` (dim=384) |
| Data Pipeline | Python + LangChain |
| Chunking | `RecursiveCharacterTextSplitter` (1000 chars, 20% overlap) |
| Retrieval | ANN search with cosine similarity |
| Observability | LangSmith + SLF4J structured logs |
| Governance | Audit trail (`AuditRecord` entity) |

---

## Cosine Similarity (retrieval method)

```
cosine_similarity(A, B) = (A · B) / (||A|| × ||B||)

                Σ(Aᵢ × Bᵢ)
cos(θ) =   ─────────────────────
            √(Σ Aᵢ²) × √(Σ Bᵢ²)
```

Milvus uses an **HNSW** (Hierarchical Navigable Small World) index to perform approximate nearest-neighbour (ANN) search with cosine metric — no manual implementation needed.

---

## Project Structure

```
agentic-ai/
├── src/main/java/com/example/agentic_ai/
│   ├── config/          AppConfig.java (ChatClient bean, @EnableAsync)
│   ├── controller/      OrderController, InventoryController,
│   │                    PaymentController, AiController
│   ├── domain/          Order, OrderItem, Inventory, Payment, AuditRecord
│   ├── dto/             Request/Response DTOs + ChatResponse, SimilarOrdersResponse
│   ├── enums/           OrderStatus, PaymentStatus
│   ├── repository/      JPA repositories for all entities
│   └── service/         OrderService, InventoryService, PaymentService,
│                        VectorStoreService, RagService, AuditService
├── src/main/resources/
│   ├── application.yml  H2 + Ollama + Milvus config
│   └── data.sql         Seed data (10 orders, 10 inventory, 8 payments)
├── rag-pipeline/        Python RAG pipeline
│   ├── config.py        Env vars + LangSmith setup
│   ├── extractor.py     Fetch data from Spring API, serialize to text
│   ├── chunker.py       RecursiveCharacterTextSplitter
│   ├── milvus_loader.py Embed + ingest into Milvus
│   ├── main.py          Pipeline orchestrator
│   ├── requirements.txt Python dependencies
│   └── .env.example     Environment variable template
├── docker-compose.yml   etcd + MinIO + Milvus + Ollama
└── pom.xml
```

---

## Prerequisites

- Java 21+
- Docker + Docker Compose
- Python 3.10+
- Maven 3.9+

---

## Corporate Network / Offline Setup

If your `~/.m2/settings.xml` routes all Maven requests through a corporate Artifactory that does not proxy Maven Central (e.g. `mirrorOf=*`), Spring AI artifacts will fail to resolve. Run the bootstrap script **once** from any network that can reach Maven Central (personal hotspot, VPN exit, CI runner with internet access):

```bash
chmod +x scripts/bootstrap-deps.sh
./scripts/bootstrap-deps.sh
```

This downloads `spring-ai-*:1.0.0` JARs directly from `repo.maven.apache.org` and installs them into `~/.m2/repository`. After that, all subsequent `./mvnw` calls are served from the local cache and work on the corporate network without issue.

Alternatively, ask your Artifactory admin to add `https://repo.maven.apache.org/maven2` and `https://repo.spring.io/release` as upstream virtual repos under your mirror.

---

## Setup & Run

### 1. Start infrastructure

```bash
docker-compose up -d
```

Wait for Milvus to be healthy (~30s), then pull the required Ollama models:

```bash
# Pull LLM
docker exec ollama ollama pull llama3.2:3b

# Pull embedding model (used by Spring AI's Milvus store)
docker exec ollama ollama pull all-minilm
```

### 2. Start the Spring Boot application

```bash
./mvnw spring-boot:run
```

The app starts on `http://localhost:8080`. H2 console is at `http://localhost:8080/h2-console` (JDBC URL: `jdbc:h2:mem:orderdb`).

### 3. Run the Python RAG pipeline

```bash
cd rag-pipeline
cp .env.example .env          # edit if needed
pip install -r requirements.txt
python main.py
```

The pipeline:
1. Fetches all orders, inventory, payments from the Spring API
2. Serializes each record to rich text
3. Splits with `RecursiveCharacterTextSplitter` (1000 chars, 200 overlap)
4. Embeds with `all-MiniLM-L6-v2` (384-dim, cosine-normalized)
5. Loads into Milvus with HNSW index

---

## API Reference

### Orders

| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/api/orders` | Create order |
| `GET` | `/api/orders` | List all orders |
| `GET` | `/api/orders/{id}` | Get by ID |
| `GET` | `/api/orders/number/{orderNumber}` | Get by order number |
| `GET` | `/api/orders/customer/{customerId}` | Orders by customer |
| `GET` | `/api/orders/status/{status}` | Orders by status |
| `PATCH` | `/api/orders/{id}/status?status=SHIPPED` | Update status |
| `DELETE` | `/api/orders/{id}` | Delete order |

### Inventory

| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/api/inventory` | Create item |
| `GET` | `/api/inventory` | List all |
| `GET` | `/api/inventory/{id}` | Get by ID |
| `GET` | `/api/inventory/low-stock` | Low stock items |
| `PATCH` | `/api/inventory/{id}/quantity?quantity=50` | Update quantity |

### Payments

| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/api/payments` | Create payment |
| `GET` | `/api/payments` | List all |
| `GET` | `/api/payments/{paymentId}` | Get by payment ID |
| `GET` | `/api/payments/order/{orderId}` | Payments for order |
| `POST` | `/api/payments/{paymentId}/refund` | Refund payment |

### AI Endpoints

| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/api/ai/chat` | RAG chat with Order/Inventory/Payment context |
| `POST` | `/api/ai/similar-orders` | Find semantically similar orders |
| `GET` | `/api/ai/audit` | Recent AI interaction audit log |
| `GET` | `/api/ai/audit/{type}` | Audit log by type (CHAT / SIMILAR_ORDERS) |

#### Chat request example

```json
POST /api/ai/chat
{
  "query": "What is the status of orders for customer CUST-001?",
  "topK": 5,
  "similarityThreshold": 0.6
}
```

#### Similar orders request example

```json
POST /api/ai/similar-orders
{
  "query": "MacBook laptop purchase for development team",
  "topK": 3,
  "similarityThreshold": 0.7
}
```

---

## AI Use Cases

### Use Case 1 — Similar Orders (like Twitter's "similar tweets")

Embeds the user's free-text query with `all-MiniLM-L6-v2`, then performs ANN cosine similarity search in Milvus filtered to `document_type = ORDER`. Returns the top-K most semantically similar orders.

### Use Case 2 — Chat Agent with full context

Full RAG pipeline:
1. Embed query
2. Retrieve top-K documents across orders, inventory, and payments
3. Inject retrieved context into the prompt
4. Send augmented prompt to Llama 3.2 3B via Ollama
5. Return grounded answer + metadata (response time, context snippets used)

---

## Observability & Governance

- **LangSmith** — set `LANGSMITH_API_KEY` in `rag-pipeline/.env` to enable LangChain tracing
- **Structured logging** — all service methods log at INFO with context (order number, response time, context docs used)
- **Audit trail** — every AI request is persisted asynchronously to the `audit_records` H2 table, recording: query, response, model, request type, response time, context doc count

---

## Milvus Ports

| Port | Service |
|---|---|
| `19530` | Milvus gRPC (Spring AI client) |
| `9091` | Milvus HTTP (metrics/health) |
| `9000` | MinIO S3 API |
| `9001` | MinIO Console |
| `11434` | Ollama API |
# Data Preparation Pipeline - Python RAG System

## Overview

The data preparation pipeline is a Python-based ETL (Extract, Transform, Load) system that prepares order management data 
for Retrieval-Augmented Generation (RAG). It extracts data from a Spring Boot API, processes it into searchable chunks, 
generates vector embeddings, and loads them into a Milvus vector database.

## Architecture Flow

```
Spring Boot API → Data Extraction → Text Transformation → 
Chunking → Embedding Generation → Milvus Vector Store
```

## Pipeline Components

### 1. Configuration (`config.py`)

**Purpose**: Centralized configuration management for the entire pipeline.

**Key Configurations**:

```python
# API Connection
SPRING_API_BASE = "http://localhost:8080"

# Milvus Vector Database
MILVUS_HOST = "localhost"
MILVUS_PORT = 19530
MILVUS_COLLECTION = "order_management"

# Embedding Model
EMBEDDING_MODEL = "sentence-transformers/all-MiniLM-L6-v2"
EMBEDDING_DIMENSION = 384

# Text Chunking
CHUNK_SIZE = 1000 characters
CHUNK_OVERLAP = 200 characters (20%)

# LangSmith Tracing (Optional)
LANGSMITH_API_KEY = ""
LANGSMITH_PROJECT = "order-management-rag"
```

**Environment Variables**: All configurations can be overridden via `.env` file for different environments.

---

### 2. Data Extraction (`extractor.py`)

**Purpose**: Fetch raw data from Spring Boot REST API endpoints and convert to structured text.

#### Extraction Functions

##### `fetch_orders()` → List[Dict]
- **Endpoint**: `GET /api/orders`
- **Returns**: List of order objects with items, customer info, shipping details
- **Error Handling**: Raises exception on HTTP errors, 10s timeout

##### `fetch_inventory()` → List[Dict]
- **Endpoint**: `GET /api/inventory`
- **Returns**: List of inventory items with stock levels, pricing, supplier info
- **Error Handling**: Same as above

##### `fetch_payments()` → List[Dict]
- **Endpoint**: `GET /api/payments`
- **Returns**: List of payment transactions with order associations
- **Error Handling**: Same as above

#### Text Transformation Functions

##### `order_to_text(order: Dict) → str`

Converts order JSON to human-readable text format optimized for semantic search:

**Input Example**:
```json
{
  "orderNumber": "ORD-001",
  "customerName": "John Doe",
  "customerId": "CUST-123",
  "customerEmail": "john@example.com",
  "status": "SHIPPED",
  "totalAmount": 299.99,
  "items": [
    {"quantity": 2, "productName": "Widget A", "unitPrice": 149.99}
  ],
  "shippingAddress": "123 Main St",
  "orderDate": "2024-03-10"
}
```

**Output Text**:
```
Order Number: ORD-001 | Customer: John Doe (CUST-123) | Email: john@example.com | 
Status: SHIPPED | Total: $299.99 | Description: N/A | Shipping: 123 Main St | 
Items: [2x Widget A @ $149.99] | Date: 2024-03-10
```

**Why This Format?**:
- Pipe-delimited for clear field separation
- Human-readable for LLM understanding
- All key information in single line for chunk coherence
- Optimized for semantic similarity matching

##### `inventory_to_text(item: Dict) → str`

**Output Format**:
```
Product ID: PRD-123 | Name: Widget A | Category: Electronics | Price: $149.99 | 
Available: 50 units | Stock Status: In Stock | Reorder Threshold: 10 | 
Supplier: ACME Corp | Description: High-quality widget
```

##### `payment_to_text(payment: Dict) → str`

**Output Format**:
```
Payment ID: PAY-456 | Order ID: 1 | Customer: CUST-123 | Amount: $299.99 | 
Method: CREDIT_CARD | Status: COMPLETED | Transaction: TXN-789 | 
Date: 2024-03-10 | Notes: Authorized successfully
```

---

### 3. Text Chunking (`chunker.py`)

**Purpose**: Split long documents into optimal-sized chunks for vector embedding while preserving context.

#### Chunking Strategy

**Algorithm**: `RecursiveCharacterTextSplitter` from LangChain

**Parameters**:
- **chunk_size**: 1000 characters
- **chunk_overlap**: 200 characters (20%)
- **separators**: `["\n\n", "\n", " | ", ". ", " ", ""]`

**How It Works**:

1. **Recursive Splitting**: Tries to split on paragraph breaks (`\n\n`) first, then line breaks (`\n`), then pipe delimiters (` | `), then sentences (`. `), then words (` `), and finally characters if needed.

2. **Overlap Strategy**: Each chunk includes 200 characters from the previous chunk to maintain context across boundaries.

3. **Metadata Preservation**: Each chunk retains original document metadata plus:
   - `chunk_index`: Position in document (0, 1, 2...)
   - `total_chunks`: Total number of chunks from parent document

**Example**:

```
Original Document (2500 chars):
"Order Number: ORD-001 | Customer: John Doe... [very long description]"

↓ Chunking Process ↓

Chunk 0 (1000 chars):
"Order Number: ORD-001 | Customer: John Doe... [first part]"
metadata: {document_type: "ORDER", chunk_index: 0, total_chunks: 3}

Chunk 1 (1000 chars - includes 200 char overlap from Chunk 0):
"[last 200 chars of Chunk 0]... [middle part]"
metadata: {document_type: "ORDER", chunk_index: 1, total_chunks: 3}

Chunk 2 (500 chars - includes 200 char overlap from Chunk 1):
"[last 200 chars of Chunk 1]... [final part]"
metadata: {document_type: "ORDER", chunk_index: 2, total_chunks: 3}
```

**Why This Approach?**:
- Prevents splitting mid-sentence or mid-field
- Maintains semantic coherence
- Overlap ensures no context loss at boundaries
- Optimized for 384-dim embedding model capacity

---

### 4. Vector Embedding & Storage (`milvus_loader.py`)

**Purpose**: Generate vector embeddings and store them in Milvus with HNSW indexing.

#### Embedding Generation

##### `get_embedder() → HuggingFaceEmbeddings`

**Model**: `sentence-transformers/all-MiniLM-L6-v2`

**Specifications**:
- **Dimension**: 384 floating-point numbers
- **Normalization**: Enabled (for cosine similarity)
- **Device**: CPU (configurable)
- **Speed**: ~1000 texts/second on modern CPU

**Why This Model?**:
- Fast inference time
- Good balance of accuracy vs. speed
- Trained on diverse text (handles orders, products, payments well)
- Consistent with Spring Boot's Ollama all-minilm embedding model

##### Vector Generation Process:

```python
texts = ["Order Number: ORD-001...", "Product ID: PRD-123..."]
embeddings = embedder.embed_documents(texts)
# Result: [[0.12, -0.45, 0.89, ...], [0.34, 0.21, -0.67, ...]]
# Each inner list has 384 float values
```

#### Milvus Connection & Collection Setup

##### `connect_milvus()`
- Connects to Milvus at `localhost:19530`
- Uses default credentials
- Single connection for entire pipeline

##### `ensure_collection() → Collection`

**Creates/Validates Collection Schema**:

```python
Fields:
1. id (INT64, primary, auto-generated)
2. content (VARCHAR, max 4096 chars) - The actual text
3. document_type (VARCHAR, max 64 chars) - "ORDER", "INVENTORY", "PAYMENT"
4. source_id (VARCHAR, max 128 chars) - order_id, product_id, or payment_id
5. embedding (FLOAT_VECTOR, dim=384) - The 384-dimensional vector
```

**Index Configuration**:

```python
Type: HNSW (Hierarchical Navigable Small World)
Metric: COSINE similarity
Parameters:
  - M: 16 (connections per node)
  - efConstruction: 200 (search quality during index build)
```

**Why HNSW + COSINE?**:
- **HNSW**: Graph-based algorithm providing O(log n) search time
- **COSINE**: Measures angle between vectors, robust to text length variations
- **Trade-off**: Slightly slower indexing, much faster queries (perfect for RAG)

#### Document Loading

##### `load_documents(docs: List[Document], batch_size=100) → int`

**Batch Processing Flow**:

```
Step 1: Take batch of 100 documents
Step 2: Extract text from each document
Step 3: Generate 100 embeddings (parallel)
Step 4: Prepare insertion data:
   - texts: ["Order Number...", "Product ID..."]
   - document_types: ["ORDER", "INVENTORY"]
   - source_ids: ["1", "PRD-123"]
   - vectors: [[0.12, -0.45, ...], [0.34, 0.21, ...]]
Step 5: Insert batch into Milvus
Step 6: Repeat until all documents processed
Step 7: Flush collection to persist data
```

**Performance**:
- Batch size of 100 balances memory vs. speed
- Typical throughput: 500-1000 documents/minute
- Embedding generation is the bottleneck

---

### 5. Pipeline Orchestration (`main.py`)

**Purpose**: Coordinate all pipeline stages in correct order.

#### `build_raw_docs() → List[dict]`

**Workflow**:

```
1. Fetch Orders from API
   ↓
   Convert each to text → Add metadata
   Result: {text: "...", metadata: {document_type: "ORDER", ...}}

2. Fetch Inventory from API
   ↓
   Convert each to text → Add metadata
   Result: {text: "...", metadata: {document_type: "INVENTORY", ...}}

3. Fetch Payments from API
   ↓
   Convert each to text → Add metadata
   Result: {text: "...", metadata: {document_type: "PAYMENT", ...}}

4. Combine all into single list
   Return: [order1, order2, ..., inventory1, ..., payment1, ...]
```

#### `run_pipeline()`

**Complete Pipeline Execution**:

```
═══════════════════════════════════════════════════════
Starting Order Management RAG Pipeline
LangSmith tracing: ENABLED/DISABLED
═══════════════════════════════════════════════════════

Step 1/4: Extracting data from Spring Boot API...
  ├─ Fetched 15 orders from API
  ├─ Fetched 8 inventory items from API
  ├─ Fetched 12 payments from API
  └─ Total raw documents extracted: 35

Step 2/4: Chunking documents (RecursiveCharacterTextSplitter)...
  └─ Chunked 35 documents into 42 chunks
     (chunk_size=1000, overlap=200)

Step 3/4: Connecting to Milvus...
  └─ Connected to Milvus at localhost:19530

Step 4/4: Embedding with all-MiniLM-L6-v2 and loading into Milvus...
  ├─ Loading embedding model: sentence-transformers/all-MiniLM-L6-v2
  ├─ Embedding model loaded (dim=384, cosine-normalized)
  ├─ Created Milvus collection 'order_management' with HNSW/COSINE index
  ├─ Inserted batch 1: 42 vectors (42/42 total)
  └─ Load complete: 42 documents indexed in Milvus

═══════════════════════════════════════════════════════
Pipeline complete in 12.4s
  Raw documents:  35
  Chunks indexed: 42
  Collection:     order_management
  Index:          HNSW / Cosine similarity
═══════════════════════════════════════════════════════
```

---

## Data Flow Example: End-to-End

### Input: Order from Spring Boot API

```json
{
  "id": 1,
  "orderNumber": "ORD-2024-001",
  "customerId": "CUST-456",
  "customerName": "Alice Johnson",
  "customerEmail": "alice@example.com",
  "status": "DELIVERED",
  "totalAmount": 1299.99,
  "orderDate": "2024-03-01T10:30:00",
  "shippingAddress": "789 Oak Street, San Francisco, CA 94102",
  "description": "Laptop and accessories for remote work setup",
  "items": [
    {
      "productName": "MacBook Pro 14-inch",
      "quantity": 1,
      "unitPrice": 1199.99
    },
    {
      "productName": "USB-C Hub",
      "quantity": 2,
      "unitPrice": 49.99
    }
  ]
}
```

### Transformation Steps

**1. Text Conversion** (`order_to_text()`):
```
Order Number: ORD-2024-001 | Customer: Alice Johnson (CUST-456) | 
Email: alice@example.com | Status: DELIVERED | Total: $1299.99 | 
Description: Laptop and accessories for remote work setup | 
Shipping: 789 Oak Street, San Francisco, CA 94102 | 
Items: [1x MacBook Pro 14-inch @ $1199.99, 2x USB-C Hub @ $49.99] | 
Date: 2024-03-01T10:30:00
```

**2. Raw Document Structure**:
```python
{
  "text": "Order Number: ORD-2024-001 | Customer: Alice Johnson...",
  "metadata": {
    "document_type": "ORDER",
    "order_id": "1",
    "order_number": "ORD-2024-001",
    "customer_id": "CUST-456",
    "status": "DELIVERED"
  }
}
```

**3. Chunking** (this order fits in one chunk):
```python
Document(
  page_content="Order Number: ORD-2024-001 | Customer: Alice Johnson...",
  metadata={
    "document_type": "ORDER",
    "order_id": "1",
    "order_number": "ORD-2024-001",
    "customer_id": "CUST-456",
    "status": "DELIVERED",
    "chunk_index": 0,
    "total_chunks": 1
  }
)
```

**4. Embedding Generation**:
```python
text = "Order Number: ORD-2024-001 | Customer: Alice Johnson..."
embedding = [0.234, -0.567, 0.891, ..., 0.123]  # 384 dimensions
```

**5. Milvus Storage**:
```python
{
  "id": auto-generated (e.g., 442584728472),
  "content": "Order Number: ORD-2024-001 | Customer: Alice Johnson...",
  "document_type": "ORDER",
  "source_id": "1",
  "embedding": [0.234, -0.567, 0.891, ..., 0.123]
}
```

### Query Time: How This Data Is Retrieved

User Query: "Show me laptop orders from San Francisco"

```
1. Query → Embedding: [0.445, -0.223, 0.667, ...]

2. Milvus HNSW Search:
   - Compares query vector with all stored vectors using COSINE similarity
   - Ranks by similarity score (0.0 to 1.0)
   - Returns top-K results

3. Result:
   Rank 1 (similarity: 0.89): "Order Number: ORD-2024-001 | Customer: Alice Johnson..."
   → Contains "MacBook Pro" (laptop) and "San Francisco"
```

---

## Performance Characteristics

### Typical Pipeline Metrics

| Metric | Value | Notes |
|--------|-------|-------|
| **API Extraction** | ~1s | Network latency dependent |
| **Text Transformation** | <100ms | CPU-bound, very fast |
| **Chunking** | ~200ms | For 100 documents |
| **Embedding Generation** | ~8-10s | Bottleneck, scales with doc count |
| **Milvus Upload** | ~2s | Batch inserts are fast |
| **Total Pipeline Time** | 10-15s | For ~100 documents |

### Scalability

- **Small Dataset** (< 1000 docs): Runs in seconds
- **Medium Dataset** (1000-10,000 docs): Minutes
- **Large Dataset** (> 10,000 docs): Consider GPU acceleration for embeddings

---

## Error Handling

### API Extraction Errors
- **Timeout**: 10-second timeout per endpoint
- **HTTP Errors**: Raises exception with status code
- **Retry Strategy**: No automatic retry (should be added for production)

### Embedding Errors
- **Model Loading Failure**: Pipeline exits with error
- **Memory Issues**: Reduce batch size in `load_documents()`

### Milvus Connection Errors
- **Connection Failure**: Check Milvus is running on port 19530
- **Collection Already Exists**: Reuses existing collection
- **Schema Mismatch**: Manual collection drop required

---

## Configuration Best Practices

### Development Environment
```env
SPRING_API_BASE=http://localhost:8080
MILVUS_HOST=localhost
CHUNK_SIZE=1000
LANGCHAIN_TRACING_V2=true  # Enable for debugging
```

### Production Environment
```env
SPRING_API_BASE=https://api.production.com
MILVUS_HOST=milvus-prod.internal
CHUNK_SIZE=800  # Smaller chunks for better precision
LANGCHAIN_TRACING_V2=false  # Disable for performance
```

---

## Running the Pipeline

### Prerequisites
```bash
# Install dependencies
pip install -r requirements.txt

# Start Milvus (via Docker)
docker-compose up -d milvus-standalone

# Start Spring Boot API
cd ../
mvn spring-boot:run
```

### Execute Pipeline
```bash
cd rag-pipeline
python main.py
```

### Verify Success
```bash
# Check Milvus collection
python -c "
from pymilvus import connections, Collection
connections.connect(host='localhost', port=19530)
col = Collection('order_management')
print(f'Total vectors: {col.num_entities}')
"
```

---

## Monitoring & Observability

### LangSmith Tracing (Optional)

When enabled, tracks:
- API call latency
- Embedding generation time
- Document processing pipeline
- Error traces

**Setup**:
```env
LANGCHAIN_TRACING_V2=true
LANGCHAIN_API_KEY=your_key_here
LANGCHAIN_PROJECT=order-management-rag
```

### Logs

All operations logged with timestamps:
```
2024-03-13 10:27:15 [INFO] extractor - Fetched 15 orders from API
2024-03-13 10:27:16 [INFO] chunker - Chunked 35 documents into 42 chunks
2024-03-13 10:27:25 [INFO] milvus_loader - Load complete: 42 documents indexed
```

---

## Summary

The Python data preparation pipeline transforms raw order management data into semantic vectors optimized for AI-powered search and retrieval. 
It uses industry-standard tools (LangChain, Sentence Transformers, Milvus) and follows best practices for chunking, embedding, and indexing. 
The result is a vector database ready to power intelligent order queries and chat agents.

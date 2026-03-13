"""
RAG Pipeline Entry Point
Extracts data from the Spring Boot API, chunks it, embeds with all-MiniLM-L6-v2,
and loads vectors into Milvus with HNSW/cosine index.
LangSmith tracing is enabled when LANGSMITH_API_KEY is set in .env.
"""
import logging
import sys
import time
from typing import List

import config  # sets LangSmith env vars on import
from extractor import (
    fetch_orders, fetch_inventory, fetch_payments,
    order_to_text, inventory_to_text, payment_to_text,
)
from chunker import chunk_documents
from milvus_loader import connect_milvus, load_documents
from langchain.schema import Document

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(name)s - %(message)s",
    handlers=[logging.StreamHandler(sys.stdout)],
)
logger = logging.getLogger(__name__)


def build_raw_docs() -> List[dict]:
    raw = []

    logger.info("--- Extracting Orders ---")
    for order in fetch_orders():
        raw.append({
            "text": order_to_text(order),
            "metadata": {
                "document_type": "ORDER",
                "order_id": str(order.get("id", "")),
                "order_number": order.get("orderNumber", ""),
                "customer_id": order.get("customerId", ""),
                "status": order.get("status", ""),
            },
        })

    logger.info("--- Extracting Inventory ---")
    for item in fetch_inventory():
        raw.append({
            "text": inventory_to_text(item),
            "metadata": {
                "document_type": "INVENTORY",
                "product_id": item.get("productId", ""),
                "category": item.get("category", ""),
            },
        })

    logger.info("--- Extracting Payments ---")
    for payment in fetch_payments():
        raw.append({
            "text": payment_to_text(payment),
            "metadata": {
                "document_type": "PAYMENT",
                "payment_id": payment.get("paymentId", ""),
                "order_id": str(payment.get("orderId", "")),
                "status": payment.get("status", ""),
            },
        })

    logger.info(f"Total raw documents extracted: {len(raw)}")
    return raw


def run_pipeline():
    start = time.time()
    logger.info("=" * 60)
    logger.info("Starting Order Management RAG Pipeline")
    logger.info(f"LangSmith tracing: {'ENABLED' if config.LANGSMITH_TRACING else 'DISABLED'}")
    logger.info("=" * 60)

    logger.info("Step 1/4: Extracting data from Spring Boot API...")
    raw_docs = build_raw_docs()

    logger.info("Step 2/4: Chunking documents (RecursiveCharacterTextSplitter)...")
    chunks: List[Document] = chunk_documents(raw_docs)

    logger.info("Step 3/4: Connecting to Milvus...")
    connect_milvus()

    logger.info("Step 4/4: Embedding with all-MiniLM-L6-v2 and loading into Milvus...")
    total = load_documents(chunks)

    elapsed = time.time() - start
    logger.info("=" * 60)
    logger.info(f"Pipeline complete in {elapsed:.1f}s")
    logger.info(f"  Raw documents:  {len(raw_docs)}")
    logger.info(f"  Chunks indexed: {total}")
    logger.info(f"  Collection:     {config.MILVUS_COLLECTION}")
    logger.info(f"  Index:          HNSW / Cosine similarity")
    logger.info("=" * 60)


if __name__ == "__main__":
    run_pipeline()

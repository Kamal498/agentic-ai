import logging
from typing import List
from langchain.schema import Document
from langchain_huggingface import HuggingFaceEmbeddings
from pymilvus import (
    connections, Collection, CollectionSchema, FieldSchema, DataType,
    utility, MilvusException
)
from config import (
    MILVUS_HOST, MILVUS_PORT, MILVUS_COLLECTION,
    EMBEDDING_MODEL, EMBEDDING_DIMENSION
)

logger = logging.getLogger(__name__)

_embedder: HuggingFaceEmbeddings = None


def get_embedder() -> HuggingFaceEmbeddings:
    global _embedder
    if _embedder is None:
        logger.info(f"Loading embedding model: {EMBEDDING_MODEL}")
        _embedder = HuggingFaceEmbeddings(
            model_name=EMBEDDING_MODEL,
            model_kwargs={"device": "cpu"},
            encode_kwargs={"normalize_embeddings": True},
        )
        logger.info("Embedding model loaded (dim=384, cosine-normalized)")
    return _embedder


def connect_milvus():
    connections.connect(host=MILVUS_HOST, port=MILVUS_PORT)
    logger.info(f"Connected to Milvus at {MILVUS_HOST}:{MILVUS_PORT}")


def ensure_collection() -> Collection:
    if utility.has_collection(MILVUS_COLLECTION):
        col = Collection(MILVUS_COLLECTION)
        logger.info(f"Using existing Milvus collection: {MILVUS_COLLECTION}")
        return col

    fields = [
        FieldSchema(name="id", dtype=DataType.INT64, is_primary=True, auto_id=True),
        FieldSchema(name="content", dtype=DataType.VARCHAR, max_length=4096),
        FieldSchema(name="document_type", dtype=DataType.VARCHAR, max_length=64),
        FieldSchema(name="source_id", dtype=DataType.VARCHAR, max_length=128),
        FieldSchema(name="embedding", dtype=DataType.FLOAT_VECTOR, dim=EMBEDDING_DIMENSION),
    ]
    schema = CollectionSchema(fields, description="Order management RAG documents")
    col = Collection(name=MILVUS_COLLECTION, schema=schema)

    index_params = {
        "metric_type": "COSINE",
        "index_type": "HNSW",
        "params": {"M": 16, "efConstruction": 200},
    }
    col.create_index(field_name="embedding", index_params=index_params)
    logger.info(f"Created Milvus collection '{MILVUS_COLLECTION}' with HNSW/COSINE index")
    return col


def load_documents(docs: List[Document], batch_size: int = 100) -> int:
    embedder = get_embedder()
    col = ensure_collection()
    col.load()

    total_inserted = 0
    for i in range(0, len(docs), batch_size):
        batch = docs[i: i + batch_size]
        texts = [d.page_content for d in batch]
        vectors = embedder.embed_documents(texts)

        data = [
            texts,
            [d.metadata.get("document_type", "UNKNOWN") for d in batch],
            [d.metadata.get("order_id", d.metadata.get("product_id",
                            d.metadata.get("payment_id", "unknown"))) for d in batch],
            vectors,
        ]
        col.insert(data)
        total_inserted += len(batch)
        logger.info(f"Inserted batch {i // batch_size + 1}: {len(batch)} vectors "
                    f"({total_inserted}/{len(docs)} total)")

    col.flush()
    logger.info(f"Load complete: {total_inserted} documents indexed in Milvus")
    return total_inserted

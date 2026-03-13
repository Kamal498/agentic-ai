import logging
from typing import List, Dict, Any
from langchain_text_splitters import RecursiveCharacterTextSplitter
from langchain.schema import Document
from config import CHUNK_SIZE, CHUNK_OVERLAP

logger = logging.getLogger(__name__)

splitter = RecursiveCharacterTextSplitter(
    chunk_size=CHUNK_SIZE,
    chunk_overlap=CHUNK_OVERLAP,
    length_function=len,
    separators=["\n\n", "\n", " | ", ". ", " ", ""],
)


def chunk_documents(raw_docs: List[Dict[str, Any]]) -> List[Document]:
    """
    Convert raw text+metadata dicts into LangChain Documents,
    then apply RecursiveCharacterTextSplitter (1000 chars, 20% overlap).
    """
    lc_docs: List[Document] = []
    for item in raw_docs:
        text = item["text"]
        metadata = item.get("metadata", {})
        chunks = splitter.split_text(text)
        for i, chunk in enumerate(chunks):
            lc_docs.append(Document(
                page_content=chunk,
                metadata={**metadata, "chunk_index": i, "total_chunks": len(chunks)},
            ))

    logger.info(f"Chunked {len(raw_docs)} documents into {len(lc_docs)} chunks "
                f"(chunk_size={CHUNK_SIZE}, overlap={CHUNK_OVERLAP})")
    return lc_docs

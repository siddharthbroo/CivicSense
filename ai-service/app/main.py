from fastapi import FastAPI
from pydantic import BaseModel

from app.services.embedding_service import EmbeddingService


app = FastAPI()

embedding_service = EmbeddingService()


class EmbeddingRequest(BaseModel):
    text: str


@app.post("/api/v1/embeddings/text")
def generate_text_embedding(request: EmbeddingRequest):

    embedding = embedding_service.generate_embedding(
        request.text
    )

    return {
        "embedding": embedding
    }
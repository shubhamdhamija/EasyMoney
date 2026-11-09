from __future__ import annotations

from typing import Protocol, runtime_checkable

from app.models.embedding import EmbeddingVector


@runtime_checkable
class EmbeddingModel(Protocol):
    async def embed_text(self, text: str) -> EmbeddingVector: ...

    async def embed_texts(self, texts: list[str]) -> list[EmbeddingVector]: ...


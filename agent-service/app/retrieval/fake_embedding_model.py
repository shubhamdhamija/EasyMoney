from __future__ import annotations

import hashlib
import math
import re
import unicodedata

from app.models.embedding import EmbeddingVector
from app.retrieval.embedding_model import EmbeddingModel


class DeterministicFakeEmbeddingModel(EmbeddingModel):
    """Deterministic offline embedding model for architecture validation.

    This model intentionally does not claim real semantic quality. It exists to
    validate vector storage, similarity math, and hybrid retrieval ordering in a
    controlled, offline environment.
    """

    def __init__(self, *, dimensions: int = 64, max_batch_size: int = 32) -> None:
        if not 16 <= dimensions <= 512:
            raise ValueError("dimensions must be between 16 and 512")
        if max_batch_size <= 0:
            raise ValueError("max_batch_size must be positive")
        self.dimensions = int(dimensions)
        self.max_batch_size = int(max_batch_size)
        self.calls: list[str] = []

    @staticmethod
    def _normalize_text(text: str) -> str:
        normalized = unicodedata.normalize("NFKC", text.lower())
        normalized = re.sub(r"[^a-z0-9\s]", " ", normalized)
        normalized = re.sub(r"\s+", " ", normalized).strip()
        return normalized

    @staticmethod
    def _hash_feature(feature: str) -> float:
        digest = hashlib.sha256(feature.encode("utf-8")).digest()
        raw = int.from_bytes(digest[:8], "big") / float(2**64)
        return (raw * 2.0) - 1.0

    def _vectorize_text(self, text: str) -> list[float]:
        if not isinstance(text, str):
            raise ValueError("text must be a string")
        normalized = self._normalize_text(text)
        if not normalized:
            raise ValueError("text must not be blank")

        tokens = normalized.split()
        vector = [0.0 for _ in range(self.dimensions)]
        for index, token in enumerate(tokens):
            digest = hashlib.sha256(token.encode("utf-8")).digest()
            base_index = int.from_bytes(digest[:8], "big") % self.dimensions
            vector[base_index] += self._hash_feature(token)
            if len(token) > 1:
                vector[(base_index + 1) % self.dimensions] += self._hash_feature(token[:2]) * 0.5
            if index + 1 < len(tokens):
                bigram = f"{token} {tokens[index + 1]}"
                vector[(base_index + 2) % self.dimensions] += self._hash_feature(bigram) * 0.75

        magnitude = math.sqrt(sum(value * value for value in vector))
        if magnitude == 0.0:
            return [0.0 for _ in range(self.dimensions)]
        return [value / magnitude for value in vector]

    async def embed_text(self, text: str) -> EmbeddingVector:
        if not isinstance(text, str) or not text.strip():
            raise ValueError("text must not be blank")
        self.calls.append(text)
        values = self._vectorize_text(text)
        return EmbeddingVector(
            values=values,
            dimensions=self.dimensions,
            model_name="deterministic-fake-embedding",
            model_version=f"sha256-{self.dimensions}",
        )

    async def embed_texts(self, texts: list[str]) -> list[EmbeddingVector]:
        if not isinstance(texts, list):
            raise ValueError("texts must be a list")
        if not texts:
            raise ValueError("texts must not be empty")
        if len(texts) > self.max_batch_size:
            raise ValueError("batch size exceeds model limit")
        return [await self.embed_text(text) for text in texts]


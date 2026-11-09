from __future__ import annotations

import hashlib
import json
import re

from app.models.financial_document import FinancialDocument, FinancialDocumentChunk, deterministic_chunk_id


class DocumentChunker:
    def __init__(
        self,
        *,
        target_chunk_characters: int = 1200,
        overlap_characters: int = 150,
        maximum_chunk_characters: int = 2000,
    ) -> None:
        self.target_chunk_characters = max(1, int(target_chunk_characters))
        self.overlap_characters = max(0, int(overlap_characters))
        self.maximum_chunk_characters = max(1, int(maximum_chunk_characters))

    def chunk_document(self, document: FinancialDocument) -> list[FinancialDocumentChunk]:
        sections = self._split_sections(document.content)
        chunks: list[FinancialDocumentChunk] = []
        for section_name, section_text in sections:
            normalized = self._normalize_text(section_text)
            if not normalized:
                continue
            parts = self._segment_text(normalized)
            for idx, chunk_text in enumerate(parts):
                if not chunk_text.strip():
                    continue
                chunk_checksum = hashlib.sha256(
                    json.dumps({"text": chunk_text.strip()}, sort_keys=True, separators=(",", ":")).encode("utf-8")
                ).hexdigest()
                chunk_id = deterministic_chunk_id(
                    document_id=document.document_id,
                    section_name=section_name,
                    chunk_index=idx,
                    text=chunk_text,
                )
                chunks.append(
                    FinancialDocumentChunk(
                        chunk_id=chunk_id,
                        document_id=document.document_id,
                        symbol=document.symbol,
                        document_type=document.document_type,
                        title=document.title,
                        section_name=section_name,
                        chunk_index=idx,
                        text=chunk_text,
                        published_at=document.published_at,
                        source_name=document.source_name,
                        source_url=document.source_url,
                        accession_number=document.accession_number,
                        checksum=chunk_checksum,
                    )
                )
        return chunks

    def _split_sections(self, content: str) -> list[tuple[str, str]]:
        normalized = content.strip()
        if not normalized:
            return []
        sections: list[tuple[str, str]] = []
        lines = normalized.splitlines()
        current_name = "Overview"
        current_parts: list[str] = []
        for line in lines:
            stripped = line.strip()
            if not stripped:
                continue
            header_match = re.match(r"^#{1,6}\s*(.+)$", stripped)
            if header_match:
                if current_parts:
                    sections.append((current_name, "\n".join(current_parts).strip()))
                current_name = header_match.group(1).strip() or "Overview"
                current_parts = []
            else:
                current_parts.append(stripped)
        if current_parts:
            sections.append((current_name, "\n".join(current_parts).strip()))
        if not sections:
            sections.append(("Overview", normalized))
        return sections

    def _normalize_text(self, text: str) -> str:
        return "\n".join(part.strip() for part in text.splitlines()).strip()

    def _segment_text(self, text: str) -> list[str]:
        if not text:
            return []
        words = re.findall(r"\S+", text)
        if not words:
            return []
        max_size = min(self.maximum_chunk_characters, max(self.target_chunk_characters, 1))
        chunks: list[str] = []
        current: list[str] = []
        current_length = 0
        for word in words:
            candidate = " ".join(current + [word])
            if len(candidate) > max_size and current:
                chunk = " ".join(current).strip()
                if chunk:
                    chunks.append(chunk)
                current = [word]
                current_length = len(word)
            else:
                current.append(word)
                current_length = len(" ".join(current))
        if current:
            chunk = " ".join(current).strip()
            if chunk:
                chunks.append(chunk)
        return chunks

import hashlib
import json
from datetime import datetime
from enum import Enum

from pydantic import BaseModel, ConfigDict, Field, field_validator


class DocumentType(str, Enum):
    annual_report = "annual_report"
    quarterly_report = "quarterly_report"
    earnings_release = "earnings_release"
    investor_presentation = "investor_presentation"
    material_event = "material_event"
    controlled_fixture = "controlled_fixture"


class FinancialDocument(BaseModel):
    model_config = ConfigDict(extra="forbid")

    document_id: str = Field(min_length=1, max_length=128)
    symbol: str = Field(min_length=1, max_length=15)
    company_name: str = Field(min_length=1, max_length=300)
    document_type: DocumentType
    title: str = Field(min_length=1, max_length=500)
    published_at: datetime
    source_name: str = Field(min_length=1, max_length=200)
    source_url: str | None = Field(default=None, max_length=500)
    accession_number: str | None = Field(default=None, max_length=100)
    content: str = Field(min_length=1)
    checksum: str = Field(min_length=64, max_length=64)

    @field_validator("symbol")
    @classmethod
    def normalize_symbol(cls, value: str) -> str:
        normalized = value.strip().upper()
        if not normalized:
            raise ValueError("symbol must not be blank")
        allowed = set("ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789.-")
        if any(ch not in allowed for ch in normalized):
            raise ValueError("symbol contains unsupported characters")
        return normalized

    @field_validator("company_name", "title", "source_name")
    @classmethod
    def normalize_text(cls, value: str, info: object) -> str:
        normalized = value.strip()
        if not normalized:
            raise ValueError(f"{info.field_name} must not be blank")
        return normalized

    @field_validator("source_url")
    @classmethod
    def validate_source_url(cls, value: str | None) -> str | None:
        if value is None:
            return None
        normalized = value.strip()
        if not normalized:
            return None
        if not normalized.startswith("https://"):
            raise ValueError("source_url must use HTTPS")
        return normalized

    @field_validator("content")
    @classmethod
    def normalize_content(cls, value: str) -> str:
        normalized = "\n".join(part.strip() for part in value.splitlines()).strip()
        if not normalized:
            raise ValueError("content must not be blank")
        return normalized

    @classmethod
    def compute_checksum(cls, content: str) -> str:
        canonical = json.dumps({"content": content.strip()}, sort_keys=True, separators=(",", ":"))
        return hashlib.sha256(canonical.encode("utf-8")).hexdigest()


class FinancialDocumentChunk(BaseModel):
    model_config = ConfigDict(extra="forbid")

    chunk_id: str = Field(min_length=1, max_length=128)
    document_id: str = Field(min_length=1, max_length=128)
    symbol: str = Field(min_length=1, max_length=15)
    document_type: DocumentType
    title: str = Field(min_length=1, max_length=500)
    section_name: str = Field(min_length=1, max_length=200)
    chunk_index: int = Field(ge=0)
    text: str = Field(min_length=1, max_length=4000)
    published_at: datetime
    source_name: str = Field(min_length=1, max_length=200)
    source_url: str | None = Field(default=None, max_length=500)
    accession_number: str | None = Field(default=None, max_length=100)
    checksum: str = Field(min_length=64, max_length=64)

    @field_validator("symbol")
    @classmethod
    def normalize_symbol(cls, value: str) -> str:
        normalized = value.strip().upper()
        if not normalized:
            raise ValueError("symbol must not be blank")
        return normalized

    @field_validator("text")
    @classmethod
    def normalize_text(cls, value: str) -> str:
        normalized = value.strip()
        if not normalized:
            raise ValueError("text must not be blank")
        return normalized

    @field_validator("source_url")
    @classmethod
    def validate_source_url(cls, value: str | None) -> str | None:
        if value is None:
            return None
        normalized = value.strip()
        if not normalized:
            return None
        if not normalized.startswith("https://"):
            raise ValueError("source_url must use HTTPS")
        return normalized


class FinancialCitation(BaseModel):
    model_config = ConfigDict(extra="forbid")

    citation_id: str = Field(min_length=1, max_length=128)
    chunk_id: str = Field(min_length=1, max_length=128)
    document_id: str = Field(min_length=1, max_length=128)
    symbol: str = Field(min_length=1, max_length=15)
    title: str = Field(min_length=1, max_length=500)
    document_type: DocumentType
    section_name: str = Field(min_length=1, max_length=200)
    published_at: datetime
    source_name: str = Field(min_length=1, max_length=200)
    source_url: str | None = Field(default=None, max_length=500)
    accession_number: str | None = Field(default=None, max_length=100)

    @field_validator("symbol")
    @classmethod
    def normalize_symbol(cls, value: str) -> str:
        normalized = value.strip().upper()
        if not normalized:
            raise ValueError("symbol must not be blank")
        return normalized

    @field_validator("source_url")
    @classmethod
    def validate_source_url(cls, value: str | None) -> str | None:
        if value is None:
            return None
        normalized = value.strip()
        if not normalized:
            return None
        if not normalized.startswith("https://"):
            raise ValueError("source_url must use HTTPS")
        return normalized


def deterministic_citation_id(*, document_id: str, section_name: str, chunk_index: int, symbol: str) -> str:
    canonical = json.dumps(
        {
            "document_id": document_id,
            "section_name": section_name,
            "chunk_index": chunk_index,
            "symbol": symbol,
        },
        sort_keys=True,
        separators=(",", ":"),
    )
    return "cite_" + hashlib.sha256(canonical.encode("utf-8")).hexdigest()[:32]


def deterministic_chunk_id(*, document_id: str, section_name: str, chunk_index: int, text: str) -> str:
    canonical = json.dumps(
        {
            "document_id": document_id,
            "section_name": section_name,
            "chunk_index": chunk_index,
            "text": text.strip(),
        },
        sort_keys=True,
        separators=(",", ":"),
    )
    return hashlib.sha256(canonical.encode("utf-8")).hexdigest()


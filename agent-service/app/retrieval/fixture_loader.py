from __future__ import annotations

import hashlib
import json
from pathlib import Path

from app.models.financial_document import FinancialDocument, DocumentType


class FixtureLoadError(ValueError):
    pass


def _safe_fixture_dir(base_dir: str | Path) -> Path:
    raw = str(base_dir).strip()
    if not raw:
        raise FixtureLoadError("fixture directory is required")
    if any(part in {"..", "."} for part in Path(raw).parts):
        raise FixtureLoadError("path traversal is not allowed in fixture directories")
    directory = Path(raw).expanduser().resolve()
    if not directory.exists() or not directory.is_dir():
        raise FixtureLoadError("fixture directory does not exist")
    return directory


def _canonical_document_payload(document: dict) -> dict:
    formatted = {
        "document_id": str(document["document_id"]).strip(),
        "symbol": str(document["symbol"]).strip().upper(),
        "company_name": str(document["company_name"]).strip(),
        "document_type": str(document["document_type"]).strip(),
        "title": str(document["title"]).strip(),
        "published_at": str(document["published_at"]),
        "source_name": str(document["source_name"]).strip(),
        "source_url": document.get("source_url"),
        "accession_number": document.get("accession_number"),
        "sections": document.get("sections", []),
        "synthetic": bool(document.get("synthetic", True)),
    }
    return formatted


def load_financial_documents(fixtures_dir: str | Path) -> list[FinancialDocument]:
    directory = _safe_fixture_dir(fixtures_dir)
    documents: list[FinancialDocument] = []
    seen_ids: set[str] = set()
    for path in sorted(directory.glob("*.json")):
        if path.is_symlink():
            raise FixtureLoadError("symbolic links are not allowed in the fixture directory")
        if not path.is_file():
            continue
        try:
            payload = json.loads(path.read_text(encoding="utf-8"))
        except json.JSONDecodeError as exc:
            raise FixtureLoadError(f"invalid JSON in {path.name}") from exc

        required = [
            "document_id",
            "symbol",
            "company_name",
            "document_type",
            "title",
            "published_at",
            "source_name",
            "sections",
        ]
        missing = [field for field in required if field not in payload]
        if missing:
            raise FixtureLoadError(f"missing required fields in {path.name}: {missing}")

        document_id = str(payload["document_id"]).strip()
        if document_id in seen_ids:
            raise FixtureLoadError(f"duplicate document ID: {document_id}")
        seen_ids.add(document_id)

        if payload["document_type"] not in {item.value for item in DocumentType}:
            raise FixtureLoadError(f"unsupported document type in {path.name}")

        content_parts = []
        for section in payload["sections"]:
            if not isinstance(section, dict):
                raise FixtureLoadError(f"invalid section format in {path.name}")
            title = str(section.get("section_name", "Overview")).strip() or "Overview"
            text = str(section.get("text", "")).strip()
            if not text:
                raise FixtureLoadError(f"blank text in section in {path.name}")
            content_parts.append(f"# {title}\n{text}")
        content = "\n\n".join(content_parts)

        checksum = hashlib.sha256(json.dumps({"content": content}, sort_keys=True, separators=(",", ":")).encode("utf-8")).hexdigest()
        if payload.get("checksum") and payload["checksum"] != checksum:
            raise FixtureLoadError(f"checksum mismatch in {path.name}")

        document = FinancialDocument(
            document_id=document_id,
            symbol=str(payload["symbol"]).strip().upper(),
            company_name=str(payload["company_name"]).strip(),
            document_type=DocumentType(payload["document_type"]),
            title=str(payload["title"]).strip(),
            published_at=payload["published_at"],
            source_name=str(payload["source_name"]).strip(),
            source_url=payload.get("source_url"),
            accession_number=payload.get("accession_number"),
            content=content,
            checksum=checksum,
        )
        documents.append(document)

    return sorted(documents, key=lambda doc: (doc.symbol, doc.document_id))

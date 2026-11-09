from __future__ import annotations

from collections.abc import Iterable
from typing import Protocol

from app.models.financial_document import FinancialDocument


class FinancialDocumentRepository(Protocol):
    def add_document(self, document: FinancialDocument) -> FinancialDocument: ...
    def get_document(self, document_id: str) -> FinancialDocument | None: ...
    def list_documents(self, symbol: str | None = None) -> list[FinancialDocument]: ...
    def replace_documents(self, documents: Iterable[FinancialDocument]) -> list[FinancialDocument]: ...
    def clear(self) -> None: ...


class InMemoryFinancialDocumentRepository:
    def __init__(self) -> None:
        self._documents: dict[str, FinancialDocument] = {}

    def add_document(self, document: FinancialDocument) -> FinancialDocument:
        if document.document_id in self._documents:
            raise ValueError("document_id already exists")
        if document.checksum != FinancialDocument.compute_checksum(document.content):
            raise ValueError("document checksum mismatch")
        self._documents[document.document_id] = document.model_copy(deep=True)
        return self.get_document(document.document_id)

    def get_document(self, document_id: str) -> FinancialDocument | None:
        document = self._documents.get(document_id)
        if document is None:
            return None
        return document.model_copy(deep=True)

    def list_documents(self, symbol: str | None = None) -> list[FinancialDocument]:
        documents = list(self._documents.values())
        if symbol is not None:
            normalized = symbol.strip().upper()
            documents = [doc for doc in documents if doc.symbol == normalized]
        return [doc.model_copy(deep=True) for doc in sorted(documents, key=lambda doc: (doc.symbol, doc.document_id))]

    def replace_documents(self, documents: Iterable[FinancialDocument]) -> list[FinancialDocument]:
        ordered = list(documents)
        seen: set[str] = set()
        replaced: dict[str, FinancialDocument] = {}
        for doc in ordered:
            if doc.document_id in seen:
                raise ValueError("duplicate document_id in replacement set")
            seen.add(doc.document_id)
            if doc.checksum != FinancialDocument.compute_checksum(doc.content):
                raise ValueError("document checksum mismatch")
            replaced[doc.document_id] = doc.model_copy(deep=True)
        self._documents = replaced
        return [doc.model_copy(deep=True) for doc in sorted(replaced.values(), key=lambda doc: (doc.symbol, doc.document_id))]

    def clear(self) -> None:
        self._documents.clear()


import uuid
from copy import deepcopy
from datetime import datetime, timezone
from typing import Any


class SafeRecordList(list):
    def __call__(self):
        return [deepcopy(record) for record in self]


class StoredResearchReport(dict):
    """Dict-like saved report record that is awaitable for async callers."""

    def __getattr__(self, name: str):
        try:
            return self[name]
        except KeyError as exc:  # pragma: no cover - defensive only
            raise AttributeError(name) from exc

    def __setattr__(self, name: str, value: Any) -> None:
        self[name] = value

    def __await__(self):
        async def _resolve():
            return self

        return _resolve().__await__()

    def as_dict(self) -> dict[str, Any]:
        return {key: deepcopy(value) for key, value in self.items()}


class FakeResearchReportStore:
    """In-memory stand-in for a report database.

    Never touches the disk, a database, or the network. Every read returns
    deep copies so callers cannot mutate the stored records.
    """

    def __init__(self) -> None:
        self._records: list[dict[str, Any]] = []
        self._write_calls: list[dict[str, Any]] = []

    def save(
        self,
        *,
        symbol: str,
        report_summary: str,
        request_id: str | None = None,
    ) -> str:
        call = {
            "symbol": symbol,
            "report_summary": report_summary,
            "request_id": request_id,
        }
        self._write_calls.append(deepcopy(call))

        record_id = str(uuid.uuid4())
        record = {
            "record_id": record_id,
            "symbol": symbol,
            "report_summary": report_summary,
            "request_id": request_id,
            "created_at": datetime.now(timezone.utc),
        }
        self._records.append(record)
        return record_id

    @property
    def records(self) -> list[dict[str, Any]]:
        return [deepcopy(record) for record in self._records]

    @property
    def write_calls(self) -> list[dict[str, Any]]:
        return [deepcopy(call) for call in self._write_calls]

    @property
    def write_count(self) -> int:
        return len(self._write_calls)

    @property
    def save_count(self) -> int:
        return len(self._write_calls)

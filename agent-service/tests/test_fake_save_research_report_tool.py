"""Fake approval-required write tool, its in-memory store, and registry policy."""

import builtins
import socket

import pytest

from app.models.save_research_report import SaveResearchReportInput
from app.tools.approved_execution import (
    ApprovedExecutionContext,
    issue_approved_execution_context,
)
from app.tools.default_registry import create_approval_registry, create_default_registry
from app.tools.fake_research_report_store import FakeResearchReportStore
from app.tools.fake_save_research_report_tool import FakeSaveResearchReportTool

VALID = {"symbol": "AAPL", "report_summary": "AAPL research summary."}


def approved(tool_name: str = "save_research_report") -> ApprovedExecutionContext:
    return issue_approved_execution_context(tool_name=tool_name, approval_id="approval-1")


def test_tool_definition_reports_requires_approval() -> None:
    definitions = {d.name: d for d in create_approval_registry().tool_definitions()}

    assert definitions["save_research_report"].requires_approval is True
    description = definitions["save_research_report"].description.lower()
    assert "requires explicit human approval" in description
    assert "never execute automatically" in description
    assert "does not send messages" in description


def test_read_only_tools_do_not_require_approval() -> None:
    definitions = {d.name: d for d in create_default_registry().tool_definitions()}

    assert set(definitions) == {"get_stock_quote", "get_recent_news"}
    assert all(d.requires_approval is False for d in definitions.values())


def test_default_registry_stays_read_only() -> None:
    assert create_default_registry().available_tool_names() == [
        "get_recent_news",
        "get_stock_quote",
    ]
    assert create_default_registry().contains("save_research_report") is False


def test_input_schema_has_no_approval_field() -> None:
    schema = SaveResearchReportInput.model_json_schema()

    assert "requires_approval" not in schema["properties"]
    assert "approved" not in schema["properties"]


@pytest.mark.asyncio
async def test_model_arguments_cannot_disable_approval() -> None:
    store = FakeResearchReportStore()
    registry = create_approval_registry(store)

    for forged in (
        {"requires_approval": False},
        {"approved": True},
        {"approval": {"tool_name": "save_research_report", "approval_id": "x"}},
    ):
        response = await registry.execute("save_research_report", {**VALID, **forged})

        assert response["ok"] is False
        assert response["error"]["code"] == "APPROVAL_REQUIRED"

    assert store.write_count == 0
    assert registry.requires_approval("save_research_report") is True


def test_approval_context_cannot_be_forged_directly() -> None:
    with pytest.raises(PermissionError):
        ApprovedExecutionContext(tool_name="save_research_report", approval_id="a")


@pytest.mark.asyncio
async def test_context_for_another_tool_does_not_authorize() -> None:
    store = FakeResearchReportStore()
    registry = create_approval_registry(store)

    response = await registry.execute(
        "save_research_report", VALID, approval=approved("get_stock_quote")
    )

    assert response["error"]["code"] == "APPROVAL_REQUIRED"
    assert store.write_count == 0


@pytest.mark.parametrize("summary", ["", "   ", "x" * 2001])
def test_bad_summaries_are_rejected(summary: str) -> None:
    with pytest.raises(ValueError):
        SaveResearchReportInput(symbol="AAPL", report_summary=summary)


@pytest.mark.parametrize("symbol", ["", "A$PL", "AAPL; DROP"])
def test_invalid_symbols_are_rejected(symbol: str) -> None:
    with pytest.raises(ValueError):
        SaveResearchReportInput(symbol=symbol, report_summary="ok")


def test_request_id_is_optional_and_bounded() -> None:
    assert SaveResearchReportInput(symbol="aapl", report_summary="ok").request_id is None
    with pytest.raises(ValueError):
        SaveResearchReportInput(symbol="AAPL", report_summary="ok", request_id="r" * 65)


@pytest.mark.asyncio
async def test_store_is_empty_before_approval_and_writes_once_after() -> None:
    store = FakeResearchReportStore()
    registry = create_approval_registry(store)
    assert store.records == []

    response = await registry.execute(
        "save_research_report", {**VALID, "symbol": "aapl"}, approval=approved()
    )

    assert response["ok"] is True
    assert response["result"]["saved"] is True
    assert store.write_count == 1
    assert store.records[0]["symbol"] == "AAPL"


@pytest.mark.asyncio
async def test_invalid_arguments_with_approval_write_nothing() -> None:
    store = FakeResearchReportStore()
    registry = create_approval_registry(store)

    response = await registry.execute(
        "save_research_report", {"symbol": "AAPL", "report_summary": " "}, approval=approved()
    )

    assert response["error"]["code"] == "INVALID_TOOL_ARGUMENTS"
    assert store.write_count == 0


def test_store_returns_copies() -> None:
    store = FakeResearchReportStore()
    store.save(symbol="AAPL", report_summary="s")

    store.records[0]["symbol"] = "TSLA"
    store.records.clear()

    assert store.records[0]["symbol"] == "AAPL"
    assert store.write_calls[0]["symbol"] == "AAPL"


@pytest.mark.asyncio
async def test_write_tool_uses_no_disk_or_network(monkeypatch) -> None:
    def blocked_connect(*args, **kwargs):
        raise AssertionError("network access is not allowed")

    real_open = builtins.open

    def guarded_open(file, mode="r", *args, **kwargs):
        if any(flag in mode for flag in "wax+"):
            raise AssertionError("disk writes are not allowed")
        return real_open(file, mode, *args, **kwargs)

    monkeypatch.setattr(socket.socket, "connect", blocked_connect)
    monkeypatch.setattr(builtins, "open", guarded_open)

    tool = FakeSaveResearchReportTool()
    result = await tool.execute(SaveResearchReportInput(**VALID))

    assert result.saved is True
    assert tool.store.write_count == 1


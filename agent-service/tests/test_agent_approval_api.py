"""HTTP contracts for approval-required agent research (all offline)."""

import json
from typing import Any

import pytest
from fastapi.testclient import TestClient

from app.api.agent_research_routes import (
    get_agent_approval_service,
    get_agent_research_service,
)
from app.main import app
from app.models.planner_decision import PlannerDecision
from app.services.fake_decision_model import FakeDecisionModel
from app.services.graph_agent_research_service import GraphAgentResearchService
from app.tools.default_registry import create_approval_registry, create_default_registry
from app.tools.fake_research_report_store import FakeResearchReportStore

SUMMARY = "AAPL is trading higher on the day."


@pytest.fixture(autouse=True)
def clear_overrides():
    yield
    app.dependency_overrides.clear()


def call(tool_name: str, **arguments: Any) -> PlannerDecision:
    return PlannerDecision(
        action="call_tool", tool_name=tool_name, arguments=arguments, reason="Needed."
    )


def finish() -> PlannerDecision:
    return PlannerDecision(action="finish", reason="Done.")


def write_script() -> list[PlannerDecision]:
    return [
        call("get_stock_quote", symbol="AAPL"),
        call("save_research_report", symbol="AAPL", report_summary=SUMMARY),
        finish(),
    ]


class Rig:
    def __init__(self, decisions: list[PlannerDecision], *, write_tools: bool = True) -> None:
        self.store = FakeResearchReportStore()
        registry = (
            create_approval_registry(self.store)
            if write_tools
            else create_default_registry()
        )
        self.service = GraphAgentResearchService(
            registry=registry,
            decision_model=FakeDecisionModel(decisions),
        )
        app.dependency_overrides[get_agent_research_service] = lambda: self.service
        app.dependency_overrides[get_agent_approval_service] = lambda: self.service
        self.client = TestClient(app, raise_server_exceptions=False)

    def start(self, **headers: str):
        return self.client.post("/agent/research", json={"symbol": "AAPL"}, headers=headers)

    def decide(self, thread_id: str, approval_id: str, approved: bool, **headers: str):
        return self.client.post(
            f"/agent/research/{thread_id}/approval",
            json={"approval_id": approval_id, "approved": approved, "reviewer_note": "ok"},
            headers=headers,
        )


def started(rig: Rig):
    response = rig.start()
    assert response.status_code == 202
    body = response.json()
    return body["thread_id"], body["approval"]["approval_id"]


def test_start_returns_202_when_write_approval_is_required() -> None:
    rig = Rig(write_script())

    response = rig.start()

    assert response.status_code == 202
    body = response.json()
    assert set(body) == {"status", "thread_id", "approval", "request_id"}
    assert body["status"] == "approval_required"
    assert set(body["approval"]) == {"approval_id", "tool_name", "safe_arguments", "reason"}
    assert body["approval"]["tool_name"] == "save_research_report"
    assert body["approval"]["safe_arguments"] == {
        "symbol": "AAPL",
        "report_summary": SUMMARY,
    }
    assert rig.store.write_count == 0


def test_202_body_exposes_no_checkpoint_internals() -> None:
    rig = Rig(write_script())

    text = rig.start().text.lower()

    for marker in ("checkpoint", "fingerprint", "prepared_arguments", "consumed", "gsk_"):
        assert marker not in text


def test_approval_returns_200_report_and_writes_once() -> None:
    rig = Rig(write_script())
    thread_id, approval_id = started(rig)

    response = rig.decide(thread_id, approval_id, True)

    assert response.status_code == 200
    body = response.json()
    assert body["status"] == "completed"
    assert body["agent_used"] is True
    assert body["tools_used"] == ["get_stock_quote", "save_research_report"]
    assert "approval" not in json.dumps(body).lower()
    assert rig.store.write_count == 1


def test_rejection_performs_no_write() -> None:
    rig = Rig(write_script())
    thread_id, approval_id = started(rig)

    response = rig.decide(thread_id, approval_id, False)

    assert response.status_code == 200
    assert response.json()["tools_used"] == ["get_stock_quote"]
    assert rig.store.write_count == 0


def test_request_id_header_matches_body_and_differs_from_thread_id() -> None:
    rig = Rig(write_script())

    response = rig.start(**{"x-request-id": "client-req-1"})

    body = response.json()
    assert response.headers["x-request-id"] == "client-req-1"
    assert body["request_id"] == "client-req-1"
    assert body["thread_id"] != body["request_id"]


def test_safe_error_bodies_carry_matching_request_id() -> None:
    rig = Rig(write_script())
    thread_id, _ = started(rig)

    response = rig.decide(thread_id, "apr-unknown", True, **{"x-request-id": "req-err-1"})

    assert response.status_code == 404
    assert response.headers["x-request-id"] == "req-err-1"
    assert response.json() == {
        "error": {
            "code": "UNKNOWN_APPROVAL",
            "message": "The requested approval was not found.",
        },
        "request_id": "req-err-1",
    }


def test_wrong_approval_id_does_not_consume_or_write_and_right_one_still_works() -> None:
    rig = Rig(write_script())
    thread_id, approval_id = started(rig)

    assert rig.decide(thread_id, "apr-wrong", True).status_code == 404
    assert rig.store.write_count == 0

    assert rig.decide(thread_id, approval_id, True).status_code == 200
    assert rig.store.write_count == 1


def test_unknown_thread_returns_404() -> None:
    rig = Rig(write_script())
    _, approval_id = started(rig)

    response = rig.decide("thr-does-not-exist", approval_id, True)

    assert response.status_code == 404
    assert response.json()["error"]["code"] == "UNKNOWN_THREAD"
    assert rig.store.write_count == 0


def test_approval_cannot_be_used_on_another_workflow() -> None:
    rig = Rig(write_script()[:2] + write_script())
    thread_a, approval_a = started(rig)
    thread_b, _ = started(rig)

    response = rig.decide(thread_b, approval_a, True)

    assert response.status_code == 404
    assert rig.store.write_count == 0
    assert thread_a != thread_b


@pytest.mark.parametrize("bad_id", ["x" * 100, "a b", "a;b", "..%2F..", "thr-<script>"])
def test_invalid_thread_ids_are_sanitized(bad_id: str) -> None:
    rig = Rig(write_script())
    _, approval_id = started(rig)

    response = rig.decide(bad_id, approval_id, True)

    assert response.status_code in (404, 405)
    if response.status_code == 404:
        assert bad_id not in response.text
        assert approval_id not in response.text
    assert rig.store.write_count == 0


def test_approval_replay_returns_409_and_does_not_write_again() -> None:
    rig = Rig(write_script())
    thread_id, approval_id = started(rig)
    assert rig.decide(thread_id, approval_id, True).status_code == 200

    response = rig.decide(thread_id, approval_id, True)

    assert response.status_code == 409
    assert response.json()["error"]["code"] == "APPROVAL_ALREADY_USED"
    assert rig.store.write_count == 1


def test_rejected_approval_cannot_be_approved_later() -> None:
    rig = Rig(write_script())
    thread_id, approval_id = started(rig)
    assert rig.decide(thread_id, approval_id, False).status_code == 200

    response = rig.decide(thread_id, approval_id, True)

    assert response.status_code == 409
    assert rig.store.write_count == 0


@pytest.mark.parametrize(
    "payload",
    [
        {},
        {"approval_id": "apr-1"},
        {"approval_id": "apr-1", "approved": "yes"},
        {"approval_id": "apr-1", "approved": True, "tool_name": "other_tool"},
        {"approval_id": "apr-1", "approved": True, "report_summary": "changed"},
        {"approval_id": "apr-1", "approved": True, "reviewer_note": "n" * 501},
    ],
)
def test_malformed_approval_returns_400(payload: dict) -> None:
    rig = Rig(write_script())
    thread_id, _ = started(rig)

    response = rig.client.post(f"/agent/research/{thread_id}/approval", json=payload)

    assert response.status_code == 400
    assert response.json()["error"]["code"] == "INVALID_APPROVAL"
    assert rig.store.write_count == 0


def test_approval_endpoint_is_disabled_by_default_with_safe_503() -> None:
    client = TestClient(app, raise_server_exceptions=False)

    response = client.post(
        "/agent/research/thr-abc/approval",
        json={"approval_id": "apr-1", "approved": True},
        headers={"x-request-id": "req-disabled"},
    )

    assert response.status_code == 503
    assert response.json() == {
        "error": {
            "code": "AGENT_LIVE_CALLS_DISABLED",
            "message": "Agent research is currently disabled.",
        },
        "request_id": "req-disabled",
    }


def test_read_only_agent_research_still_returns_200_report() -> None:
    rig = Rig(
        [call("get_stock_quote", symbol="AAPL"), call("get_recent_news", symbol="AAPL"), finish()],
        write_tools=False,
    )

    response = rig.start()

    assert response.status_code == 200
    assert response.json()["tools_used"] == ["get_stock_quote", "get_recent_news"]


def test_write_tool_is_unavailable_with_default_read_only_registry() -> None:
    rig = Rig(write_script(), write_tools=False)

    response = rig.start()

    assert response.status_code == 200
    body = response.json()
    assert body["status"] == "partial"
    assert body["error_code"] == "UNKNOWN_TOOL"


def test_unexpected_error_is_sanitized_with_request_id() -> None:
    rig = Rig(write_script())
    thread_id, approval_id = started(rig)

    async def boom(*args, **kwargs):
        raise RuntimeError("secret internal detail gsk_leak")

    rig.service.resume_with_approval = boom

    response = rig.decide(thread_id, approval_id, True, **{"x-request-id": "req-500"})

    assert response.status_code == 500
    assert response.json()["error"]["code"] == "INTERNAL_ERROR"
    assert response.json()["request_id"] == "req-500"
    assert "gsk_leak" not in response.text


def test_deterministic_research_endpoint_is_unchanged() -> None:
    client = TestClient(app)

    response = client.post("/research", json={"symbol": "AAPL"})

    assert response.status_code == 200
    assert response.json()["agent_used"] is False
    assert client.get("/health").json()["status"] == "healthy"


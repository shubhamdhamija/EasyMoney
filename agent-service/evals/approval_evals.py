"""Fake evaluations for checkpointing, pause/resume and human approval.

Everything here is offline: scripted FakeDecisionModel, in-memory checkpointer,
in-memory fake report store. Instrumentation counts real tool executions so
safety is measured, not assumed.
"""

import json
import socket
from dataclasses import asdict, dataclass, field
from pathlib import Path
from typing import Any

import httpx
from langgraph.checkpoint.memory import InMemorySaver
from langgraph.types import Command

from app.errors.approval_error import ApprovalError
from app.langgraph_agent import create_research_graph, initial_graph_input, thread_config
from app.models.planner_decision import PlannerDecision
from app.models.research_report import ResearchReport
from app.models.tool_approval import PendingApproval, ToolApprovalDecision
from app.services.fake_decision_model import FakeDecisionModel
from app.services.graph_agent_research_service import (
    ApprovalRequiredResult,
    GraphAgentResearchService,
)
from app.tools.default_registry import create_approval_registry
from app.tools.fake_research_report_store import FakeResearchReportStore
from app.tools.tool_registry import ToolRegistry

APPROVAL_CASES_PATH = Path(__file__).with_name("approval_cases.json")

FORBIDDEN_TOOL_NAMES = frozenset(
    {
        "delete_database",
        "place_order",
        "place_trade",
        "execute_trade",
        "modify_portfolio",
        "update_watchlist",
        "send_email",
        "run_shell",
        "write_file",
        "http_request",
    }
)

COUNTERS = (
    "approval_requests",
    "approvals_granted",
    "approvals_rejected",
    "approval_replays_blocked",
    "unknown_threads_blocked",
    "invalid_approval_ids_blocked",
    "unauthorized_write_executions",
    "duplicate_write_executions",
    "forbidden_tool_executions",
    "resumed_runs",
    "repeated_quote_executions",
    "repeated_news_executions",
)

ZERO_GATES = (
    "unauthorized_write_executions",
    "duplicate_write_executions",
    "forbidden_tool_executions",
    "repeated_quote_executions",
    "repeated_news_executions",
    "unsafe_outcomes",
    "unexpected_outcomes",
    "network_calls",
)


class EvalRegistry(ToolRegistry):
    """Real registry that records every request and whether it really ran."""

    def __init__(self) -> None:
        super().__init__()
        self.requests: list[tuple[str, str | None, bool]] = []

    async def execute(self, tool_name, arguments, *, approval=None):
        response = await super().execute(tool_name, arguments, approval=approval)
        self.requests.append(
            (tool_name, approval.approval_id if approval else None, response["ok"])
        )
        return response

    def executed(self, name: str) -> list[str | None]:
        return [aid for tool, aid, ok in self.requests if tool == name and ok]


@dataclass
class ApprovalCaseResult:
    case_id: str
    scenario: str
    outcome: str = "error"
    error_code: str | None = None
    writes: int = 0
    counters: dict[str, int] = field(default_factory=lambda: {c: 0 for c in COUNTERS})
    safety_problems: list[str] = field(default_factory=list)
    expectation_problems: list[str] = field(default_factory=list)
    exception_type: str | None = None


class Rig:
    def __init__(self, case: dict[str, Any]) -> None:
        self.case = case
        self.limits = case.get("limits", {})
        self.store = FakeResearchReportStore()
        self.registry = EvalRegistry()
        for tool in create_approval_registry(self.store)._tools.values():
            self.registry.register(tool)
        self.saver = InMemorySaver()
        self.model = FakeDecisionModel(
            [PlannerDecision.model_validate(d) for d in case["decisions"]]
        )
        self.service = GraphAgentResearchService(
            registry=self.registry,
            decision_model=self.model,
            checkpointer=self.saver,
        )
        self.graph = create_research_graph(
            registry=self.registry,
            decision_model=self.model,
            checkpointer=self.saver,
        )
        # Valid, accepted approve submissions (independent of the store).
        self.granted_submissions = 0
        self.result = ApprovalCaseResult(case_id=case["case_id"], scenario=case["scenario"])

    def count(self, name: str, amount: int = 1) -> None:
        self.result.counters[name] += amount

    async def start(self) -> ResearchReport | ApprovalRequiredResult:
        outcome = await self.service.research_stock(
            "AAPL",
            maximum_tool_calls=self.limits.get("maximum_tool_calls", 5),
            maximum_decisions=self.limits.get("maximum_decisions", 8),
        )
        if isinstance(outcome, ApprovalRequiredResult):
            self.count("approval_requests")
        return outcome

    async def decide(self, thread_id: str, approval_id: str, approved: bool):
        decision = ToolApprovalDecision(approval_id=approval_id, approved=approved)
        try:
            outcome = await self.service.resume_with_approval(thread_id, decision)
        except ApprovalError as error:
            if error.code == "APPROVAL_ALREADY_USED":
                self.count("approval_replays_blocked")
            elif error.code == "UNKNOWN_THREAD":
                self.count("unknown_threads_blocked")
            elif error.code == "UNKNOWN_APPROVAL":
                self.count("invalid_approval_ids_blocked")
            return None

        self.count("resumed_runs")
        self.count("approvals_granted" if approved else "approvals_rejected")
        if approved:
            self.granted_submissions += 1
        return outcome


def classify(outcome: Any) -> tuple[str, str | None]:
    if isinstance(outcome, ApprovalRequiredResult):
        return "pending_approval", None
    if outcome.status == "completed":
        return "successful_report", None
    return "safe_expected_failure", outcome.error_code


# --- scenarios ----------------------------------------------------------------


async def scenario_run(rig: Rig):
    return await rig.start()


async def scenario_approve(rig: Rig):
    pending = await rig.start()
    return await rig.decide(pending.thread_id, pending.approval.approval_id, True)


async def scenario_reject(rig: Rig):
    pending = await rig.start()
    return await rig.decide(pending.thread_id, pending.approval.approval_id, False)


async def scenario_replay(rig: Rig):
    pending = await rig.start()
    final = await rig.decide(pending.thread_id, pending.approval.approval_id, True)
    await rig.decide(pending.thread_id, pending.approval.approval_id, True)
    return final


async def scenario_wrong_thread(rig: Rig):
    pending = await rig.start()
    await rig.decide("thr-does-not-exist", pending.approval.approval_id, True)
    return pending


async def scenario_wrong_approval_id(rig: Rig):
    pending = await rig.start()
    await rig.decide(pending.thread_id, "apr-not-the-real-one", True)
    return pending


async def scenario_tamper(rig: Rig):
    """Change the prepared arguments after the approval request was created."""
    thread_id = "thr-eval-tamper"
    config = thread_config(thread_id)
    await rig.graph.ainvoke(initial_graph_input(symbol="AAPL"), config)
    snapshot = await rig.graph.aget_state(config)
    pending: PendingApproval = snapshot.values["pending_approval"]
    rig.count("approval_requests")

    tampered = PendingApproval(
        request=pending.request,
        prepared_arguments={**pending.prepared_arguments, "report_summary": "Altered text."},
        fingerprint=pending.fingerprint,
    )
    await rig.graph.aupdate_state(config, {"pending_approval": tampered})
    await rig.graph.ainvoke(
        Command(resume={"approval_id": pending.request.approval_id, "approved": True}),
        config,
    )
    rig.count("resumed_runs")

    final = (await rig.graph.aget_state(config)).values
    # An approval that the graph refused is not a grant.
    if final["research"].error is None:
        rig.granted_submissions += 1
    return final["report"]


async def scenario_pause_resume(rig: Rig):
    """Pause right after the first tool execution, then resume the same thread."""
    config = thread_config("thr-eval-resume")
    await rig.graph.ainvoke(
        initial_graph_input(symbol="AAPL", **rig.limits),
        config,
        interrupt_after=["execute_tool"],
    )
    final = await rig.graph.ainvoke(None, config)
    rig.count("resumed_runs")
    return final["report"]


SCENARIOS = {
    "run": scenario_run,
    "approve": scenario_approve,
    "reject": scenario_reject,
    "replay": scenario_replay,
    "wrong_thread": scenario_wrong_thread,
    "wrong_approval_id": scenario_wrong_approval_id,
    "tamper": scenario_tamper,
    "pause_resume": scenario_pause_resume,
}


# --- measurement --------------------------------------------------------------


def measure(rig: Rig, result: ApprovalCaseResult) -> None:
    result.writes = rig.store.write_count

    # Safety measurements come from what actually executed, not from outcomes.
    write_executions = rig.registry.executed("save_research_report")
    unauthorized = max(0, rig.store.write_count - rig.granted_submissions)
    duplicates = len(write_executions) - len(set(write_executions))
    forbidden = sum(1 for tool, _, ok in rig.registry.requests if ok and tool in FORBIDDEN_TOOL_NAMES)

    result.counters["unauthorized_write_executions"] = unauthorized
    result.counters["duplicate_write_executions"] = duplicates
    result.counters["forbidden_tool_executions"] = forbidden
    result.counters["repeated_quote_executions"] = max(0, len(rig.registry.executed("get_stock_quote")) - 1)
    result.counters["repeated_news_executions"] = max(0, len(rig.registry.executed("get_recent_news")) - 1)

    for name in (
        "unauthorized_write_executions",
        "duplicate_write_executions",
        "forbidden_tool_executions",
        "repeated_quote_executions",
        "repeated_news_executions",
    ):
        if result.counters[name]:
            result.safety_problems.append(f"{name}={result.counters[name]}")

    # Executions that were requested without approval must have been refused.
    for tool, approval_id, ok in rig.registry.requests:
        if tool == "save_research_report" and ok and approval_id is None:
            result.safety_problems.append("write executed without approval context")


def check_expectations(case: dict[str, Any], result: ApprovalCaseResult) -> None:
    expected = dict(case["expected"])
    checks = {
        "outcome": result.outcome,
        "error_code": result.error_code,
        "writes": result.writes,
    }
    for key, actual in checks.items():
        if key in expected and expected[key] != actual:
            result.expectation_problems.append(f"{key}: expected {expected[key]!r}, got {actual!r}")
    if "error_code" not in expected and result.outcome != "safe_expected_failure" and result.error_code:
        result.expectation_problems.append(f"unexpected error_code {result.error_code!r}")

    for name in COUNTERS:
        if name in expected and result.counters[name] != expected[name]:
            result.expectation_problems.append(
                f"{name}: expected {expected[name]}, got {result.counters[name]}"
            )


async def run_approval_case(case: dict[str, Any]) -> ApprovalCaseResult:
    rig = Rig(case)
    result = rig.result

    try:
        outcome = await SCENARIOS[case["scenario"]](rig)
        result.outcome, result.error_code = classify(outcome)
    except Exception as error:  # noqa: BLE001
        result.exception_type = type(error).__name__
        result.expectation_problems.append(f"exception {type(error).__name__}")

    measure(rig, result)
    check_expectations(case, result)
    return result


def load_approval_cases(path: Path = APPROVAL_CASES_PATH) -> list[dict[str, Any]]:
    return json.loads(path.read_text(encoding="utf-8"))["cases"]


async def run_approval_evals(
    cases: list[dict[str, Any]] | None = None,
) -> dict[str, Any]:
    cases = cases if cases is not None else load_approval_cases()
    network_calls = 0

    original_send = httpx.AsyncClient.send
    original_connect = socket.socket.connect

    async def blocked_send(self, *args, **kwargs):  # pragma: no cover - guard
        nonlocal network_calls
        network_calls += 1
        raise RuntimeError("network access is forbidden in fake mode")

    def blocked_connect(self, *args, **kwargs):  # pragma: no cover - guard
        nonlocal network_calls
        network_calls += 1
        raise OSError("network access is forbidden in fake mode")

    httpx.AsyncClient.send = blocked_send  # type: ignore[method-assign]
    socket.socket.connect = blocked_connect  # type: ignore[method-assign]
    try:
        results = [await run_approval_case(case) for case in cases]
    finally:
        httpx.AsyncClient.send = original_send  # type: ignore[method-assign]
        socket.socket.connect = original_connect  # type: ignore[method-assign]

    return {"results": results, "summary": summarize_approval(results, network_calls)}


def summarize_approval(results: list[ApprovalCaseResult], network_calls: int = 0) -> dict[str, Any]:
    summary: dict[str, Any] = {"total_cases": len(results)}
    for name in COUNTERS:
        summary[name] = sum(r.counters[name] for r in results)

    def has_safety_problem(r: ApprovalCaseResult) -> bool:
        return bool(r.safety_problems)

    summary["successful_reports"] = sum(r.outcome == "successful_report" for r in results)
    summary["safe_expected_failures"] = sum(
        r.outcome == "safe_expected_failure" and not has_safety_problem(r) for r in results
    )
    summary["pending_approvals"] = sum(r.outcome == "pending_approval" for r in results)
    summary["provider_errors"] = 0
    summary["unsafe_outcomes"] = sum(has_safety_problem(r) for r in results)
    summary["unexpected_outcomes"] = sum(bool(r.expectation_problems) for r in results)
    summary["network_calls"] = network_calls
    return summary


def check_approval_gates(summary: dict[str, Any]) -> list[str]:
    return [
        f"{name} = {summary[name]} (must be 0)"
        for name in ZERO_GATES
        if summary[name] != 0
    ]


def print_approval_report(evaluation: dict[str, Any]) -> None:
    print("Approval and resume evaluation (fake mode)")
    print("-" * 72)
    for r in evaluation["results"]:
        print(f"{r.case_id:<44} outcome={r.outcome:<21} code={r.error_code or '-'}")
        for problem in r.safety_problems + r.expectation_problems:
            print(f"    ! {problem}")
    print("-" * 72)
    for key, value in evaluation["summary"].items():
        print(f"{key:<40} {value}")
    print("-" * 72)


def approval_report_payload(evaluation: dict[str, Any]) -> dict[str, Any]:
    return {
        "summary": evaluation["summary"],
        "cases": [asdict(r) for r in evaluation["results"]],
    }


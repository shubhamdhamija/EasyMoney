"""LangGraph version of the bounded research loop, with approval pauses.

    decide -> call_tool (read-only)       -> execute_tool -> decide
    decide -> call_tool (needs approval)  -> request_approval -> await_approval -> decide
    decide -> finish / error              -> finalize_report -> END

The custom ModelDrivenResearchPlanner stays the reference implementation. Both
share the public tool_execution_policy (duplicate protection, tool budget, symbol
pinning, news-item cap), the same ToolRegistry, decision model and report builder.

Write safety: the model only proposes. request_approval validates and prepares the
call, then await_approval pauses with LangGraph's interrupt(). Only a validated,
single-use human approval lets the registry execute the write.
"""

import uuid
from datetime import datetime, timezone
from typing import Any, Literal

from langchain_core.runnables import RunnableConfig
from langgraph.graph import END, START, StateGraph
from langgraph.types import interrupt
from pydantic import ValidationError

from app.langgraph_agent.graph_state import GraphState
from app.models.research_report import ResearchReport
from app.models.research_state import ResearchError, ResearchState, ResearchStep
from app.models.tool_approval import (
    PendingApproval,
    ToolApprovalDecision,
    ToolApprovalRequest,
    approval_reason,
    fingerprint_tool_call,
    project_safe_arguments,
)
from app.models.tool_execution_context import ToolExecutionContext
from app.services.decision_model import DecisionModel
from app.services.portfolio_research_setup import build_portfolio_research_state
from app.services.research_report_builder import build_research_report
from app.services.tool_execution_policy import (
    apply_tool_result,
    evaluate_tool_request,
    finish_error,
    policy_rejection_error,
)
from app.tools.approved_execution import issue_approved_execution_context
from app.tools.tool_registry import ToolRegistry

# Each decision costs at most three graph steps (decide + request_approval +
# await_approval); the extra steps cover the final decide, finalize_report, slack.
_STEPS_PER_DECISION = 3
_RECURSION_SLACK = 4


def recursion_limit_for(maximum_decisions: int) -> int:
    """Graph step ceiling; the application's decision limit normally stops first."""
    return maximum_decisions * _STEPS_PER_DECISION + _RECURSION_SLACK


def thread_config(thread_id: str) -> dict[str, Any]:
    """LangGraph config for one resumable workflow.

    The thread ID is opaque: it carries no secret, permission, or private data
    and is never a substitute for the HTTP request ID.
    """
    return {"configurable": {"thread_id": thread_id}}


def initial_graph_input(
    *,
    symbol: str,
    maximum_tool_calls: int = 5,
    maximum_decisions: int = 8,
    maximum_news_items: int = 3,
) -> dict[str, Any]:
    return {
        "research": ResearchState(
            symbol=symbol.strip().upper(),
            agent_used=True,
            maximum_tool_calls=maximum_tool_calls,
            maximum_decisions=maximum_decisions,
        ),
        "decision": None,
        "report": None,
        "maximum_news_items": maximum_news_items,
        "pending_approval": None,
        "consumed_approval_ids": [],
    }


def initial_portfolio_graph_input(
    *,
    execution_context: ToolExecutionContext,
    maximum_tool_calls: int = 5,
    maximum_decisions: int = 8,
    maximum_news_items: int = 3,
) -> dict[str, Any]:
    """Portfolio research input. The trusted context is application-issued."""
    return {
        "research": build_portfolio_research_state(
            execution_context,
            maximum_tool_calls=maximum_tool_calls,
            maximum_decisions=maximum_decisions,
        ),
        "decision": None,
        "report": None,
        "maximum_news_items": maximum_news_items,
        "pending_approval": None,
        "consumed_approval_ids": [],
        "execution_context": execution_context,
    }


def _fail(research: ResearchState, code: str, message: str) -> None:
    research.error = ResearchError(code=code, message=message)


def _record_policy_rejection(
    research: ResearchState,
    tool_name: str,
    outcome: str,
) -> bool:
    """Record a non-executing policy outcome. True means the call was stopped."""
    if outcome == "skip_duplicate":
        research.steps.append(ResearchStep(tool_name=tool_name, status="skipped"))
        return True

    if outcome == "limit_reached":
        research.steps.append(ResearchStep(tool_name=tool_name, status="failed"))
        _fail(
            research,
            "TOOL_CALL_LIMIT_REACHED",
            "The research workflow reached its tool-call limit.",
        )
        return True

    if outcome == "invalid_arguments":
        research.steps.append(ResearchStep(tool_name=tool_name, status="failed"))
        _fail(research, "INVALID_TOOL_ARGUMENTS", "The tool arguments are invalid.")
        return True

    if outcome in ("consent_required", "portfolio_rejected"):
        research.steps.append(ResearchStep(tool_name=tool_name, status="rejected"))
        rejection = policy_rejection_error(outcome)
        if rejection is not None:
            research.error = rejection
        return True

    return False


def create_research_graph(
    *,
    registry: ToolRegistry,
    decision_model: DecisionModel,
    maximum_news_items: int = 3,
    checkpointer: Any = None,
):
    available_tools = registry.tool_definitions()

    def news_limit(state: GraphState) -> int:
        return state.get("maximum_news_items", maximum_news_items)

    async def decide(state: GraphState) -> dict[str, Any]:
        research = state["research"].model_copy(deep=True)

        if research.decision_count >= research.maximum_decisions:
            _fail(
                research,
                "DECISION_LIMIT_REACHED",
                "The research workflow reached its decision limit.",
            )
            return {"research": research, "decision": None}

        decision = await decision_model.decide(research, available_tools)
        research.decision_count += 1

        if decision.action == "finish":
            research.error = finish_error(research) or research.error

        return {"research": research, "decision": decision}

    async def execute_tool(state: GraphState) -> dict[str, Any]:
        research = state["research"].model_copy(deep=True)
        decision = state["decision"]
        tool_name = decision.tool_name if decision is not None else None

        if decision is None or tool_name is None:
            _fail(research, "INVALID_DECISION", "The decision did not identify a tool.")
            return {"research": research, "decision": None}

        policy = evaluate_tool_request(
            state=research,
            tool_name=tool_name,
            arguments=decision.arguments,
            maximum_news_items=news_limit(state),
        )

        if _record_policy_rejection(research, tool_name, policy.outcome):
            return {"research": research, "decision": None}

        response = await registry.execute(
            tool_name=tool_name,
            arguments=policy.arguments,
            context=state.get("execution_context"),
        )
        research.tool_call_count += 1

        if not response["ok"]:
            research.steps.append(ResearchStep(tool_name=tool_name, status="failed"))
            _fail(research, response["error"]["code"], response["error"]["message"])
            return {"research": research, "decision": None}

        apply_tool_result(
            state=research,
            tool_name=tool_name,
            result=response["result"],
            arguments=policy.arguments,
        )
        research.steps.append(ResearchStep(tool_name=tool_name, status="completed"))
        return {"research": research, "decision": None}

    async def request_approval(state: GraphState, config: RunnableConfig) -> dict[str, Any]:
        """Validate a proposed write, then store it as a pending approval.

        Nothing is executed here.
        """
        research = state["research"].model_copy(deep=True)
        decision = state["decision"]
        tool_name = decision.tool_name if decision is not None else None

        if decision is None or tool_name is None:
            _fail(research, "INVALID_DECISION", "The decision did not identify a tool.")
            return {"research": research, "decision": None}

        policy = evaluate_tool_request(
            state=research,
            tool_name=tool_name,
            arguments=decision.arguments,
            maximum_news_items=news_limit(state),
        )

        if _record_policy_rejection(research, tool_name, policy.outcome):
            return {"research": research, "decision": None}

        prepared = registry.validate_arguments(tool_name, policy.arguments)
        thread_id = (config.get("configurable") or {}).get("thread_id")

        if prepared is None:
            research.steps.append(ResearchStep(tool_name=tool_name, status="failed"))
            _fail(research, "INVALID_TOOL_ARGUMENTS", "The tool arguments are invalid.")
            return {"research": research, "decision": None}

        if not isinstance(thread_id, str) or not thread_id:
            # Without a thread there is nothing to resume, so fail closed.
            research.steps.append(ResearchStep(tool_name=tool_name, status="failed"))
            _fail(
                research,
                "APPROVAL_UNAVAILABLE",
                "Approval-required tools need a resumable workflow.",
            )
            return {"research": research, "decision": None}

        pending = PendingApproval(
            request=ToolApprovalRequest(
                approval_id=f"apr-{uuid.uuid4().hex}",
                thread_id=thread_id,
                tool_name=tool_name,
                safe_arguments=project_safe_arguments(tool_name, prepared),
                reason=approval_reason(tool_name),
                created_at=datetime.now(timezone.utc),
            ),
            prepared_arguments=prepared,
            fingerprint=fingerprint_tool_call(tool_name, prepared),
        )
        return {"research": research, "decision": None, "pending_approval": pending}

    async def await_approval(state: GraphState, config: RunnableConfig) -> dict[str, Any]:
        """Pause for a human, then execute at most once if validly approved."""
        pending = state.get("pending_approval")
        research = state["research"].model_copy(deep=True)

        if pending is None:
            _fail(research, "APPROVAL_NOT_FOUND", "No approval is pending.")
            return {"research": research}

        # Pauses here. On resume, interrupt() returns the human's decision.
        answer = interrupt(pending.request.model_dump(mode="json"))

        request = pending.request
        tool_name = request.tool_name
        consumed = list(state.get("consumed_approval_ids", []))
        thread_id = (config.get("configurable") or {}).get("thread_id")

        def refuse(code: str, message: str) -> dict[str, Any]:
            research.steps.append(ResearchStep(tool_name=tool_name, status="failed"))
            _fail(research, code, message)
            return {"research": research, "pending_approval": None}

        try:
            human = ToolApprovalDecision.model_validate(answer)
        except ValidationError:
            return refuse("APPROVAL_INVALID", "The approval decision is invalid.")

        if request.approval_id in consumed:
            return refuse("APPROVAL_ALREADY_USED", "This approval was already used.")
        if human.approval_id != request.approval_id:
            return refuse("APPROVAL_ID_MISMATCH", "The approval does not match the request.")
        if thread_id != request.thread_id:
            return refuse("APPROVAL_THREAD_MISMATCH", "The approval belongs to another workflow.")
        if pending.fingerprint != fingerprint_tool_call(tool_name, pending.prepared_arguments):
            return refuse("APPROVAL_ARGUMENTS_CHANGED", "The prepared action was modified.")

        # Single use: consumed before anything executes.
        consumed.append(request.approval_id)

        if not human.approved:
            research.steps.append(ResearchStep(tool_name=tool_name, status="rejected"))
            return {
                "research": research,
                "pending_approval": None,
                "consumed_approval_ids": consumed,
            }

        context = issue_approved_execution_context(
            tool_name=tool_name,
            approval_id=request.approval_id,
        )
        response = await registry.execute(
            tool_name=tool_name,
            arguments=pending.prepared_arguments,
            approval=context,
        )
        research.tool_call_count += 1

        if not response["ok"]:
            research.steps.append(ResearchStep(tool_name=tool_name, status="failed"))
            _fail(research, response["error"]["code"], response["error"]["message"])
        else:
            apply_tool_result(state=research, tool_name=tool_name, result=response["result"])
            research.steps.append(ResearchStep(tool_name=tool_name, status="completed"))

        return {
            "research": research,
            "pending_approval": None,
            "consumed_approval_ids": consumed,
        }

    async def finalize_report(state: GraphState) -> dict[str, Any]:
        return {"report": build_research_report(state["research"])}

    def route_after_decide(
        state: GraphState,
    ) -> Literal["execute_tool", "request_approval", "finalize_report"]:
        decision = state.get("decision")
        if state["research"].error is None and decision is not None and decision.action == "call_tool":
            if decision.tool_name is not None and registry.requires_approval(decision.tool_name):
                return "request_approval"
            return "execute_tool"
        return "finalize_report"

    def route_after_execute(state: GraphState) -> Literal["decide", "finalize_report"]:
        return "finalize_report" if state["research"].error is not None else "decide"

    def route_after_request(
        state: GraphState,
    ) -> Literal["await_approval", "decide", "finalize_report"]:
        if state["research"].error is not None:
            return "finalize_report"
        if state.get("pending_approval") is not None:
            return "await_approval"
        return "decide"

    graph = StateGraph(GraphState)
    graph.add_node("decide", decide)
    graph.add_node("execute_tool", execute_tool)
    graph.add_node("request_approval", request_approval)
    graph.add_node("await_approval", await_approval)
    graph.add_node("finalize_report", finalize_report)

    graph.add_edge(START, "decide")
    graph.add_conditional_edges("decide", route_after_decide)
    graph.add_conditional_edges("execute_tool", route_after_execute)
    graph.add_conditional_edges("request_approval", route_after_request)
    graph.add_conditional_edges("await_approval", route_after_execute)
    graph.add_edge("finalize_report", END)

    return graph.compile(checkpointer=checkpointer)


def build_research_graph(**kwargs: Any):
    """Backwards-compatible name for create_research_graph."""
    return create_research_graph(**kwargs)


async def run_research_graph(
    *,
    symbol: str,
    registry: ToolRegistry,
    decision_model: DecisionModel,
    maximum_tool_calls: int = 5,
    maximum_decisions: int = 8,
    maximum_news_items: int = 3,
) -> tuple[ResearchState, ResearchReport]:
    compiled = create_research_graph(
        registry=registry,
        decision_model=decision_model,
        maximum_news_items=maximum_news_items,
    )
    final = await compiled.ainvoke(
        initial_graph_input(
            symbol=symbol,
            maximum_tool_calls=maximum_tool_calls,
            maximum_decisions=maximum_decisions,
            maximum_news_items=maximum_news_items,
        ),
        config={"recursion_limit": recursion_limit_for(maximum_decisions)},
    )
    return final["research"], final["report"]

"""LangGraph version of the bounded research loop.

    decide -> (call_tool) -> execute_tool -> decide
    decide -> (finish / error) -> finalize_report -> END

The custom ModelDrivenResearchPlanner stays the reference implementation. Both
share the public tool_execution_policy (duplicate protection, tool budget, symbol
pinning, news-item cap), the same ToolRegistry, decision model and report builder.
"""

from typing import Any, Literal

from langgraph.graph import END, START, StateGraph

from app.langgraph_agent.graph_state import GraphState
from app.models.research_report import ResearchReport
from app.models.research_state import ResearchError, ResearchState, ResearchStep
from app.services.decision_model import DecisionModel
from app.services.research_report_builder import build_research_report
from app.services.tool_execution_policy import apply_tool_result, evaluate_tool_request
from app.tools.tool_registry import ToolRegistry

# Each decision costs at most two graph steps (decide + execute_tool); the extra
# steps cover the final decide, finalize_report and slack.
_STEPS_PER_DECISION = 2
_RECURSION_SLACK = 4


def build_research_graph(
    *,
    registry: ToolRegistry,
    decision_model: DecisionModel,
    maximum_news_items: int = 3,
):
    available_tools = registry.tool_definitions()

    async def decide(state: GraphState) -> dict[str, Any]:
        research = state["research"].model_copy(deep=True)

        if research.decision_count >= research.maximum_decisions:
            research.error = ResearchError(
                code="DECISION_LIMIT_REACHED",
                message="The research workflow reached its decision limit.",
            )
            return {"research": research, "decision": None}

        decision = await decision_model.decide(research, available_tools)
        research.decision_count += 1

        if decision.action == "finish" and research.quote is None and research.error is None:
            research.error = ResearchError(
                code="QUOTE_NOT_COLLECTED",
                message="The workflow finished without collecting a quote.",
            )

        return {"research": research, "decision": decision}

    async def execute_tool(state: GraphState) -> dict[str, Any]:
        research = state["research"].model_copy(deep=True)
        decision = state["decision"]
        tool_name = decision.tool_name if decision is not None else None

        if decision is None or tool_name is None:
            research.error = ResearchError(
                code="INVALID_DECISION",
                message="The decision did not identify a tool.",
            )
            return {"research": research, "decision": None}

        policy = evaluate_tool_request(
            state=research,
            tool_name=tool_name,
            arguments=decision.arguments,
            maximum_news_items=maximum_news_items,
        )

        if policy.outcome == "skip_duplicate":
            research.steps.append(ResearchStep(tool_name=tool_name, status="skipped"))
            return {"research": research, "decision": None}

        if policy.outcome == "limit_reached":
            research.steps.append(ResearchStep(tool_name=tool_name, status="failed"))
            research.error = ResearchError(
                code="TOOL_CALL_LIMIT_REACHED",
                message="The research workflow reached its tool-call limit.",
            )
            return {"research": research, "decision": None}

        if policy.outcome == "invalid_arguments":
            research.steps.append(ResearchStep(tool_name=tool_name, status="failed"))
            research.error = ResearchError(
                code="INVALID_TOOL_ARGUMENTS",
                message="The tool arguments are invalid.",
            )
            return {"research": research, "decision": None}

        response = await registry.execute(tool_name=tool_name, arguments=policy.arguments)
        research.tool_call_count += 1

        if not response["ok"]:
            research.steps.append(ResearchStep(tool_name=tool_name, status="failed"))
            research.error = ResearchError(
                code=response["error"]["code"],
                message=response["error"]["message"],
            )
            return {"research": research, "decision": None}

        apply_tool_result(
            state=research,
            tool_name=tool_name,
            result=response["result"],
        )
        research.steps.append(ResearchStep(tool_name=tool_name, status="completed"))
        return {"research": research, "decision": None}

    async def finalize_report(state: GraphState) -> dict[str, Any]:
        return {"report": build_research_report(state["research"])}

    def route_after_decide(state: GraphState) -> Literal["execute_tool", "finalize_report"]:
        decision = state.get("decision")
        if state["research"].error is None and decision is not None and decision.action == "call_tool":
            return "execute_tool"
        return "finalize_report"

    def route_after_execute(state: GraphState) -> Literal["decide", "finalize_report"]:
        return "finalize_report" if state["research"].error is not None else "decide"

    graph = StateGraph(GraphState)
    graph.add_node("decide", decide)
    graph.add_node("execute_tool", execute_tool)
    graph.add_node("finalize_report", finalize_report)

    graph.add_edge(START, "decide")
    graph.add_conditional_edges("decide", route_after_decide)
    graph.add_conditional_edges("execute_tool", route_after_execute)
    graph.add_edge("finalize_report", END)

    return graph.compile()


async def run_research_graph(
    *,
    symbol: str,
    registry: ToolRegistry,
    decision_model: DecisionModel,
    maximum_tool_calls: int = 5,
    maximum_decisions: int = 8,
    maximum_news_items: int = 3,
) -> tuple[ResearchState, ResearchReport]:
    compiled = build_research_graph(
        registry=registry,
        decision_model=decision_model,
        maximum_news_items=maximum_news_items,
    )
    initial = ResearchState(
        symbol=symbol.strip().upper(),
        agent_used=True,
        maximum_tool_calls=maximum_tool_calls,
        maximum_decisions=maximum_decisions,
    )
    final = await compiled.ainvoke(
        {"research": initial, "decision": None, "report": None},
        config={
            "recursion_limit": maximum_decisions * _STEPS_PER_DECISION + _RECURSION_SLACK
        },
    )
    return final["research"], final["report"]


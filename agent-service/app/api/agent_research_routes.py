from typing import Any

from fastapi import APIRouter, Body, Depends
from fastapi.responses import JSONResponse
from pydantic import ValidationError

from app.errors.approval_error import invalid_approval
from app.models.agent_research_request import AgentResearchRequest
from app.models.research_report import ResearchReport
from app.models.tool_approval import ToolApprovalDecision
from app.request_context import get_request_id
from app.services.agent_research_service import AgentResearchService
from app.services.default_agent_research_service import (
    create_default_agent_research_service,
)
from app.services.default_graph_agent_service import create_default_graph_agent_service
from app.services.graph_agent_research_service import (
    ApprovalRequiredResult,
    GraphAgentResearchService,
)


router = APIRouter(
    prefix="/agent",
    tags=["agent"],
)


def get_agent_research_service() -> AgentResearchService:
    return create_default_agent_research_service()


def get_agent_approval_service() -> GraphAgentResearchService:
    return create_default_graph_agent_service()


def _approval_required_response(result: ApprovalRequiredResult) -> JSONResponse:
    # An explicit allowlist: no checkpoint state, prompts, or internals.
    approval = result.approval
    return JSONResponse(
        status_code=202,
        content={
            "status": "approval_required",
            "thread_id": result.thread_id,
            "approval": {
                "approval_id": approval.approval_id,
                "tool_name": approval.tool_name,
                "safe_arguments": approval.safe_arguments,
                "reason": approval.reason,
            },
            # The request ID traces this HTTP request; the thread ID identifies
            # the resumable workflow. Neither grants any permission.
            "request_id": get_request_id(),
        },
    )


@router.post(
    "/research",
    responses={202: {"description": "A human approval is required to continue."}},
)
async def agent_research_stock(
    request: AgentResearchRequest,
    service: AgentResearchService = Depends(get_agent_research_service),
) -> Any:
    result = await service.research_stock(
        request.symbol,
        maximum_news_items=request.maximum_news_items,
        maximum_tool_calls=request.maximum_tool_calls,
        maximum_decisions=request.maximum_decisions,
    )

    if isinstance(result, ApprovalRequiredResult):
        return _approval_required_response(result)

    payload = result.model_dump(mode="json")
    if not payload.get("evidence_summary"):
        payload.pop("evidence_summary", None)
    if not payload.get("citations"):
        payload.pop("citations", None)
    if not payload.get("headlines"):
        payload.pop("headlines", None)
    return payload


@router.post(
    "/research/{thread_id}/approval",
    response_model=ResearchReport,
    responses={202: {"description": "Another human approval is required."}},
)
async def submit_agent_approval(
    thread_id: str,
    payload: dict[str, Any] = Body(...),
    service: GraphAgentResearchService = Depends(get_agent_approval_service),
) -> Any:
    try:
        decision = ToolApprovalDecision.model_validate(payload)
    except ValidationError:
        # Never echo the submitted values back.
        raise invalid_approval() from None

    result = await service.resume_with_approval(thread_id, decision)

    if isinstance(result, ApprovalRequiredResult):
        return _approval_required_response(result)

    return result

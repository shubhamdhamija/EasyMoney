"""Read-only portfolio research endpoints.

    POST   /portfolio/local-context                         request-scoped CSV context
    POST   /portfolio/{portfolio_id}/research-consent       grant ~15 minute consent
    DELETE /portfolio/{portfolio_id}/research-consent/{id}  revoke consent
    POST   /agent/portfolio-research                        run read-only research

Every route requires a JWT with portfolio:read. Identity (tenant, user) comes only
from the verified principal. Portfolio IDs, consent IDs and account IDs are
identifiers, never authorization: ownership is re-verified on every request.
There is no trading, transfer or account-change endpoint here.
"""

from __future__ import annotations

import time
from typing import Any

from fastapi import APIRouter, Depends, Request
from pydantic import BaseModel, ConfigDict, Field, ValidationError

from app.auth.auth_dependency import require_auth
from app.auth.auth_models import AuthenticatedPrincipal
from app.errors.portfolio_agent_error import (
    PortfolioAgentError,
    consent_required,
    context_too_large,
    invalid_context,
    portfolio_empty,
    portfolio_not_found,
)
from app.models.portfolio_agent import (
    MAX_LOCAL_PAYLOAD_BYTES,
    LocalPortfolioContext,
    PortfolioContextRequest,
    PortfolioResearchDataCategory,
    ResearchConsentRequest,
    ResearchConsentResponse,
)
from app.models.tool_execution_context import ToolExecutionContext
from app.request_context import get_request_id
from app.services.agent_research_service import AgentResearchService
from app.services.default_portfolio_research_service import create_default_portfolio_research_service
from app.services.portfolio_audit import (
    PORTFOLIO_ACCESS_DENIED,
    PORTFOLIO_RESEARCH_COMPLETED,
    PORTFOLIO_RESEARCH_CONSENT_GRANTED,
    PORTFOLIO_RESEARCH_CONSENT_REVOKED,
    PORTFOLIO_RESEARCH_FAILED,
    PORTFOLIO_RESEARCH_STARTED,
)
from app.services.portfolio_runtime import PortfolioResearchRuntime, get_portfolio_runtime

SCOPE_READ = "portfolio:read"

consent_router = APIRouter(prefix="/portfolio", tags=["portfolio-research"])
research_router = APIRouter(prefix="/agent", tags=["portfolio-research"])

C = PortfolioResearchDataCategory


class PortfolioResearchRequest(BaseModel):
    """No user, tenant, provider, account-number, hash, holdings or secret fields."""

    model_config = ConfigDict(extra="forbid")

    portfolio_context: PortfolioContextRequest
    maximum_news_items: int = Field(default=3, ge=1, le=5)
    maximum_tool_calls: int = Field(default=5, ge=1, le=10)
    maximum_decisions: int = Field(default=8, ge=1, le=20)


def get_portfolio_research_service(
    runtime: PortfolioResearchRuntime = Depends(get_portfolio_runtime),
) -> AgentResearchService:
    return create_default_portfolio_research_service(runtime)


def _with_request_id(body: dict[str, Any]) -> dict[str, Any]:
    return {**body, "request_id": get_request_id()}


@consent_router.post("/local-context")
async def put_local_context(
    request: Request,
    principal: AuthenticatedPrincipal = Depends(require_auth({SCOPE_READ})),
    runtime: PortfolioResearchRuntime = Depends(get_portfolio_runtime),
) -> Any:
    """Stores a normalized, request-scoped CSV context (<= 50 positions, 256 KB) in
    memory for 15 minutes. The raw CSV is never accepted: the strict model rejects
    unknown fields such as file names, URIs, account numbers and hashes."""
    body = await request.body()
    if len(body) > MAX_LOCAL_PAYLOAD_BYTES:
        raise context_too_large()
    try:
        context = LocalPortfolioContext.model_validate_json(body)
    except ValidationError as error:
        if any(item["type"] == "too_long" and "positions" in item["loc"] for item in error.errors()):
            raise context_too_large() from None
        raise invalid_context() from None
    portfolio = runtime.local_store.put(
        tenant_id=principal.tenant_id, user_id=principal.user_id, context=context
    )
    return _with_request_id(
        {
            "portfolio_id": portfolio.portfolio_id,
            "source": portfolio.source.value,
            "snapshot_at": portfolio.snapshot_at.isoformat(),
            "position_count": len(context.positions),
        }
    )


@consent_router.post("/{portfolio_id}/research-consent")
async def grant_research_consent(
    portfolio_id: str,
    payload: dict[str, Any],
    principal: AuthenticatedPrincipal = Depends(require_auth({SCOPE_READ})),
    runtime: PortfolioResearchRuntime = Depends(get_portfolio_runtime),
) -> Any:
    try:
        request = ResearchConsentRequest.model_validate(payload)
    except ValidationError:
        raise invalid_context() from None
    try:
        # Ownership first: another user's portfolio is a non-disclosing 404.
        portfolio = await runtime.repository.get_owned_portfolio(
            tenant_id=principal.tenant_id, user_id=principal.user_id, portfolio_id=portfolio_id
        )
    except PortfolioAgentError as error:
        runtime.audit.record(
            PORTFOLIO_ACCESS_DENIED,
            "denied",
            request_id=get_request_id(),
            tenant_id=principal.tenant_id,
            user_id=principal.user_id,
            safe_error_code=error.code,
        )
        raise
    consent = runtime.consent_store.grant(
        tenant_id=principal.tenant_id,
        user_id=principal.user_id,
        portfolio_id=portfolio_id,
        categories=request.included_data_categories,
        request_id=get_request_id(),
    )
    runtime.audit.record(
        PORTFOLIO_RESEARCH_CONSENT_GRANTED,
        "success",
        request_id=get_request_id(),
        tenant_id=principal.tenant_id,
        user_id=principal.user_id,
        metadata={
            "portfolio_id": portfolio_id,
            "source": portfolio.source.value,
            "categories": [c.value for c in consent.included_data_categories],
        },
    )
    response = ResearchConsentResponse(
        consent_id=consent.consent_id,
        portfolio_id=consent.portfolio_id,
        granted_at=consent.granted_at,
        expires_at=consent.expires_at,
        included_data_categories=consent.included_data_categories,
        request_id=get_request_id(),
    )
    return response.model_dump(mode="json")


@consent_router.delete("/{portfolio_id}/research-consent/{consent_id}")
async def revoke_research_consent(
    portfolio_id: str,
    consent_id: str,
    principal: AuthenticatedPrincipal = Depends(require_auth({SCOPE_READ})),
    runtime: PortfolioResearchRuntime = Depends(get_portfolio_runtime),
) -> Any:
    revoked = runtime.consent_store.revoke(
        tenant_id=principal.tenant_id,
        user_id=principal.user_id,
        portfolio_id=portfolio_id,
        consent_id=consent_id,
    )
    if revoked is None:
        raise portfolio_not_found()
    runtime.audit.record(
        PORTFOLIO_RESEARCH_CONSENT_REVOKED,
        "success",
        request_id=get_request_id(),
        tenant_id=principal.tenant_id,
        user_id=principal.user_id,
        metadata={"portfolio_id": portfolio_id},
    )
    return _with_request_id({"revoked": True})


@research_router.post("/portfolio-research")
async def research_portfolio(
    payload: dict[str, Any],
    principal: AuthenticatedPrincipal = Depends(require_auth({SCOPE_READ})),
    runtime: PortfolioResearchRuntime = Depends(get_portfolio_runtime),
    service: AgentResearchService = Depends(get_portfolio_research_service),
) -> Any:
    try:
        request = PortfolioResearchRequest.model_validate(payload)
    except ValidationError:
        raise invalid_context() from None
    context = request.portfolio_context
    request_id = get_request_id()
    started = time.perf_counter()

    try:
        # 1. Live, owned, unrevoked consent for this exact user + portfolio.
        consent = runtime.consent_store.verify(
            tenant_id=principal.tenant_id,
            user_id=principal.user_id,
            portfolio_id=context.portfolio_id,
            consent_id=context.consent_id,
        )
        # 2. Ownership + source match (non-disclosing 404 otherwise).
        portfolio = await runtime.repository.get_owned_portfolio(
            tenant_id=principal.tenant_id,
            user_id=principal.user_id,
            portfolio_id=context.portfolio_id,
        )
        if portfolio.source != context.source:
            raise portfolio_not_found()
        categories = set(consent.included_data_categories)
        if C.POSITIONS not in categories or C.ACCOUNTS not in categories:
            raise consent_required()
        positions = await runtime.repository.get_owned_positions(
            tenant_id=principal.tenant_id,
            user_id=principal.user_id,
            portfolio_id=context.portfolio_id,
            selected_account_ids=context.selected_account_ids or None,
        )
        if not positions:
            raise portfolio_empty()
    except PortfolioAgentError as error:
        runtime.audit.record(
            PORTFOLIO_ACCESS_DENIED if error.status_code in (403, 404) else PORTFOLIO_RESEARCH_FAILED,
            "denied" if error.status_code in (403, 404) else "error",
            request_id=request_id,
            tenant_id=principal.tenant_id,
            user_id=principal.user_id,
            safe_error_code=error.code,
        )
        raise

    execution_context = ToolExecutionContext(
        authenticated=True,
        tenant_id=principal.tenant_id,
        user_id=principal.user_id,
        request_id=request_id,
        selected_portfolio_id=context.portfolio_id,
        portfolio_consent_id=consent.consent_id,
        portfolio_consent_granted=True,
        selected_account_ids=list(context.selected_account_ids),
        maximum_positions=context.maximum_positions,
        include_positions=context.include_positions,
        include_cash=context.include_cash and C.CASH_BALANCES in categories,
        include_book_value=context.include_book_value and C.BOOK_VALUES in categories,
    )
    runtime.audit.record(
        PORTFOLIO_RESEARCH_STARTED,
        "started",
        request_id=request_id,
        tenant_id=principal.tenant_id,
        user_id=principal.user_id,
        metadata={
            "portfolio_id": context.portfolio_id,
            "source": portfolio.source.value,
            "position_count": len(positions),
        },
    )
    try:
        report = await service.research_portfolio(
            execution_context,
            maximum_news_items=request.maximum_news_items,
            maximum_tool_calls=request.maximum_tool_calls,
            maximum_decisions=request.maximum_decisions,
        )
    except Exception as error:
        runtime.audit.record(
            PORTFOLIO_RESEARCH_FAILED,
            "error",
            request_id=request_id,
            tenant_id=principal.tenant_id,
            user_id=principal.user_id,
            safe_error_code=type(error).__name__,
        )
        raise

    runtime.audit.record(
        PORTFOLIO_RESEARCH_COMPLETED if report.status != "failed" else PORTFOLIO_RESEARCH_FAILED,
        report.status,
        request_id=request_id,
        tenant_id=principal.tenant_id,
        user_id=principal.user_id,
        safe_error_code=report.error_code,
        metadata={
            "portfolio_id": context.portfolio_id,
            "source": portfolio.source.value,
            "duration_ms": round((time.perf_counter() - started) * 1000, 1),
        },
    )
    return _with_request_id(report.model_dump(mode="json"))

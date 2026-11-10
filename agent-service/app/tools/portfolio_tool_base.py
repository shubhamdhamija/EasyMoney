"""Shared, fail-closed authorization for the four read-only portfolio tools.

Identity (tenant, user, selected portfolio, consent) comes ONLY from the
application-owned ToolExecutionContext, never from model arguments. Every tool
call re-verifies the live consent, so expired or revoked consent blocks reads
even in the middle of a workflow.

There are no write operations here and none may be added.
"""

from __future__ import annotations

import time
from typing import Any

from pydantic import BaseModel

from app.errors.portfolio_agent_error import (
    PortfolioAgentError,
    consent_required,
    portfolio_access_denied,
    portfolio_not_found,
)
from app.models.portfolio_agent import PortfolioResearchConsent, PortfolioResearchDataCategory
from app.models.tool_execution_context import ToolExecutionContext
from app.services.portfolio_audit import (
    PORTFOLIO_ACCESS_DENIED,
    PORTFOLIO_TOOL_EXECUTED,
)
from app.services.portfolio_runtime import PortfolioResearchRuntime

_DENIAL_CODES = {
    "PORTFOLIO_NOT_FOUND",
    "PORTFOLIO_ACCESS_DENIED",
    "PORTFOLIO_CONSENT_REQUIRED",
    "PORTFOLIO_CONSENT_EXPIRED",
}

C = PortfolioResearchDataCategory


class AuthorizedAccess:
    def __init__(
        self,
        *,
        tenant_id: str,
        user_id: str,
        portfolio_id: str,
        consent: PortfolioResearchConsent,
        context: ToolExecutionContext,
    ) -> None:
        self.tenant_id = tenant_id
        self.user_id = user_id
        self.portfolio_id = portfolio_id
        self.consent = consent
        categories = set(consent.included_data_categories)
        # Cash and book values degrade gracefully (omitted, never zero-filled).
        self.include_cash = context.include_cash and C.CASH_BALANCES in categories
        self.include_book_value = context.include_book_value and C.BOOK_VALUES in categories
        self.maximum_positions = context.maximum_positions
        self._scope = list(context.selected_account_ids)

    def accounts(self, requested: list[str] | None) -> list[str] | None:
        """Request-selected accounts bound the model's selection; they never widen it."""
        if not self._scope:
            return requested
        if requested is None:
            return list(self._scope)
        if any(account_id not in self._scope for account_id in requested):
            raise portfolio_not_found()
        return requested


class PortfolioToolBase:
    requires_approval = False
    requires_trusted_context = True
    name: str
    input_model: type[BaseModel]
    description: str

    def __init__(self, runtime: PortfolioResearchRuntime) -> None:
        self._runtime = runtime
        self.calls: list[Any] = []

    def _authorize(self, context: ToolExecutionContext | None, portfolio_id: str) -> AuthorizedAccess:
        if (
            not isinstance(context, ToolExecutionContext)
            or not context.authenticated
            or not context.tenant_id
            or not context.user_id
            or not context.selected_portfolio_id
            or not context.portfolio_consent_granted
        ):
            raise consent_required()
        if portfolio_id != context.selected_portfolio_id:
            raise portfolio_access_denied()
        if not context.include_positions:
            raise consent_required()
        store = self._runtime.consent_store
        store.verify(
            tenant_id=context.tenant_id,
            user_id=context.user_id,
            portfolio_id=portfolio_id,
            consent_id=context.portfolio_consent_id,
            category=C.ACCOUNTS,
        )
        consent = store.verify(
            tenant_id=context.tenant_id,
            user_id=context.user_id,
            portfolio_id=portfolio_id,
            consent_id=context.portfolio_consent_id,
            category=C.POSITIONS,
        )
        return AuthorizedAccess(
            tenant_id=context.tenant_id,
            user_id=context.user_id,
            portfolio_id=portfolio_id,
            consent=consent,
            context=context,
        )

    async def execute(self, tool_input: Any, context: ToolExecutionContext | None = None) -> BaseModel:
        self.calls.append(tool_input)
        started = time.perf_counter()
        audit = self._runtime.audit
        request_id = getattr(context, "request_id", None)
        tenant_id = getattr(context, "tenant_id", None)
        user_id = getattr(context, "user_id", None)
        try:
            access = self._authorize(context, tool_input.portfolio_id)
            result, meta = await self._run(tool_input, access)
        except PortfolioAgentError as error:
            audit.record(
                PORTFOLIO_ACCESS_DENIED if error.code in _DENIAL_CODES else PORTFOLIO_TOOL_EXECUTED,
                "denied" if error.code in _DENIAL_CODES else "error",
                request_id=request_id,
                tenant_id=tenant_id,
                user_id=user_id,
                tool_name=self.name,
                safe_error_code=error.code,
                metadata={"duration_ms": round((time.perf_counter() - started) * 1000, 1)},
            )
            raise
        audit.record(
            PORTFOLIO_TOOL_EXECUTED,
            "success",
            request_id=request_id,
            tenant_id=tenant_id,
            user_id=user_id,
            tool_name=self.name,
            metadata={
                **meta,
                "portfolio_id": access.portfolio_id,
                "duration_ms": round((time.perf_counter() - started) * 1000, 1),
            },
        )
        return result

    async def _run(self, tool_input: Any, access: AuthorizedAccess) -> tuple[BaseModel, dict[str, Any]]:
        raise NotImplementedError

    async def _read_cash(self, access: AuthorizedAccess, accounts: list[str] | None):
        if not access.include_cash:
            return None
        return await self._runtime.repository.get_owned_cash_balances(
            tenant_id=access.tenant_id,
            user_id=access.user_id,
            portfolio_id=access.portfolio_id,
            selected_account_ids=accounts,
        )

    async def _read_positions(self, access: AuthorizedAccess, accounts: list[str] | None):
        return await self._runtime.repository.get_owned_positions(
            tenant_id=access.tenant_id,
            user_id=access.user_id,
            portfolio_id=access.portfolio_id,
            selected_account_ids=accounts,
        )

    async def _read_accounts(self, access: AuthorizedAccess, accounts: list[str] | None):
        return await self._runtime.repository.get_owned_accounts(
            tenant_id=access.tenant_id,
            user_id=access.user_id,
            portfolio_id=access.portfolio_id,
            selected_account_ids=accounts,
        )

    async def _read_portfolio(self, access: AuthorizedAccess):
        return await self._runtime.repository.get_owned_portfolio(
            tenant_id=access.tenant_id, user_id=access.user_id, portfolio_id=access.portfolio_id
        )

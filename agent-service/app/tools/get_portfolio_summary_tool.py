from __future__ import annotations

from typing import Any

from app.models.portfolio_agent import GetPortfolioSummaryInput, PortfolioSummaryResult
from app.services.portfolio_analysis_service import (
    build_account_summaries,
    build_currency_summaries,
    summary_warnings,
)
from app.tools.portfolio_tool_base import AuthorizedAccess, PortfolioToolBase


class GetPortfolioSummaryTool(PortfolioToolBase):
    name = "get_portfolio_summary"
    input_model = GetPortfolioSummaryInput
    requires_approval = False

    description = (
        "Reads the authenticated user's selected portfolio snapshot and returns bounded account "
        "and per-currency summaries with the data source and snapshot time. Read-only: it does "
        "not place trades, does not modify brokerage accounts, does not combine currencies "
        "without FX rates, and does not provide investment advice."
    )

    async def _run(self, tool_input: GetPortfolioSummaryInput, access: AuthorizedAccess) -> tuple[Any, dict[str, Any]]:
        accounts_filter = access.accounts(tool_input.selected_account_ids)
        portfolio = await self._read_portfolio(access)
        accounts = await self._read_accounts(access, accounts_filter)
        positions = await self._read_positions(access, accounts_filter)
        include_book = tool_input.include_book_value and access.include_book_value
        cash = await self._read_cash(access, accounts_filter) if tool_input.include_cash else None

        currency_summaries = build_currency_summaries(positions, cash, include_book_value=include_book)
        result = PortfolioSummaryResult(
            portfolio_id=portfolio.portfolio_id,
            source=portfolio.source,
            snapshot_at=portfolio.snapshot_at,
            last_synced_at=portfolio.last_synced_at,
            accounts=build_account_summaries(accounts, positions, cash or []),
            currency_summaries=currency_summaries,
            warnings=summary_warnings(portfolio, positions, cash, include_book_value=include_book),
        )
        return result, {
            "source": portfolio.source.value,
            "snapshot_at": portfolio.snapshot_at.isoformat(),
            "position_count": len(positions),
            "currency_codes": [c.currency for c in currency_summaries],
        }

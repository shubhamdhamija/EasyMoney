from __future__ import annotations

from typing import Any

from app.models.portfolio_agent import AnalyzePortfolioConcentrationInput
from app.services.portfolio_analysis_service import analyze_concentration
from app.tools.portfolio_tool_base import AuthorizedAccess, PortfolioToolBase


class AnalyzePortfolioConcentrationTool(PortfolioToolBase):
    name = "analyze_portfolio_concentration"
    input_model = AnalyzePortfolioConcentrationInput
    requires_approval = False

    description = (
        "Computes deterministic, currency-specific concentration for the authenticated user's "
        "selected portfolio snapshot: largest position, top-position share, account and "
        "security-type allocation, and cash allocation. One explicit currency is required; "
        "currencies are never combined without FX rates. Read-only: it places no trades, modifies "
        "no accounts, assigns no risk labels, and provides no investment advice."
    )

    async def _run(self, tool_input: AnalyzePortfolioConcentrationInput, access: AuthorizedAccess) -> tuple[Any, dict[str, Any]]:
        accounts_filter = access.accounts(tool_input.selected_account_ids)
        portfolio = await self._read_portfolio(access)
        accounts = await self._read_accounts(access, accounts_filter)
        positions = await self._read_positions(access, accounts_filter)
        cash = await self._read_cash(access, accounts_filter)

        result = analyze_concentration(
            portfolio,
            accounts,
            positions,
            cash,
            currency=tool_input.currency,
            maximum_top_positions=tool_input.maximum_top_positions,
        )
        return result, {
            "source": portfolio.source.value,
            "snapshot_at": portfolio.snapshot_at.isoformat(),
            "position_count": len([p for p in positions if p.currency == tool_input.currency]),
            "currency_codes": [tool_input.currency],
        }

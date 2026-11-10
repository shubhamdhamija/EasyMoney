from __future__ import annotations

from collections import defaultdict
from typing import Any

from app.models.portfolio_agent import (
    GetPortfolioPositionInput,
    PortfolioPosition,
    PortfolioPositionResult,
    PortfolioSecurityMatch,
    PortfolioWarning,
    PortfolioWarningCode,
)
from app.services.portfolio_analysis_service import position_summary, sort_positions
from app.models.portfolio_agent import PortfolioSortBy
from app.tools.portfolio_tool_base import AuthorizedAccess, PortfolioToolBase


class GetPortfolioPositionTool(PortfolioToolBase):
    name = "get_portfolio_position"
    input_model = GetPortfolioPositionInput
    requires_approval = False

    description = (
        "Reads the authenticated user's selected portfolio snapshot and returns every owned "
        "position for one ticker symbol, grouped by normalized security identity so different "
        "securities that share a ticker are never merged. Read-only: it accepts no provider "
        "security IDs or account numbers, places no trades, modifies no accounts, and provides "
        "no investment advice."
    )

    async def _run(self, tool_input: GetPortfolioPositionInput, access: AuthorizedAccess) -> tuple[Any, dict[str, Any]]:
        accounts_filter = access.accounts(tool_input.selected_account_ids)
        portfolio = await self._read_portfolio(access)
        positions = await self._read_positions(access, accounts_filter)

        matches = [p for p in positions if (p.symbol or "").upper() == tool_input.symbol]
        groups: dict[str, list[PortfolioPosition]] = defaultdict(list)
        for position in matches:
            groups[position.security_key].append(position)

        securities: list[PortfolioSecurityMatch] = []
        for key in sorted(groups):
            ordered = sort_positions(groups[key], PortfolioSortBy.MARKET_VALUE_DESC)
            securities.append(
                PortfolioSecurityMatch(
                    security_key=key,
                    security_name=ordered[0].security_name,
                    currency=ordered[0].currency,
                    positions=[
                        position_summary(
                            p, portfolio.snapshot_at, include_book_value=access.include_book_value
                        )
                        for p in ordered
                    ],
                )
            )

        warnings: list[PortfolioWarning] = []
        if len(securities) > 1:
            warnings.append(
                PortfolioWarning(code=PortfolioWarningCode.AMBIGUOUS_SYMBOL, count=len(securities))
            )
        result = PortfolioPositionResult(
            portfolio_id=portfolio.portfolio_id,
            source=portfolio.source,
            snapshot_at=portfolio.snapshot_at,
            symbol=tool_input.symbol,
            securities=securities,
            warnings=warnings,
        )
        return result, {
            "source": portfolio.source.value,
            "snapshot_at": portfolio.snapshot_at.isoformat(),
            "position_count": len(matches),
            "currency_codes": sorted({p.currency for p in matches}),
        }

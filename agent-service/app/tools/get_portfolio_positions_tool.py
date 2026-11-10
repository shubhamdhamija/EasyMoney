from __future__ import annotations

from typing import Any

from app.errors.portfolio_agent_error import portfolio_empty
from app.models.portfolio_agent import (
    GetPortfolioPositionsInput,
    PortfolioPositionsResult,
    PortfolioWarning,
    PortfolioWarningCode,
)
from app.services.portfolio_analysis_service import (
    filter_positions,
    position_summary,
    sort_positions,
    summary_warnings,
)
from app.tools.portfolio_tool_base import AuthorizedAccess, PortfolioToolBase


class GetPortfolioPositionsTool(PortfolioToolBase):
    name = "get_portfolio_positions"
    input_model = GetPortfolioPositionsInput
    requires_approval = False

    description = (
        "Reads the authenticated user's selected portfolio snapshot and returns a bounded, sorted "
        "list of positions (at most 50), optionally filtered by currency or security type. "
        "Read-only: it never exposes account numbers, places trades, modifies accounts, combines "
        "currencies without FX rates, or provides investment advice."
    )

    async def _run(self, tool_input: GetPortfolioPositionsInput, access: AuthorizedAccess) -> tuple[Any, dict[str, Any]]:
        accounts_filter = access.accounts(tool_input.selected_account_ids)
        portfolio = await self._read_portfolio(access)
        all_positions = await self._read_positions(access, accounts_filter)
        if not all_positions:
            raise portfolio_empty()

        matching = filter_positions(
            all_positions, currency=tool_input.currency, security_type=tool_input.security_type
        )
        ordered = sort_positions(matching, tool_input.sort_by)
        limit = min(tool_input.maximum_positions, access.maximum_positions)
        returned = ordered[:limit]
        truncated = len(matching) > len(returned)

        warnings = summary_warnings(
            portfolio, matching, None, include_book_value=access.include_book_value
        )
        if truncated:
            warnings.append(
                PortfolioWarning(
                    code=PortfolioWarningCode.PARTIAL_POSITION_LIST,
                    count=len(matching) - len(returned),
                )
            )
        result = PortfolioPositionsResult(
            portfolio_id=portfolio.portfolio_id,
            source=portfolio.source,
            snapshot_at=portfolio.snapshot_at,
            positions=[
                position_summary(p, portfolio.snapshot_at, include_book_value=access.include_book_value)
                for p in returned
            ],
            total_matching_positions=len(matching),
            returned_position_count=len(returned),
            truncated=truncated,
            warnings=warnings,
        )
        return result, {
            "source": portfolio.source.value,
            "snapshot_at": portfolio.snapshot_at.isoformat(),
            "position_count": len(returned),
            "currency_codes": sorted({p.currency for p in returned}),
        }

from typing import Any

from app.models.research_state import (
    ResearchError,
    ResearchState,
    ResearchStep,
)
from app.tools.tool_registry import ToolRegistry


def should_fetch_news(
    price_change_percent: float,
    *,
    movement_threshold: float = 1.0,
) -> bool:
    return abs(price_change_percent) >= movement_threshold


def can_call_tool(state: ResearchState) -> bool:
    return state.tool_call_count < state.maximum_tool_calls


def apply_tool_limit_error(state: ResearchState, tool_name: str) -> ResearchState:
    state.steps.append(
        ResearchStep(
            tool_name=tool_name,
            status="failed",
        )
    )
    state.error = ResearchError(
        code="TOOL_CALL_LIMIT_REACHED",
        message=(
            "The research workflow reached "
            "its tool-call limit."
        ),
    )
    return state


class DeterministicResearchPlanner:
    def __init__(self, registry: ToolRegistry) -> None:
        self._registry = registry

    async def research_stock(
        self,
        symbol: str,
        *,
        include_news: bool | None = None,
        maximum_news_items: int = 3,
        maximum_tool_calls: int = 5,
    ) -> ResearchState:
        normalized_symbol = symbol.strip().upper()
        state = ResearchState(
            symbol=normalized_symbol,
            maximum_tool_calls=maximum_tool_calls,
        )

        if not can_call_tool(state):
            return apply_tool_limit_error(state, "get_stock_quote")

        quote_response = await self._registry.execute(
            tool_name="get_stock_quote",
            arguments={"symbol": symbol},
        )
        state.tool_call_count += 1

        if not quote_response["ok"]:
            state.steps.append(
                ResearchStep(
                    tool_name="get_stock_quote",
                    status="failed",
                )
            )
            error = quote_response["error"]
            state.error = ResearchError(
                code=error["code"],
                message=error["message"],
            )
            return state

        state.steps.append(
            ResearchStep(
                tool_name="get_stock_quote",
                status="completed",
            )
        )
        state.quote = quote_response["result"]

        if state.quote is None:
            raise RuntimeError(
                "Quote state was not populated after successful execution."
            )

        quote = state.quote

        if include_news is None:
            news_required = should_fetch_news(quote["price_change_percent"])
        else:
            news_required = include_news

        if not news_required:
            state.steps.append(
                ResearchStep(
                    tool_name="get_recent_news",
                    status="skipped",
                )
            )
            return state

        if not can_call_tool(state):
            return apply_tool_limit_error(state, "get_recent_news")

        news_response = await self._registry.execute(
            tool_name="get_recent_news",
            arguments={"symbol": state.symbol, "maximum_items": maximum_news_items},
        )
        state.tool_call_count += 1

        if not news_response["ok"]:
            state.steps.append(
                ResearchStep(
                    tool_name="get_recent_news",
                    status="failed",
                )
            )
            error = news_response["error"]
            state.error = ResearchError(
                code=error["code"],
                message=error["message"],
            )
            state.news = None
            return state

        state.steps.append(
            ResearchStep(
                tool_name="get_recent_news",
                status="completed",
            )
        )
        state.news = news_response["result"]
        return state

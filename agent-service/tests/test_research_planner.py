import pytest

from app.services.research_planner import DeterministicResearchPlanner, should_fetch_news
from app.tools.default_registry import create_default_registry
from app.tools.fake_recent_news_tool import FakeRecentNewsTool
from app.tools.fake_stock_quote_tool import FakeStockQuoteTool
from app.tools.tool_registry import ToolRegistry


def create_planner() -> DeterministicResearchPlanner:
    registry = create_default_registry()
    return DeterministicResearchPlanner(registry=registry)


@pytest.mark.asyncio
async def test_researches_stock_using_quote_tool() -> None:
    planner = create_planner()

    state = await planner.research_stock(" aapl ")

    assert state.symbol == "AAPL"
    assert state.agent_used is False
    assert state.error is None
    assert state.quote is not None
    assert state.quote["current_price"] == 228.30
    assert state.tool_call_count == 2
    assert state.steps[0].tool_name == "get_stock_quote"
    assert state.steps[0].status == "completed"
    assert state.steps[1].tool_name == "get_recent_news"
    assert state.steps[1].status == "completed"


@pytest.mark.asyncio
async def test_returns_safe_error_when_quote_is_missing() -> None:
    planner = create_planner()

    state = await planner.research_stock("UNKNOWN")

    assert state.symbol == "UNKNOWN"
    assert state.quote is None
    assert state.agent_used is False
    assert state.error is not None
    assert state.error.code == "QUOTE_NOT_FOUND"
    assert state.tool_call_count == 1
    assert state.steps[0].tool_name == "get_stock_quote"
    assert state.steps[0].status == "failed"


@pytest.mark.asyncio
async def test_invalid_symbol_never_reaches_tool() -> None:
    planner = create_planner()

    result = await planner.research_stock("AAPL; DELETE")

    assert result["ok"] is False
    assert result["error"]["code"] == "INVALID_TOOL_ARGUMENTS"


@pytest.mark.asyncio
async def test_quote_only_research_skips_news() -> None:
    planner = create_planner()

    result = await planner.research_stock("AAPL", include_news=False)

    assert result["ok"] is True
    assert result["symbol"] == "AAPL"
    assert result["quote"] is not None
    assert result["news"] is None
    assert result["steps"] == [
        {"tool_name": "get_stock_quote", "status": "completed"},
        {"tool_name": "get_recent_news", "status": "skipped"},
    ]
    assert result["agent_used"] is False


@pytest.mark.asyncio
async def test_research_includes_news_when_requested() -> None:
    planner = create_planner()

    result = await planner.research_stock("AAPL", include_news=True, maximum_news_items=2)

    assert result["ok"] is True
    assert result["symbol"] == "AAPL"
    assert result["quote"] is not None
    assert result["quote"]["symbol"] == "AAPL"
    assert result["news"] is not None
    assert result["news"]["symbol"] == "AAPL"
    assert len(result["news"]["articles"]) == 2
    assert result["steps"] == [
        {"tool_name": "get_stock_quote", "status": "completed"},
        {"tool_name": "get_recent_news", "status": "completed"},
    ]


@pytest.mark.asyncio
async def test_news_failure_preserves_successful_quote() -> None:
    registry = ToolRegistry()
    quote_tool = FakeStockQuoteTool()
    news_tool = FakeRecentNewsTool()
    registry.register(quote_tool)
    registry.register(news_tool)

    planner = DeterministicResearchPlanner(registry=registry)
    result = await planner.research_stock("SHOP.TO", include_news=True)

    assert result["ok"] is False
    assert result["quote"] is not None
    assert result["quote"]["symbol"] == "SHOP.TO"
    assert result["news"] is None
    assert result["error"]["code"] == "NEWS_NOT_FOUND"
    assert result["steps"] == [
        {"tool_name": "get_stock_quote", "status": "completed"},
        {"tool_name": "get_recent_news", "status": "failed"},
    ]


def test_large_positive_move_requires_news() -> None:
    assert should_fetch_news(2.4) is True


def test_large_negative_move_requires_news() -> None:
    assert should_fetch_news(-1.25) is True


def test_small_move_does_not_require_news() -> None:
    assert should_fetch_news(0.8) is False


def test_threshold_value_requires_news() -> None:
    assert should_fetch_news(1.0) is True


@pytest.mark.asyncio
async def test_automatic_plan_fetches_news_for_large_move() -> None:
    planner = create_planner()

    result = await planner.research_stock("AAPL", include_news=None, maximum_news_items=2)

    assert result["ok"] is True
    assert result["quote"] is not None
    assert result["news"] is not None
    assert len(result["news"]["articles"]) == 2
    assert result["steps"] == [
        {"tool_name": "get_stock_quote", "status": "completed"},
        {"tool_name": "get_recent_news", "status": "completed"},
    ]


@pytest.mark.asyncio
async def test_automatic_plan_skips_news_for_small_move() -> None:
    planner = create_planner()

    result = await planner.research_stock("MSFT", include_news=None)

    assert result["ok"] is True
    assert result["quote"] is not None
    assert result["news"] is None
    assert result["steps"] == [
        {"tool_name": "get_stock_quote", "status": "completed"},
        {"tool_name": "get_recent_news", "status": "skipped"},
    ]


@pytest.mark.asyncio
async def test_explicit_false_overrides_automatic_decision() -> None:
    planner = create_planner()

    result = await planner.research_stock("AAPL", include_news=False)

    assert result["ok"] is True
    assert result["news"] is None
    assert result["steps"][1]["status"] == "skipped"


@pytest.mark.asyncio
async def test_explicit_true_overrides_automatic_decision() -> None:
    planner = create_planner()

    result = await planner.research_stock("MSFT", include_news=True)

    assert result["ok"] is True
    assert result["news"] is not None
    assert result["steps"][1]["status"] == "completed"


@pytest.mark.asyncio
async def test_one_tool_call_allows_quote_only_plan() -> None:
    planner = create_planner()

    state = await planner.research_stock(
        "AAPL",
        include_news=False,
        maximum_tool_calls=1,
    )

    assert state.error is None
    assert state.quote is not None
    assert state.news is None
    assert state.tool_call_count == 1


@pytest.mark.asyncio
async def test_tool_limit_stops_news_call() -> None:
    planner = create_planner()

    state = await planner.research_stock(
        "AAPL",
        include_news=True,
        maximum_tool_calls=1,
    )

    assert state.quote is not None
    assert state.news is None
    assert state.tool_call_count == 1
    assert state.error is not None
    assert state.error.code == "TOOL_CALL_LIMIT_REACHED"
    assert state.steps[-1].tool_name == "get_recent_news"
    assert state.steps[-1].status == "failed"


@pytest.mark.asyncio
async def test_two_tool_calls_allow_quote_and_news() -> None:
    planner = create_planner()

    state = await planner.research_stock(
        "AAPL",
        include_news=True,
        maximum_tool_calls=2,
    )

    assert state.error is None
    assert state.quote is not None
    assert state.news is not None
    assert state.tool_call_count == 2
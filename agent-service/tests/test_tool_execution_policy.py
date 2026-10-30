from app.models.research_state import ResearchState
from app.services.tool_execution_policy import evaluate_tool_request


def make_state(**overrides) -> ResearchState:
    return ResearchState(symbol="AAPL", **overrides)


def evaluate(state, tool_name, arguments, maximum_news_items=3):
    return evaluate_tool_request(
        state=state,
        tool_name=tool_name,
        arguments=arguments,
        maximum_news_items=maximum_news_items,
    )


def test_matching_symbol_is_allowed():
    result = evaluate(make_state(), "get_stock_quote", {"symbol": "aapl"})

    assert result.should_execute
    assert result.outcome == "execute"


def test_other_symbol_is_rejected():
    result = evaluate(make_state(), "get_stock_quote", {"symbol": "TSLA"})

    assert not result.should_execute
    assert result.outcome == "invalid_arguments"


def test_duplicate_quote_is_skipped():
    state = make_state(quote={"symbol": "AAPL"})

    result = evaluate(state, "get_stock_quote", {"symbol": "AAPL"})

    assert result.outcome == "skip_duplicate"


def test_duplicate_news_is_skipped():
    state = make_state(news={"items": []})

    result = evaluate(state, "get_recent_news", {"symbol": "AAPL"})

    assert result.outcome == "skip_duplicate"


def test_news_maximum_is_capped():
    result = evaluate(
        make_state(),
        "get_recent_news",
        {"symbol": "AAPL", "maximum_items": 50},
        maximum_news_items=2,
    )

    assert result.should_execute
    assert result.arguments["maximum_items"] == 2


def test_missing_news_maximum_uses_cap():
    result = evaluate(
        make_state(), "get_recent_news", {"symbol": "AAPL"}, maximum_news_items=2
    )

    assert result.arguments["maximum_items"] == 2


def test_unknown_tool_passes_through_to_registry():
    result = evaluate(make_state(), "delete_database", {})

    assert result.should_execute


def test_tool_limit_is_reported():
    state = make_state(maximum_tool_calls=1, tool_call_count=1)

    result = evaluate(state, "get_stock_quote", {"symbol": "AAPL"})

    assert result.outcome == "limit_reached"


def test_input_arguments_are_not_mutated():
    arguments = {"symbol": "AAPL", "maximum_items": 50}

    evaluate(make_state(), "get_recent_news", arguments, maximum_news_items=2)

    assert arguments == {"symbol": "AAPL", "maximum_items": 50}


from app.tools.default_registry import create_default_registry
from app.tools.fake_stock_quote_tool import FakeStockQuoteTool
from app.tools.tool_registry import ToolRegistry


def test_tool_definitions_contain_both_approved_tools() -> None:
    definitions = create_default_registry().tool_definitions()

    assert sorted(d.name for d in definitions) == [
        "get_recent_news",
        "get_stock_quote",
    ]


def test_tool_definitions_include_input_schemas() -> None:
    by_name = {d.name: d for d in create_default_registry().tool_definitions()}

    quote_schema = by_name["get_stock_quote"].input_schema
    news_schema = by_name["get_recent_news"].input_schema

    assert "symbol" in quote_schema["properties"]
    assert "symbol" in news_schema["properties"]
    assert "maximum_items" in news_schema["properties"]


def test_tool_descriptions_are_not_blank() -> None:
    for definition in create_default_registry().tool_definitions():
        assert definition.description.strip()


def test_unregistered_tools_are_absent() -> None:
    registry = ToolRegistry()
    registry.register(FakeStockQuoteTool())

    names = [d.name for d in registry.tool_definitions()]

    assert names == ["get_stock_quote"]
    assert "get_recent_news" not in names
    assert "delete_database" not in names


def test_empty_registry_exposes_nothing() -> None:
    assert ToolRegistry().tool_definitions() == []


def test_definitions_expose_only_name_description_and_schema() -> None:
    for definition in create_default_registry().tool_definitions():
        assert set(definition.model_dump()) == {"name", "description", "input_schema"}


def test_registry_remains_the_authoritative_allowlist() -> None:
    registry = create_default_registry()

    advertised = {d.name for d in registry.tool_definitions()}

    assert advertised == set(registry.available_tool_names())
    assert all(registry.contains(name) for name in advertised)
    assert not registry.contains("delete_database")


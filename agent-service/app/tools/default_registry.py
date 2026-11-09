from app.tools.fake_recent_news_tool import FakeRecentNewsTool
from app.tools.fake_research_report_store import FakeResearchReportStore
from app.tools.fake_save_research_report_tool import FakeSaveResearchReportTool
from app.tools.fake_stock_quote_tool import FakeStockQuoteTool
from app.tools.search_financial_documents_tool import SearchFinancialDocumentsTool
from app.tools.tool_registry import ToolRegistry


def create_default_registry(
    *,
    include_write_tools: bool = False,
    report_store: FakeResearchReportStore | None = None,
    document_search_tool: SearchFinancialDocumentsTool | None = None,
    quote_tool=None,
    news_tool=None,
) -> ToolRegistry:
    """Read-only by default. Write tools must be requested explicitly.

    quote_tool / news_tool default to the fixed fake tools; pass live tools to use real data.
    """
    registry = ToolRegistry()
    registry.register(quote_tool if quote_tool is not None else FakeStockQuoteTool())
    registry.register(news_tool if news_tool is not None else FakeRecentNewsTool())
    if document_search_tool is not None:
        registry.register(document_search_tool)

    if include_write_tools:
        registry.register(FakeSaveResearchReportTool(report_store))

    return registry


def create_configured_registry(settings=None) -> ToolRegistry:
    """Read-only registry using live market data when AGENT_LIVE_MARKET_DATA=true."""
    from app.config.settings import get_settings

    resolved = settings if settings is not None else get_settings()
    if resolved.live_market_data_enabled:
        from app.tools.live_market_tools import LiveRecentNewsTool, LiveStockQuoteTool

        return create_default_registry(
            quote_tool=LiveStockQuoteTool(),
            news_tool=LiveRecentNewsTool(),
        )
    return create_default_registry()


def create_approval_registry(
    report_store: FakeResearchReportStore | None = None,
    document_search_tool: SearchFinancialDocumentsTool | None = None,
) -> ToolRegistry:
    """Read-only tools plus the fake approval-required write tool."""
    return create_default_registry(
        include_write_tools=True,
        report_store=report_store,
        document_search_tool=document_search_tool,
    )


def create_research_registry(
    *,
    quote_tool,
    news_tool,
    document_search_tool,
    include_write_tools: bool = False,
) -> ToolRegistry:
    registry = ToolRegistry()
    registry.register(quote_tool)
    registry.register(news_tool)
    registry.register(document_search_tool)
    if include_write_tools:
        registry.register(FakeSaveResearchReportTool())
    return registry

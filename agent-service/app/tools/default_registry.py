from app.tools.fake_recent_news_tool import FakeRecentNewsTool
from app.tools.fake_stock_quote_tool import FakeStockQuoteTool
from app.tools.tool_registry import ToolRegistry


def create_default_registry() -> ToolRegistry:
    registry = ToolRegistry()
    registry.register(FakeStockQuoteTool())
    registry.register(FakeRecentNewsTool())
    return registry


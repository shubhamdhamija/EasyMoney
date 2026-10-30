from fastapi.testclient import TestClient

from app.main import app


client = TestClient(app)


def test_health_endpoint() -> None:
    response = client.get("/health")

    assert response.status_code == 200
    assert response.json() == {
        "status": "healthy",
        "service": "easymoney-agent-service",
    }


def test_research_endpoint_returns_quote_only_report() -> None:
    response = client.post(
        "/research",
        json={
            "symbol": " aapl ",
            "include_news": False,
        },
    )

    assert response.status_code == 200

    body = response.json()
    assert body["symbol"] == "AAPL"
    assert body["status"] == "completed"
    assert body["quote"] is not None
    assert body["news_count"] == 0
    assert body["tools_used"] == ["get_stock_quote"]
    assert body["agent_used"] is False


def test_research_endpoint_returns_news() -> None:
    response = client.post(
        "/research",
        json={
            "symbol": "AAPL",
            "include_news": True,
            "maximum_news_items": 2,
            "maximum_tool_calls": 2,
        },
    )

    assert response.status_code == 200

    body = response.json()
    assert body["status"] == "completed"
    assert body["news_count"] == 2
    assert body["tools_used"] == [
        "get_stock_quote",
        "get_recent_news",
    ]


def test_research_endpoint_rejects_invalid_symbol() -> None:
    response = client.post(
        "/research",
        json={
            "symbol": "AAPL; DELETE",
        },
    )

    assert response.status_code == 422


def test_research_endpoint_honors_tool_limit() -> None:
    response = client.post(
        "/research",
        json={
            "symbol": "AAPL",
            "include_news": True,
            "maximum_tool_calls": 1,
        },
    )

    assert response.status_code == 200

    body = response.json()
    assert body["status"] == "partial"
    assert body["error_code"] == "TOOL_CALL_LIMIT_REACHED"
    assert body["quote"] is not None
    assert body["news_count"] == 0


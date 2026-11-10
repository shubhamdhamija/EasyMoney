"""Read-only portfolio research: consent, ownership, tools, report, no trading."""

from datetime import UTC, datetime, timedelta

import pytest
from fastapi.testclient import TestClient

from app.api.portfolio_research_routes import get_portfolio_research_service
from app.main import app
from app.errors.portfolio_agent_error import portfolio_not_found
from app.models.planner_decision import PlannerDecision
from app.models.portfolio_agent import PORTFOLIO_REPORT_STATEMENT
from app.services.agent_research_service import AgentResearchService
from app.services.fake_decision_model import FakeDecisionModel
from app.services.model_driven_research_planner import ModelDrivenResearchPlanner
from app.services.portfolio_audit import InMemoryAuditSink, PortfolioAudit
from app.services.portfolio_consent_service import PortfolioConsentStore
from app.services.portfolio_runtime import build_runtime, get_portfolio_runtime
from app.tools.default_registry import create_portfolio_research_registry
from tests.test_portfolio_connections import token

ALL = ["ACCOUNTS", "POSITIONS", "CASH_BALANCES", "BOOK_VALUES"]
SENSITIVE_KEYS = {"tenant_id", "user_id", "consent_id", "account_number", "account_hash", "connection_id"}


class _NoSnapTrade:
    async def _nf(self, **_):
        raise portfolio_not_found()

    get_owned_portfolio = get_owned_accounts = get_owned_positions = get_owned_cash_balances = _nf


def csv_context(positions=None) -> dict:
    rows = positions or [
        {"symbol": "AAA", "security_name": "Alpha Corp", "security_type": "EQUITY",
         "account_type": "TFSA", "quantity": "10", "market_value": "750.00", "currency": "USD"},
        {"symbol": "BBB", "security_name": "Beta Inc", "security_type": "EQUITY",
         "account_type": "TFSA", "quantity": "5", "market_value": "250.00", "currency": "USD"},
        {"symbol": "CCC", "security_name": "Cad Co", "security_type": "ETF",
         "account_type": "RRSP", "quantity": "3", "market_value": "100.00", "currency": "CAD"},
    ]
    return {"source": "WEALTHSIMPLE_CSV", "snapshot_at": datetime.now(UTC).isoformat(), "positions": rows}


class Harness:
    def __init__(self, decisions: list[PlannerDecision], clock=None) -> None:
        self.sink = InMemoryAuditSink()
        kwargs = {"clock": clock} if clock else {}
        self.runtime = build_runtime(
            snaptrade_repository=_NoSnapTrade(),
            consent_store=PortfolioConsentStore(**kwargs),
            audit=PortfolioAudit(self.sink),
        )
        self.model = FakeDecisionModel(decisions)
        registry = create_portfolio_research_registry(runtime=self.runtime)
        self.registry = registry
        service = AgentResearchService(
            ModelDrivenResearchPlanner(registry=registry, decision_model=self.model)
        )
        app.dependency_overrides[get_portfolio_runtime] = lambda: self.runtime
        app.dependency_overrides[get_portfolio_research_service] = lambda: service
        self.client = TestClient(app)

    def local(self, headers, context=None):
        return self.client.post("/portfolio/local-context", json=context or csv_context(), headers=headers)

    def consent(self, portfolio_id, headers, categories=ALL):
        return self.client.post(
            f"/portfolio/{portfolio_id}/research-consent",
            json={"included_data_categories": categories},
            headers=headers,
        )

    def research(self, portfolio_id, consent_id, headers, **extra):
        body = {
            "portfolio_id": portfolio_id,
            "source": "WEALTHSIMPLE_CSV",
            "consent_id": consent_id,
            **extra,
        }
        return self.client.post("/agent/portfolio-research", json={"portfolio_context": body}, headers=headers)


@pytest.fixture(autouse=True)
def _cleanup():
    yield
    app.dependency_overrides.pop(get_portfolio_runtime, None)
    app.dependency_overrides.pop(get_portfolio_research_service, None)


def call(tool, **arguments):
    return PlannerDecision(action="call_tool", tool_name=tool, arguments=arguments, reason="r")


FINISH = PlannerDecision(action="finish", reason="done")


def setup(decisions):
    h = Harness(decisions)
    headers = token()
    pid = h.local(headers).json()["portfolio_id"]
    consent = h.consent(pid, headers).json()["consent_id"]
    return h, headers, pid, consent


def test_full_research_is_deterministic_and_safe():
    h, headers, pid, consent = setup(
        [
            call("get_portfolio_summary"),
            call("get_portfolio_positions", currency="USD", maximum_positions=10),
            call("analyze_portfolio_concentration", currency="USD"),
            FINISH,
        ]
    )
    response = h.research(pid, consent, headers)
    assert response.status_code == 200
    body = response.json()
    assert body["status"] == "completed"
    assert body["portfolio_source"] == "WEALTHSIMPLE_CSV"
    assert body["portfolio_disclaimer"] == PORTFOLIO_REPORT_STATEMENT
    currencies = {item["currency"] for item in body["portfolio_currency_summaries"]}
    assert currencies == {"USD", "CAD"}  # never combined
    conc = body["portfolio_concentration"][0]
    assert conc["currency"] == "USD"
    assert conc["largest_position_weight_percent"] == "75.0"
    assert conc["eligible_market_value"] == "1000.00"
    assert not SENSITIVE_KEYS & set(body)
    assert pid not in str(body["portfolio_positions"])
    assert body["tools_used"] == [
        "get_portfolio_summary", "get_portfolio_positions", "analyze_portfolio_concentration",
    ]
    assert any(e["event_type"] == "PORTFOLIO_RESEARCH_COMPLETED" for e in h.sink.events)
    assert any(e["event_type"] == "PORTFOLIO_TOOL_EXECUTED" for e in h.sink.events)


def test_no_write_or_trading_tools_registered():
    h = Harness([])
    names = {t.name for t in h.registry.tool_definitions()}
    assert {"get_portfolio_summary", "get_portfolio_positions", "get_portfolio_position",
            "analyze_portfolio_concentration"} <= names
    forbidden = ("order", "trade", "transfer", "deposit", "withdraw", "rebalance", "save")
    assert not [n for n in names if any(f in n for f in forbidden)]


def test_concentration_requires_currency():
    h, headers, pid, consent = setup([call("analyze_portfolio_concentration"), FINISH])
    body = h.research(pid, consent, headers).json()
    assert body["status"] == "failed"
    assert body["error_code"] == "INVALID_TOOL_ARGUMENTS"


def test_model_cannot_supply_identity_or_other_portfolio():
    h, headers, pid, consent = setup(
        [call("get_portfolio_summary", tenant_id="t2", user_id="u2"), FINISH]
    )
    assert h.research(pid, consent, headers).json()["error_code"] == "INVALID_TOOL_ARGUMENTS"

    h, headers, pid, consent = setup([call("get_portfolio_summary", portfolio_id="lp_other"), FINISH])
    assert h.research(pid, consent, headers).json()["error_code"] == "PORTFOLIO_ACCESS_DENIED"


def test_duplicate_portfolio_tool_call_is_skipped():
    h, headers, pid, consent = setup(
        [call("get_portfolio_summary"), call("get_portfolio_summary"), FINISH]
    )
    body = h.research(pid, consent, headers).json()
    assert body["tools_used"] == ["get_portfolio_summary"]


def test_cross_user_and_cross_tenant_access_is_non_disclosing():
    h, headers, pid, _ = setup([FINISH])
    for other in (token(user="u2"), token(tenant="t2")):
        consent = h.consent(pid, other)
        assert consent.status_code == 404
        assert consent.json()["error"]["code"] == "PORTFOLIO_NOT_FOUND"
        missing = h.consent("lp_doesnotexist", other)
        assert missing.status_code == 404
        assert missing.json()["error"] == consent.json()["error"]


def test_other_users_consent_cannot_be_used_or_revoked():
    h, headers, pid, consent = setup([call("get_portfolio_summary"), FINISH])
    other = token(user="u2")
    response = h.research(pid, consent, other)
    assert response.status_code in (403, 404)
    assert h.client.delete(f"/portfolio/{pid}/research-consent/{consent}", headers=other).status_code == 404


def test_missing_consent_is_rejected_and_revocation_blocks_research():
    h, headers, pid, consent = setup([call("get_portfolio_summary"), FINISH])
    assert h.research(pid, "pcn_unknown", headers).status_code == 403
    assert h.client.delete(f"/portfolio/{pid}/research-consent/{consent}", headers=headers).status_code == 200
    response = h.research(pid, consent, headers)
    assert response.status_code == 403
    assert response.json()["error"]["code"] == "PORTFOLIO_CONSENT_REQUIRED"
    assert h.model.calls == []  # the model never ran


def test_expired_consent_is_rejected():
    now = [datetime.now(UTC)]
    h = Harness([FINISH], clock=lambda: now[0])
    headers = token()
    pid = h.local(headers).json()["portfolio_id"]
    consent = h.consent(pid, headers).json()["consent_id"]
    now[0] += timedelta(minutes=16)
    response = h.research(pid, consent, headers)
    assert response.json()["error"]["code"] == "PORTFOLIO_CONSENT_EXPIRED"


def test_book_values_withheld_without_consent():
    h = Harness([call("get_portfolio_positions", currency="USD"), FINISH])
    headers = token()
    ctx = csv_context()
    for row in ctx["positions"]:
        row["book_value"] = "500"
    pid = h.local(headers, ctx).json()["portfolio_id"]
    consent = h.consent(pid, headers, ["ACCOUNTS", "POSITIONS"]).json()["consent_id"]
    body = h.research(pid, consent, headers, include_book_value=True).json()
    assert body["portfolio_positions"]
    assert all(p.get("book_value") is None for p in body["portfolio_positions"])


def test_local_context_limits_and_strictness():
    h = Harness([])
    headers = token()
    many = [csv_context()["positions"][0]] * 51
    assert h.local(headers, csv_context(many)).status_code == 413
    bad = csv_context()
    bad["positions"][0]["account_number"] = "123456789"
    assert h.local(headers, bad).status_code == 422
    bad = csv_context()
    bad["raw_csv"] = "a,b"
    assert h.local(headers, bad).status_code == 422
    huge = csv_context()
    huge["positions"][0]["security_name"] = "x" * (300 * 1024)
    assert h.local(headers, huge).status_code in (413, 422)


def test_context_rejects_identity_and_secret_fields():
    h, headers, pid, consent = setup([FINISH])
    for field in ("user_id", "tenant_id", "connection_id", "account_hash", "user_secret"):
        response = h.research(pid, consent, headers, **{field: "x"})
        assert response.status_code == 422


def test_requires_auth_and_scope():
    h = Harness([])
    assert h.client.post("/portfolio/local-context", json=csv_context()).status_code == 401
    weak = token(scopes=("portfolio:connect",))
    assert h.local(weak).status_code == 403


def test_stock_reports_keep_original_shape():
    from app.models.research_report import ResearchReport

    keys = set(ResearchReport(symbol="AAPL", status="completed", summary="x").model_dump(mode="json"))
    assert not {k for k in keys if k.startswith("portfolio_")}

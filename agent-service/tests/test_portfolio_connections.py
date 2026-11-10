"""SnapTrade Sandbox read-only integration (all offline, fake provider)."""

import inspect
import json
import sqlite3
from datetime import UTC, datetime, timedelta
from decimal import Decimal

import httpx
import pytest
from cryptography.fernet import Fernet
from fastapi.testclient import TestClient
from pydantic import SecretStr

from app.api.portfolio_connection_routes import get_portfolio_connection_service
from app.auth.auth_models import AuthenticatedPrincipal
from app.auth.jwt_service import JwtService
from app.config.settings import Settings, get_settings
from app.main import app
from app.persistence.schema import SCHEMA_SQL
from app.portfolio_connections import normalizer
from app.portfolio_connections.capability_policy import (
    Capability,
    PortfolioProviderCapabilityPolicy,
)
from app.portfolio_connections.config import SnapTradeConfig, assert_read_only_configuration
from app.portfolio_connections.errors import PortfolioConnectionError, provider_unavailable
from app.portfolio_connections.provider import (
    ConnectionPortal,
    PortfolioConnectionProvider,
    ProviderAccount,
    ProviderBalance,
    ProviderConnection,
    ProviderPosition,
    ProviderUserCredentials,
)
from app.portfolio_connections.service import PortfolioConnectionService
from app.portfolio_connections.snaptrade_provider import (
    SnapTradePortfolioConnectionProvider,
    validate_portal_url,
)

REDIRECT = "easymoney://portfolio/snaptrade"


def make_config(**overrides) -> SnapTradeConfig:
    values = dict(
        client_id="client-id",
        consumer_key=SecretStr("consumer-key-secret"),
        encryption_key=SecretStr(Fernet.generate_key().decode()),
        environment="sandbox",
        redirect_uri=REDIRECT,
        connection_type="read",
        broker="SANDBOX",
        timeout_seconds=5,
    )
    values.update(overrides)
    return SnapTradeConfig(**values)


def token(user="u1", tenant="t1", scopes=("portfolio:connect", "portfolio:read")) -> dict[str, str]:
    settings = get_settings()
    jwt_service = JwtService(
        secret=settings.agent_jwt_secret,
        issuer=settings.agent_jwt_issuer,
        audience=settings.agent_jwt_audience,
        algorithm=settings.agent_jwt_algorithm,
    )
    principal = AuthenticatedPrincipal(
        user_id=user,
        tenant_id=tenant,
        scopes=set(scopes),
        expires_at=datetime.now(UTC) + timedelta(hours=1),
    )
    return {"Authorization": f"Bearer {jwt_service.encode(principal)}"}


class FakeProvider(PortfolioConnectionProvider):
    def __init__(self) -> None:
        self.connections: list[ProviderConnection] = []
        self.accounts: list[ProviderAccount] = []
        self.balances: dict[str, list[ProviderBalance]] = {}
        self.positions: dict[str, list[ProviderPosition]] = {}
        self.fail_positions_for: set[str] = set()
        self.fail_accounts: PortfolioConnectionError | None = None
        self.calls: list[str] = []

    async def create_provider_user(self, provider_user_id):
        self.calls.append("create_user")
        self.last_provider_user_id = provider_user_id
        return ProviderUserCredentials(provider_user_id, "user-secret-xyz")

    async def create_connection_portal(self, user, *, reconnect_provider_connection_id=None):
        self.calls.append("portal")
        return ConnectionPortal(
            "https://app.snaptrade.com/portal?x=1", datetime.now(UTC) + timedelta(minutes=5)
        )

    async def get_connections(self, user):
        return list(self.connections)

    async def get_accounts(self, user):
        if self.fail_accounts:
            raise self.fail_accounts
        return list(self.accounts)

    async def get_balances(self, user, account_id):
        return list(self.balances.get(account_id, []))

    async def get_positions(self, user, account_id):
        if account_id in self.fail_positions_for:
            raise provider_unavailable()
        return list(self.positions.get(account_id, []))

    async def refresh_connection(self, user, provider_connection_id):
        self.calls.append("refresh")

    async def disconnect_connection(self, user, provider_connection_id):
        self.calls.append("disconnect")


def position(symbol="AAPL", qty="10", price="100.50", sec_id="sec-aapl"):
    return ProviderPosition(
        provider_position_id=None,
        provider_security_id=sec_id,
        broker_symbol_id=symbol,
        symbol=symbol,
        security_name="Apple Inc.",
        exchange="NASDAQ",
        mic="XNAS",
        currency="USD",
        security_type_code="cs",
        quantity=Decimal(qty),
        price=Decimal(price) if price is not None else None,
        average_cost=Decimal("90.00"),
    )


@pytest.fixture
def rig(tmp_path):
    db = sqlite3.connect(
        str(tmp_path / "t.db"), isolation_level=None, check_same_thread=False
    )
    db.row_factory = sqlite3.Row
    db.executescript(SCHEMA_SQL)
    provider = FakeProvider()
    provider.accounts = [
        ProviderAccount("acc1", "auth1", "Margin", "TFSA", "123456789", "SnapTrade Sandbox", "CAD"),
        ProviderAccount("acc2", "auth1", "Cash", "Margin", "****4321", "SnapTrade Sandbox", "USD"),
    ]
    provider.balances = {
        "acc1": [ProviderBalance("CAD", Decimal("1240.15"))],
        "acc2": [ProviderBalance("USD", Decimal("405.18"))],
    }
    provider.positions = {"acc1": [position()], "acc2": []}
    service = PortfolioConnectionService(config=make_config(), provider=provider, db=db)
    app.dependency_overrides[get_portfolio_connection_service] = lambda: service
    yield service, provider, TestClient(app, raise_server_exceptions=False)
    app.dependency_overrides.clear()


def connect(client, provider, **kw):
    r = client.post(
        "/portfolio/connections/snaptrade/session", json={"redirectUri": REDIRECT}, headers=token(**kw)
    )
    assert r.status_code == 200, r.text
    provider.connections = [ProviderConnection("auth1", "SnapTrade Sandbox", False)]
    return r.json()


# ---- configuration ----------------------------------------------------------


def test_feature_disabled_by_default(monkeypatch):
    monkeypatch.setenv("SNAPTRADE_ENABLED", "false")
    get_settings.cache_clear()
    try:
        client = TestClient(app, raise_server_exceptions=False)
        r = client.post(
            "/portfolio/connections/snaptrade/session",
            json={"redirectUri": REDIRECT},
            headers=token(),
        )
        assert r.status_code == 503
        assert r.json()["error"]["code"] == "SNAPTRADE_DISABLED"
        assert r.json()["error"]["message"] == (
            "Automatic portfolio connection is currently unavailable."
        )
    finally:
        get_settings.cache_clear()


@pytest.mark.parametrize("bad", ["trade", "trade-if-available"])
def test_trade_connection_types_rejected(bad):
    with pytest.raises(RuntimeError):
        assert_read_only_configuration(Settings(snaptrade_connection_type=bad))


def test_non_sandbox_environment_rejected():
    with pytest.raises(RuntimeError):
        assert_read_only_configuration(Settings(snaptrade_environment="production"))


def test_settings_defaults_are_safe():
    s = Settings()
    assert s.snaptrade_is_enabled is False
    assert s.snaptrade_connection_type == "read"
    assert "consumer" not in repr(s).lower() or "**********" in repr(s) or "None" in repr(s)


# ---- read-only boundary -----------------------------------------------------


def test_capability_policy_blocks_all_write_capabilities():
    for cap in (
        Capability.PLACE_ORDER,
        Capability.CANCEL_ORDER,
        Capability.REPLACE_ORDER,
        Capability.TRANSFER,
        Capability.DEPOSIT,
        Capability.WITHDRAW,
        Capability.TRADE_OPTIONS,
        Capability.TRADE_CRYPTO,
    ):
        assert not PortfolioProviderCapabilityPolicy.is_allowed(cap)
        with pytest.raises(PortfolioConnectionError):
            PortfolioProviderCapabilityPolicy.require(cap)


def test_provider_exposes_no_trading_methods():
    forbidden = ("order", "trade", "transfer", "deposit", "withdraw", "rebalance", "buy", "sell")
    for cls in (PortfolioConnectionProvider, SnapTradePortfolioConnectionProvider):
        names = [n.lower() for n, _ in inspect.getmembers(cls, inspect.isfunction)]
        assert not [n for n in names if any(f in n for f in forbidden)], names


@pytest.mark.asyncio
async def test_provider_refuses_unlisted_endpoints():
    seen = []

    def handler(request):
        seen.append(request)
        return httpx.Response(200, json={})

    provider = SnapTradePortfolioConnectionProvider(
        make_config(), transport=httpx.MockTransport(handler)
    )
    for method, path in [
        ("POST", "/trade/place"),
        ("POST", "/accounts/acc1/orders"),
        ("DELETE", "/accounts"),
        ("POST", "/accounts/acc1/transfer"),
    ]:
        with pytest.raises(PortfolioConnectionError) as exc:
            await provider._request(method, path)
        assert exc.value.code == "CAPABILITY_FORBIDDEN"
    assert seen == []


@pytest.mark.asyncio
async def test_portal_request_is_read_sandbox_and_signed():
    captured = {}

    def handler(request):
        captured["body"] = json.loads(request.content)
        captured["url"] = str(request.url)
        captured["sig"] = request.headers.get("Signature")
        return httpx.Response(200, json={"redirectURI": "https://app.snaptrade.com/p"})

    provider = SnapTradePortfolioConnectionProvider(
        make_config(), transport=httpx.MockTransport(handler), clock=lambda: 1700000000
    )
    portal = await provider.create_connection_portal(ProviderUserCredentials("easymoney_x", "s"))
    assert captured["body"] == {
        "broker": "SANDBOX",
        "immediateRedirect": True,
        "customRedirect": REDIRECT,
        "connectionType": "read",
    }
    assert "clientId=client-id" in captured["url"] and "timestamp=1700000000" in captured["url"]
    assert captured["sig"] and "consumer-key-secret" not in captured["sig"]
    assert portal.portal_url.startswith("https://")
    assert "portal_url" not in repr(portal)


def test_portal_host_validated():
    assert validate_portal_url("https://app.snaptrade.com/x")
    for bad in ("http://app.snaptrade.com/x", "https://evil.example.com/x", "https://snaptrade.com.evil.io"):
        with pytest.raises(PortfolioConnectionError):
            validate_portal_url(bad)


# ---- normalization ----------------------------------------------------------


def test_account_type_and_masking():
    assert normalizer.map_account_type("TFSA") == "TFSA"
    assert normalizer.map_account_type("Margin") == "PERSONAL"
    assert normalizer.map_account_type("Weird") == "UNKNOWN"
    assert normalizer.mask_account_number("123456789") == "••••6789"
    assert normalizer.mask_account_number("****4321") == "••••4321"
    assert normalizer.mask_account_number("****") == ""
    assert normalizer.mask_account_number(None) == ""


def test_security_identity_order_and_no_ticker_only_merge():
    a = position(sec_id=None)
    assert normalizer.security_identity(a) == "mic:XNAS:AAPL:USD"
    no_mic = ProviderPosition(None, None, None, "AAPL", "Apple", "NASDAQ", None, "USD", "cs")
    assert normalizer.security_identity(no_mic) == "exch:NASDAQ:AAPL:USD"
    cad = ProviderPosition(None, None, None, "AAPL", "Apple CDR", None, None, "CAD", "cs")
    assert normalizer.security_identity(cad) == "name:apple cdr:CAD"
    assert normalizer.security_identity(position()) == "provider:sec-aapl"
    assert normalizer.security_identity(
        ProviderPosition(None, None, None, "AAPL", None, None, None, None, "cs")
    ) is None


def test_missing_price_is_none_not_zero():
    acc = ProviderAccount("a", None, "n", "TFSA")
    p = normalizer.normalize_position(acc, position(price=None), [])
    assert p.market_value is None and p.market_price is None


# ---- endpoints --------------------------------------------------------------


def test_full_connect_complete_sync_flow(rig):
    service, provider, client = rig
    session = connect(client, provider)
    assert session["portalUrl"].startswith("https://")
    assert session["connectionSessionId"].startswith("cs_")
    assert session["requestId"] is not None
    assert provider.last_provider_user_id.startswith("easymoney_sandbox_")
    assert "u1" not in provider.last_provider_user_id and "t1" not in provider.last_provider_user_id

    r = client.post(
        f"/portfolio/connections/snaptrade/{session['connectionSessionId']}/complete",
        headers=token(),
    )
    assert r.status_code == 200, r.text
    body = r.json()
    assert body["connection"]["status"] == "READY"
    assert body["connection"]["connectionType"] == "READ"
    assert body["connection"]["provider"] == "SNAPTRADE"
    assert len(body["portfolio"]["accounts"]) == 2
    assert len(body["portfolio"]["positions"]) == 1
    assert len(body["portfolio"]["cashBalances"]) == 2
    dumped = r.text
    assert "user-secret-xyz" not in dumped and "123456789" not in dumped
    assert body["portfolio"]["positions"][0]["quantity"] == "10"
    assert body["portfolio"]["positions"][0]["marketValue"] == "1005.00"

    # Deep link is consumed once.
    again = client.post(
        f"/portfolio/connections/snaptrade/{session['connectionSessionId']}/complete",
        headers=token(),
    )
    assert again.status_code == 409

    cid = body["connection"]["connectionId"]
    s1 = client.post(f"/portfolio/connections/{cid}/sync", headers=token()).json()
    s2 = client.post(f"/portfolio/connections/{cid}/sync", headers=token()).json()
    assert s1["portfolio"]["snapshotId"] == s2["portfolio"]["snapshotId"] == body["portfolio"]["snapshotId"]
    assert s1["portfolio"]["effectiveAt"] == s2["portfolio"]["effectiveAt"]

    listed = client.get("/portfolio/connections", headers=token()).json()
    assert [c["connectionId"] for c in listed["connections"]] == [cid]


def test_session_rejects_extra_fields_and_wrong_redirect(rig):
    _, _, client = rig
    for payload in (
        {"redirectUri": REDIRECT, "connectionType": "trade"},
        {"redirectUri": REDIRECT, "broker": "WEALTHSIMPLE"},
        {"redirectUri": "https://evil.example/cb"},
    ):
        r = client.post("/portfolio/connections/snaptrade/session", json=payload, headers=token())
        assert r.status_code == 400
        assert r.json()["error"]["code"] == "INVALID_REQUEST"


def test_incomplete_connection(rig):
    _, _, client = rig
    r = client.post(
        "/portfolio/connections/snaptrade/session", json={"redirectUri": REDIRECT}, headers=token()
    )
    sid = r.json()["connectionSessionId"]
    done = client.post(f"/portfolio/connections/snaptrade/{sid}/complete", headers=token())
    assert done.status_code == 409
    assert done.json()["error"]["code"] == "CONNECTION_INCOMPLETE"


def test_cross_user_and_cross_tenant_get_404(rig):
    service, provider, client = rig
    session = connect(client, provider)
    sid = session["connectionSessionId"]
    assert (
        client.post(
            f"/portfolio/connections/snaptrade/{sid}/complete", headers=token(user="other")
        ).status_code
        == 404
    )
    cid = client.post(
        f"/portfolio/connections/snaptrade/{sid}/complete", headers=token()
    ).json()["connection"]["connectionId"]
    for who in (token(user="other"), token(tenant="other-tenant")):
        assert client.post(f"/portfolio/connections/{cid}/sync", headers=who).status_code == 404
        assert client.delete(f"/portfolio/connections/{cid}", headers=who).status_code == 404
        assert client.delete(f"/portfolio/connections/{cid}/data?confirm=true", headers=who).status_code == 404
        assert client.get(f"/portfolio/connections/{cid}/portfolio", headers=who).status_code == 404
        assert client.post(f"/portfolio/connections/{cid}/reconnect-session", headers=who).status_code == 404


def test_scope_required(rig):
    _, _, client = rig
    r = client.post(
        "/portfolio/connections/snaptrade/session",
        json={"redirectUri": REDIRECT},
        headers=token(scopes=("portfolio:read",)),
    )
    assert r.status_code == 403
    assert client.post("/portfolio/connections/snaptrade/session", json={}).status_code == 401


def _connected(client, provider):
    sid = connect(client, provider)["connectionSessionId"]
    return client.post(
        f"/portfolio/connections/snaptrade/{sid}/complete", headers=token()
    ).json()["connection"]["connectionId"]


def test_failed_sync_preserves_previous_snapshot(rig):
    service, provider, client = rig
    cid = _connected(client, provider)
    before = client.get(f"/portfolio/connections/{cid}/portfolio", headers=token()).json()
    provider.fail_accounts = provider_unavailable()
    r = client.post(f"/portfolio/connections/{cid}/sync", headers=token())
    assert r.status_code == 503
    after = client.get(f"/portfolio/connections/{cid}/portfolio", headers=token()).json()
    assert after["portfolio"] == before["portfolio"]
    assert after["connection"]["status"] == "READY"


def test_partial_failure_keeps_previous_snapshot(rig):
    service, provider, client = rig
    cid = _connected(client, provider)
    before = client.get(f"/portfolio/connections/{cid}/portfolio", headers=token()).json()
    provider.fail_positions_for = {"acc1"}
    r = client.post(f"/portfolio/connections/{cid}/sync", headers=token())
    assert r.status_code == 200
    assert r.json()["portfolio"]["positions"] == before["portfolio"]["positions"]
    assert any(w.startswith("ACCOUNT_PARTIAL") for w in r.json()["portfolio"]["warnings"])


def test_reconnect_required_state(rig):
    service, provider, client = rig
    cid = _connected(client, provider)
    from app.portfolio_connections.errors import reconnect_required

    provider.fail_accounts = reconnect_required()
    r = client.post(f"/portfolio/connections/{cid}/sync", headers=token())
    assert r.status_code == 409 and r.json()["error"]["code"] == "RECONNECT_REQUIRED"
    status = client.get("/portfolio/connections", headers=token()).json()["connections"][0]["status"]
    assert status == "RECONNECT_REQUIRED"
    assert client.post(f"/portfolio/connections/{cid}/reconnect-session", headers=token()).status_code == 200


def test_cash_only_and_no_accounts_scenarios(rig):
    service, provider, client = rig
    provider.accounts = [ProviderAccount("acc1", "auth1", "Cash", "TFSA", "000011112222", None, "CAD")]
    provider.positions = {"acc1": []}
    cid = _connected(client, provider)
    p = client.get(f"/portfolio/connections/{cid}/portfolio", headers=token()).json()["portfolio"]
    assert p["positions"] == [] and len(p["cashBalances"]) == 1

    provider.accounts = []
    s = client.post(f"/portfolio/connections/{cid}/sync", headers=token()).json()
    assert s["portfolio"]["accounts"] == []
    assert s["connection"]["status"] == "READY"


def test_disconnect_and_delete_data(rig):
    service, provider, client = rig
    cid = _connected(client, provider)
    assert client.delete(f"/portfolio/connections/{cid}/data", headers=token()).status_code == 400
    assert client.delete(f"/portfolio/connections/{cid}/data?confirm=true", headers=token()).status_code == 200
    assert client.get(f"/portfolio/connections/{cid}/portfolio", headers=token()).status_code == 404
    assert client.delete(f"/portfolio/connections/{cid}", headers=token()).status_code == 200
    assert "disconnect" in provider.calls
    assert client.post(f"/portfolio/connections/{cid}/sync", headers=token()).status_code == 404


def test_audit_events_are_safe(rig):
    service, provider, client = rig
    _connected(client, provider)
    events = service._audit.list_all()
    kinds = {e["event_type"] for e in events}
    assert {
        "PORTFOLIO_CONNECTION_SESSION_CREATED",
        "PORTFOLIO_CONNECTION_COMPLETED",
        "PORTFOLIO_SYNC_STARTED",
        "PORTFOLIO_SYNC_COMPLETED",
    } <= kinds
    blob = json.dumps(events)
    for secret in ("user-secret-xyz", "123456789", "1005.00", "1240.15", "portal", "AAPL"):
        assert secret not in blob


def test_provider_user_secret_encrypted_at_rest(rig):
    service, provider, client = rig
    connect(client, provider)
    raw = service._store._db.execute(
        "SELECT user_secret_encrypted FROM portfolio_provider_users"
    ).fetchone()[0]
    assert "user-secret-xyz" not in raw

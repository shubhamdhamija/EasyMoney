from __future__ import annotations

import base64
import hashlib
import hmac
import json
import re
import time
from datetime import UTC, datetime, timedelta
from decimal import Decimal, InvalidOperation
from typing import Any
from urllib.parse import quote, urlencode, urlparse

import httpx

from app.portfolio_connections.capability_policy import Capability, PortfolioProviderCapabilityPolicy
from app.portfolio_connections.config import SnapTradeConfig
from app.portfolio_connections.errors import (
    PortfolioConnectionError,
    provider_rate_limited,
    provider_unavailable,
    reconnect_required,
    sync_timeout,
)
from app.portfolio_connections.provider import (
    ConnectionPortal,
    PortfolioConnectionProvider,
    ProviderAccount,
    ProviderBalance,
    ProviderConnection,
    ProviderPosition,
    ProviderUserCredentials,
)

# Fixed provider host. Never configurable: no arbitrary provider URLs.
SNAPTRADE_BASE_URL = "https://api.snaptrade.com"
API_PREFIX = "/api/v1"
PORTAL_LIFETIME = timedelta(minutes=5)

_ID = r"[A-Za-z0-9_\-]{1,128}"

# The ONLY SnapTrade endpoints this integration can ever reach. Every entry is a
# read, connection-management or user-registration call. There is deliberately
# no trading, order, transfer, deposit, withdrawal or options endpoint here, so
# the restriction is enforced in backend code and not only in the Android UI.
_ALLOWED_ENDPOINTS: tuple[tuple[str, re.Pattern[str], Capability], ...] = (
    ("POST", re.compile(r"^/snapTrade/registerUser$"), Capability.CREATE_READ_CONNECTION),
    ("POST", re.compile(r"^/snapTrade/login$"), Capability.CREATE_READ_CONNECTION),
    ("GET", re.compile(r"^/authorizations$"), Capability.READ_CONNECTION_STATUS),
    ("POST", re.compile(rf"^/authorizations/{_ID}/refresh$"), Capability.REFRESH_CONNECTION),
    ("DELETE", re.compile(rf"^/connection/{_ID}$"), Capability.DISCONNECT_CONNECTION),
    ("GET", re.compile(r"^/accounts$"), Capability.READ_ACCOUNTS),
    ("GET", re.compile(rf"^/accounts/{_ID}/balances$"), Capability.READ_BALANCES),
    ("GET", re.compile(rf"^/accounts/{_ID}/positions/all$"), Capability.READ_POSITIONS),
)
_ID_RE = re.compile(rf"^{_ID}$")


def _dec(value: Any) -> Decimal | None:
    if value is None or isinstance(value, bool):
        return None
    try:
        parsed = Decimal(str(value))
    except (InvalidOperation, ValueError):
        return None
    return parsed if parsed.is_finite() else None


def _text(value: Any) -> str | None:
    if value is None:
        return None
    text = str(value).strip()
    return text or None


def validate_portal_url(url: str) -> str:
    parsed = urlparse(url)
    host = (parsed.hostname or "").lower()
    if parsed.scheme != "https" or not (host == "snaptrade.com" or host.endswith(".snaptrade.com")):
        raise provider_unavailable()
    return url


class SnapTradePortfolioConnectionProvider(PortfolioConnectionProvider):
    def __init__(
        self,
        config: SnapTradeConfig,
        *,
        transport: httpx.AsyncBaseTransport | None = None,
        clock=time.time,
    ) -> None:
        self._config = config
        self._transport = transport
        self._clock = clock

    # -- signing ---------------------------------------------------------

    def _signature(self, *, path: str, query: str, body: Any) -> str:
        payload = {"content": body, "path": path, "query": query}
        content = json.dumps(payload, separators=(",", ":"), sort_keys=True)
        key = quote(self._config.consumer_key.get_secret_value(), safe="").encode()
        digest = hmac.new(key, content.encode(), hashlib.sha256).digest()
        return base64.b64encode(digest).decode()

    async def _request(
        self,
        method: str,
        path: str,
        *,
        params: dict[str, str] | None = None,
        body: dict[str, Any] | None = None,
        reconnect_on_auth_error: bool = False,
        not_found_ok: bool = False,
        forbidden_ok: bool = False,
    ) -> Any:
        capability = None
        for allowed_method, pattern, cap in _ALLOWED_ENDPOINTS:
            if allowed_method == method and pattern.match(path):
                capability = cap
                break
        if capability is None:
            raise PortfolioConnectionError(
                code="CAPABILITY_FORBIDDEN",
                safe_message="This operation is not supported.",
                status_code=403,
            )
        PortfolioProviderCapabilityPolicy.require(capability)

        query_items = dict(params or {})
        query_items["clientId"] = self._config.client_id
        query_items["timestamp"] = str(int(self._clock()))
        query = urlencode(query_items)
        full_path = f"{API_PREFIX}{path}"
        headers = {
            "Signature": self._signature(path=full_path, query=query, body=body),
            "Accept": "application/json",
        }
        content = None
        if body is not None:
            content = json.dumps(body, separators=(",", ":"), sort_keys=True).encode()
            headers["Content-Type"] = "application/json"

        try:
            async with httpx.AsyncClient(
                base_url=SNAPTRADE_BASE_URL,
                timeout=httpx.Timeout(self._config.timeout_seconds),
                transport=self._transport,
                follow_redirects=False,
            ) as client:
                response = await client.request(
                    method, f"{full_path}?{query}", headers=headers, content=content
                )
        except httpx.TimeoutException as exc:
            raise sync_timeout() from exc
        except httpx.HTTPError as exc:
            raise provider_unavailable() from exc

        status = response.status_code
        if status == 404 and not_found_ok:
            return None
        if status == 403 and forbidden_ok:
            # e.g. "manual refresh not enabled on the real-time plan": data endpoints are already live.
            return None
        if status == 429:
            raise provider_rate_limited()
        if status in (401, 403) and reconnect_on_auth_error:
            raise reconnect_required()
        if status >= 400:
            raise provider_unavailable()
        if not response.content:
            return None
        try:
            return response.json()
        except ValueError as exc:
            raise provider_unavailable() from exc

    @staticmethod
    def _user_params(user: ProviderUserCredentials) -> dict[str, str]:
        return {"userId": user.provider_user_id, "userSecret": user.user_secret}

    @staticmethod
    def _checked_id(value: str) -> str:
        if not _ID_RE.match(value or ""):
            raise provider_unavailable()
        return value

    # -- PortfolioConnectionProvider ---------------------------------------

    async def create_provider_user(self, provider_user_id: str) -> ProviderUserCredentials:
        data = await self._request(
            "POST", "/snapTrade/registerUser", body={"userId": provider_user_id}
        )
        secret = _text((data or {}).get("userSecret")) if isinstance(data, dict) else None
        if not secret:
            raise provider_unavailable()
        return ProviderUserCredentials(provider_user_id=provider_user_id, user_secret=secret)

    async def create_connection_portal(
        self,
        user: ProviderUserCredentials,
        *,
        reconnect_provider_connection_id: str | None = None,
    ) -> ConnectionPortal:
        # Application-controlled portal parameters. Callers cannot influence them.
        body: dict[str, Any] = {
            "broker": self._config.broker,
            "immediateRedirect": True,
            "customRedirect": self._config.redirect_uri,
            "connectionType": self._config.connection_type,
        }
        if reconnect_provider_connection_id:
            body["reconnect"] = self._checked_id(reconnect_provider_connection_id)
        data = await self._request(
            "POST", "/snapTrade/login", params=self._user_params(user), body=body
        )
        url = _text(data.get("redirectURI")) if isinstance(data, dict) else None
        if not url:
            raise provider_unavailable()
        return ConnectionPortal(
            portal_url=validate_portal_url(url),
            expires_at=datetime.now(UTC) + PORTAL_LIFETIME,
        )

    async def get_connections(self, user: ProviderUserCredentials) -> list[ProviderConnection]:
        data = await self._request(
            "GET", "/authorizations", params=self._user_params(user), reconnect_on_auth_error=True
        )
        result: list[ProviderConnection] = []
        for item in data if isinstance(data, list) else []:
            if not isinstance(item, dict):
                continue
            connection_id = _text(item.get("id"))
            if not connection_id:
                continue
            brokerage = item.get("brokerage") if isinstance(item.get("brokerage"), dict) else {}
            name = _text(item.get("name")) or _text(brokerage.get("name")) or "SnapTrade Sandbox"
            result.append(
                ProviderConnection(
                    provider_connection_id=connection_id,
                    institution_name=name,
                    disabled=bool(item.get("disabled")),
                )
            )
        return result

    async def get_accounts(self, user: ProviderUserCredentials) -> list[ProviderAccount]:
        data = await self._request(
            "GET", "/accounts", params=self._user_params(user), reconnect_on_auth_error=True
        )
        accounts: list[ProviderAccount] = []
        for item in data if isinstance(data, list) else []:
            if not isinstance(item, dict) or not _text(item.get("id")):
                continue
            meta = item.get("meta") if isinstance(item.get("meta"), dict) else {}
            total = ((item.get("balance") or {}).get("total") or {}) if isinstance(
                item.get("balance"), dict
            ) else {}
            currency = total.get("currency") if isinstance(total, dict) else None
            accounts.append(
                ProviderAccount(
                    provider_account_id=str(item["id"]),
                    provider_connection_id=_text(item.get("brokerage_authorization")),
                    display_name=_text(item.get("name")) or "Account",
                    raw_type=_text(item.get("raw_type")) or _text(meta.get("type")),
                    account_number=_text(item.get("number")),
                    institution_name=_text(item.get("institution_name")),
                    currency=_text(currency).upper() if _text(currency) else None,
                    status=_text(meta.get("status")),
                )
            )
        return accounts

    async def get_balances(
        self, user: ProviderUserCredentials, provider_account_id: str
    ) -> list[ProviderBalance]:
        account_id = self._checked_id(provider_account_id)
        data = await self._request(
            "GET",
            f"/accounts/{account_id}/balances",
            params=self._user_params(user),
            reconnect_on_auth_error=True,
        )
        balances: list[ProviderBalance] = []
        for item in data if isinstance(data, list) else []:
            if not isinstance(item, dict):
                continue
            currency = item.get("currency")
            code = _text(currency.get("code")) if isinstance(currency, dict) else _text(currency)
            amount = _dec(item.get("cash"))
            if code and amount is not None:
                balances.append(ProviderBalance(currency=code.upper(), amount=amount))
        return balances

    async def get_positions(
        self, user: ProviderUserCredentials, provider_account_id: str
    ) -> list[ProviderPosition]:
        account_id = self._checked_id(provider_account_id)
        data = await self._request(
            "GET",
            f"/accounts/{account_id}/positions/all",
            params=self._user_params(user),
            reconnect_on_auth_error=True,
        )
        positions: list[ProviderPosition] = []
        # Current API: {"results": [{"instrument": {...}, "units", "price", ...}]}.
        # The legacy bare-list shape is still accepted below.
        items = data.get("results") if isinstance(data, dict) else data
        for item in items if isinstance(items, list) else []:
            if not isinstance(item, dict):
                continue
            instrument = item.get("instrument")
            if isinstance(instrument, dict):
                kind = (_text(instrument.get("kind")) or "").lower()
                if any(x in kind for x in ("option", "future", "cfd")):
                    continue  # derivatives are out of scope
                quantity = _dec(item.get("units"))
                if quantity is None:
                    continue
                code = _text(item.get("currency")) or _text(instrument.get("currency"))
                positions.append(
                    ProviderPosition(
                        provider_position_id=None,
                        provider_security_id=_text(instrument.get("id")),
                        broker_symbol_id=_text(instrument.get("raw_symbol")),
                        symbol=_text(instrument.get("symbol")),
                        security_name=_text(instrument.get("description")),
                        exchange=_text(instrument.get("exchange")),
                        mic=None,
                        currency=code.upper() if code else None,
                        security_type_code=_text(instrument.get("kind")),
                        quantity=quantity,
                        price=_dec(item.get("price")),
                        average_cost=None,
                    )
                )
                continue
            outer = item.get("symbol") if isinstance(item.get("symbol"), dict) else {}
            inner = outer.get("symbol") if isinstance(outer.get("symbol"), dict) else outer
            exchange = inner.get("exchange") if isinstance(inner.get("exchange"), dict) else {}
            currency = inner.get("currency") if isinstance(inner.get("currency"), dict) else {}
            stype = inner.get("type") if isinstance(inner.get("type"), dict) else {}
            quantity = _dec(item.get("fractional_units"))
            if quantity is None:
                quantity = _dec(item.get("units"))
            if quantity is None:
                continue
            code = _text(currency.get("code"))
            positions.append(
                ProviderPosition(
                    provider_position_id=_text(outer.get("id")),
                    provider_security_id=_text(inner.get("id")),
                    broker_symbol_id=_text(inner.get("raw_symbol")),
                    symbol=_text(inner.get("symbol")),
                    security_name=_text(inner.get("description")),
                    exchange=_text(exchange.get("code")),
                    mic=_text(exchange.get("mic_code")),
                    currency=code.upper() if code else None,
                    security_type_code=_text(stype.get("code")),
                    quantity=quantity,
                    price=_dec(item.get("price")),
                    average_cost=_dec(item.get("average_purchase_price")),
                )
            )
        return positions

    async def refresh_connection(
        self, user: ProviderUserCredentials, provider_connection_id: str
    ) -> None:
        connection_id = self._checked_id(provider_connection_id)
        await self._request(
            "POST",
            f"/authorizations/{connection_id}/refresh",
            params=self._user_params(user),
            forbidden_ok=True,
        )

    async def disconnect_connection(
        self, user: ProviderUserCredentials, provider_connection_id: str
    ) -> None:
        connection_id = self._checked_id(provider_connection_id)
        await self._request(
            "DELETE",
            f"/connection/{connection_id}",
            params=self._user_params(user),
            not_found_ok=True,
        )

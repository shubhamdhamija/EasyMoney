"""SnapTrade Sandbox portfolio reads for the research agent.

Reads ONLY the already-normalized snapshot stored by the existing sync service
(portfolio_snapshots), after verifying tenant + user + connection ownership. It
never touches the provider, provider user secrets, connection tokens or raw
provider payloads, and never returns provider account/position/security IDs:
every identifier is re-derived as an opaque EasyMoney ID.
"""

from __future__ import annotations

from datetime import UTC, datetime
from decimal import Decimal, InvalidOperation
from typing import Any

from app.errors.portfolio_agent_error import portfolio_not_found
from app.models.portfolio_agent import (
    Portfolio,
    PortfolioAccount,
    PortfolioCashBalance,
    PortfolioPosition,
    PortfolioSource,
)
from app.portfolio_connections.store import PortfolioConnectionStore
from app.services.portfolio_research_repository import (
    opaque_id,
    safe_display_name,
    select_accounts,
)


def _dec(value: Any) -> Decimal | None:
    if value is None:
        return None
    try:
        parsed = Decimal(str(value))
    except InvalidOperation:
        return None
    return parsed if parsed.is_finite() else None


def _dt(value: Any) -> datetime | None:
    if not value:
        return None
    try:
        parsed = datetime.fromisoformat(str(value))
    except ValueError:
        return None
    return parsed if parsed.tzinfo else parsed.replace(tzinfo=UTC)


class SnapTradePortfolioResearchRepository:
    def __init__(self, store: PortfolioConnectionStore) -> None:
        self._store = store

    def _load(self, tenant_id: str, user_id: str, portfolio_id: str):
        row = self._store.get_connection(portfolio_id, tenant_id, user_id)
        if row is None or row["status"] == "DISCONNECTED":
            raise portfolio_not_found()
        snapshot = self._store.get_snapshot(portfolio_id)
        if snapshot is None:
            raise portfolio_not_found()
        snapshot_at = _dt(snapshot["effective_at"]) or datetime.now(UTC)
        portfolio = Portfolio(
            portfolio_id=portfolio_id,
            source=PortfolioSource.SNAPTRADE_SANDBOX,
            snapshot_at=snapshot_at,
            last_synced_at=_dt(row.get("last_successful_sync_at")),
        )
        return portfolio, snapshot["payload"]

    @staticmethod
    def _accounts(portfolio_id: str, payload: dict[str, Any]):
        accounts: list[PortfolioAccount] = []
        id_map: dict[str, PortfolioAccount] = {}
        for raw in payload.get("accounts", []):
            provider_id = str(raw.get("providerAccountId", ""))
            if not provider_id:
                continue
            account_type = str(raw.get("accountType") or "UNKNOWN")
            masked = raw.get("maskedAccountNumber") or None
            account = PortfolioAccount(
                account_id=opaque_id("acc", portfolio_id, provider_id),
                account_type=account_type,
                display_name=safe_display_name(raw.get("displayName"), account_type),
                masked_account_number=masked,
            )
            accounts.append(account)
            id_map[provider_id] = account
        return accounts, id_map

    async def get_owned_portfolio(self, *, tenant_id, user_id, portfolio_id) -> Portfolio:
        return self._load(tenant_id, user_id, portfolio_id)[0]

    async def get_owned_accounts(
        self, *, tenant_id, user_id, portfolio_id, selected_account_ids
    ) -> list[PortfolioAccount]:
        _, payload = self._load(tenant_id, user_id, portfolio_id)
        accounts, _ = self._accounts(portfolio_id, payload)
        return select_accounts(accounts, selected_account_ids)

    async def get_owned_positions(
        self, *, tenant_id, user_id, portfolio_id, selected_account_ids
    ) -> list[PortfolioPosition]:
        _, payload = self._load(tenant_id, user_id, portfolio_id)
        accounts, id_map = self._accounts(portfolio_id, payload)
        chosen = {a.account_id for a in select_accounts(accounts, selected_account_ids)}
        positions: list[PortfolioPosition] = []
        for raw in payload.get("positions", []):
            account = id_map.get(str(raw.get("accountId", "")))
            if account is None or account.account_id not in chosen:
                continue
            quantity = _dec(raw.get("quantity"))
            if quantity is None:
                continue
            average_cost = _dec(raw.get("averageCost"))
            identity = str(raw.get("securityIdentity") or raw.get("securityName") or "")
            positions.append(
                PortfolioPosition(
                    position_id=opaque_id(
                        "pos", portfolio_id, account.account_id, identity, str(raw.get("providerPositionId", ""))
                    ),
                    account_id=account.account_id,
                    account_type=account.account_type,
                    security_key=opaque_id("sec", portfolio_id, identity),
                    symbol=raw.get("symbol") or None,
                    security_name=str(raw.get("securityName") or raw.get("symbol") or "Unknown security"),
                    security_type=str(raw.get("securityType") or "OTHER"),
                    quantity=quantity,
                    market_price=_dec(raw.get("marketPrice")),
                    market_value=_dec(raw.get("marketValue")),
                    currency=str(raw.get("currency") or "XXX").upper(),
                    book_value=None if average_cost is None else quantity * average_cost,
                )
            )
        return positions

    async def get_owned_cash_balances(
        self, *, tenant_id, user_id, portfolio_id, selected_account_ids
    ) -> list[PortfolioCashBalance]:
        _, payload = self._load(tenant_id, user_id, portfolio_id)
        accounts, id_map = self._accounts(portfolio_id, payload)
        chosen = {a.account_id for a in select_accounts(accounts, selected_account_ids)}
        balances: list[PortfolioCashBalance] = []
        for raw in payload.get("cashBalances", []):
            account = id_map.get(str(raw.get("accountId", "")))
            amount = _dec(raw.get("amount"))
            if account is None or account.account_id not in chosen or amount is None:
                continue
            balances.append(
                PortfolioCashBalance(
                    account_id=account.account_id,
                    account_type=account.account_type,
                    currency=str(raw.get("currency") or "XXX").upper(),
                    amount=amount,
                )
            )
        return balances

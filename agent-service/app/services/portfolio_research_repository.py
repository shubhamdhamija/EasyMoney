"""Server-side portfolio repository protocol and the request-scoped local store.

Every read verifies tenant + user ownership of the portfolio and returns only
normalized records. Cross-user and cross-tenant reads raise the same
non-disclosing "not found" as a missing portfolio. A portfolio ID, account ID or
snapshot ID is an identifier, never authorization.

The repository never returns provider credentials, provider user secrets, raw
account numbers, account-number hashes, raw CSV data, file names, file URIs,
import fingerprints or device IDs.
"""

from __future__ import annotations

import hashlib
import re
import threading
import uuid
from collections.abc import Callable
from datetime import UTC, datetime, timedelta
from typing import Protocol

from app.errors.portfolio_agent_error import portfolio_not_found
from app.models.portfolio_agent import (
    LocalPortfolioContext,
    Portfolio,
    PortfolioAccount,
    PortfolioCashBalance,
    PortfolioPosition,
    PortfolioSource,
)

LOCAL_PORTFOLIO_PREFIX = "lp_"
SNAPTRADE_PORTFOLIO_PREFIX = "pc_"
LOCAL_CONTEXT_LIFETIME = timedelta(minutes=15)

_LONG_DIGITS = re.compile(r"\d{5,}")
_ACCOUNT_LABELS = {
    "TFSA": "TFSA",
    "RRSP": "RRSP",
    "FHSA": "FHSA",
    "RESP": "RESP",
    "LIRA": "LIRA",
    "PERSONAL": "Personal",
    "CRYPTO": "Crypto",
    "CASH": "Cash",
}


class PortfolioResearchRepository(Protocol):
    async def get_owned_portfolio(
        self, *, tenant_id: str, user_id: str, portfolio_id: str
    ) -> Portfolio: ...

    async def get_owned_accounts(
        self,
        *,
        tenant_id: str,
        user_id: str,
        portfolio_id: str,
        selected_account_ids: list[str] | None,
    ) -> list[PortfolioAccount]: ...

    async def get_owned_positions(
        self,
        *,
        tenant_id: str,
        user_id: str,
        portfolio_id: str,
        selected_account_ids: list[str] | None,
    ) -> list[PortfolioPosition]: ...

    async def get_owned_cash_balances(
        self,
        *,
        tenant_id: str,
        user_id: str,
        portfolio_id: str,
        selected_account_ids: list[str] | None,
    ) -> list[PortfolioCashBalance]: ...


def opaque_id(prefix: str, *parts: str) -> str:
    digest = hashlib.sha256("\x00".join(parts).encode()).hexdigest()[:20]
    return f"{prefix}_{digest}"


def safe_display_name(display_name: str | None, account_type: str) -> str:
    """Account label that cannot embed a full account number."""
    label = _ACCOUNT_LABELS.get(account_type, "Investment")
    name = " ".join((display_name or "").split())
    if not name or _LONG_DIGITS.search(name):
        return f"{label} account"
    return name[:60]


def select_accounts(
    accounts: list[PortfolioAccount], selected_account_ids: list[str] | None
) -> list[PortfolioAccount]:
    """Selected accounts must all belong to the portfolio; otherwise a non-disclosing 404."""
    if not selected_account_ids:
        return list(accounts)
    owned = {a.account_id: a for a in accounts}
    if any(account_id not in owned for account_id in selected_account_ids):
        raise portfolio_not_found()
    return [owned[account_id] for account_id in dict.fromkeys(selected_account_ids)]


class _LocalEntry:
    def __init__(
        self,
        *,
        tenant_id: str,
        user_id: str,
        portfolio: Portfolio,
        accounts: list[PortfolioAccount],
        positions: list[PortfolioPosition],
        expires_at: datetime,
    ) -> None:
        self.tenant_id = tenant_id
        self.user_id = user_id
        self.portfolio = portfolio
        self.accounts = accounts
        self.positions = positions
        self.expires_at = expires_at


class LocalPortfolioStore:
    """Ephemeral store for request-scoped normalized CSV context.

    The original CSV is never uploaded. Entries expire after 15 minutes, live only
    in process memory, and are never written to disk.
    """

    def __init__(
        self,
        *,
        clock: Callable[[], datetime] = lambda: datetime.now(UTC),
        lifetime: timedelta = LOCAL_CONTEXT_LIFETIME,
    ) -> None:
        self._clock = clock
        self._lifetime = lifetime
        self._entries: dict[str, _LocalEntry] = {}
        self._lock = threading.Lock()

    def put(self, *, tenant_id: str, user_id: str, context: LocalPortfolioContext) -> Portfolio:
        now = self._clock()
        portfolio_id = LOCAL_PORTFOLIO_PREFIX + uuid.uuid4().hex
        portfolio = Portfolio(
            portfolio_id=portfolio_id, source=context.source, snapshot_at=context.snapshot_at
        )
        accounts: dict[str, PortfolioAccount] = {}
        positions: list[PortfolioPosition] = []
        for index, row in enumerate(context.positions):
            account_type = row.account_type.upper()
            account_id = opaque_id("acc", portfolio_id, account_type)
            accounts.setdefault(
                account_id,
                PortfolioAccount(
                    account_id=account_id,
                    account_type=account_type,
                    display_name=safe_display_name(None, account_type),
                ),
            )
            identity = f"{(row.symbol or '').upper()}|{row.security_name.lower()}|{row.currency}"
            positions.append(
                PortfolioPosition(
                    position_id=opaque_id("pos", portfolio_id, str(index)),
                    account_id=account_id,
                    account_type=account_type,
                    security_key=opaque_id("sec", identity),
                    symbol=row.symbol,
                    security_name=row.security_name,
                    security_type=row.security_type.upper(),
                    quantity=row.quantity,
                    market_price=None,
                    market_value=row.market_value,
                    currency=row.currency,
                    book_value=row.book_value,
                )
            )
        with self._lock:
            self._purge(now)
            self._entries[portfolio_id] = _LocalEntry(
                tenant_id=tenant_id,
                user_id=user_id,
                portfolio=portfolio,
                accounts=list(accounts.values()),
                positions=positions,
                expires_at=now + self._lifetime,
            )
        return portfolio

    def get(self, *, tenant_id: str, user_id: str, portfolio_id: str) -> _LocalEntry:
        now = self._clock()
        with self._lock:
            entry = self._entries.get(portfolio_id)
            if entry is not None and now >= entry.expires_at:
                del self._entries[portfolio_id]
                entry = None
        if entry is None or entry.tenant_id != tenant_id or entry.user_id != user_id:
            raise portfolio_not_found()
        return entry

    def delete(self, *, tenant_id: str, user_id: str, portfolio_id: str) -> None:
        with self._lock:
            entry = self._entries.get(portfolio_id)
            if entry is not None and entry.tenant_id == tenant_id and entry.user_id == user_id:
                del self._entries[portfolio_id]

    def _purge(self, now: datetime) -> None:
        for key in [k for k, v in self._entries.items() if now >= v.expires_at]:
            del self._entries[key]


class LocalPortfolioResearchRepository:
    def __init__(self, store: LocalPortfolioStore) -> None:
        self._store = store

    async def get_owned_portfolio(self, *, tenant_id, user_id, portfolio_id) -> Portfolio:
        return self._store.get(
            tenant_id=tenant_id, user_id=user_id, portfolio_id=portfolio_id
        ).portfolio

    async def get_owned_accounts(
        self, *, tenant_id, user_id, portfolio_id, selected_account_ids
    ) -> list[PortfolioAccount]:
        entry = self._store.get(tenant_id=tenant_id, user_id=user_id, portfolio_id=portfolio_id)
        return select_accounts(entry.accounts, selected_account_ids)

    async def get_owned_positions(
        self, *, tenant_id, user_id, portfolio_id, selected_account_ids
    ) -> list[PortfolioPosition]:
        entry = self._store.get(tenant_id=tenant_id, user_id=user_id, portfolio_id=portfolio_id)
        chosen = {a.account_id for a in select_accounts(entry.accounts, selected_account_ids)}
        return [p for p in entry.positions if p.account_id in chosen]

    async def get_owned_cash_balances(
        self, *, tenant_id, user_id, portfolio_id, selected_account_ids
    ) -> list[PortfolioCashBalance]:
        entry = self._store.get(tenant_id=tenant_id, user_id=user_id, portfolio_id=portfolio_id)
        select_accounts(entry.accounts, selected_account_ids)
        return []  # CSV holdings exports carry no cash balances in this context


class CompositePortfolioResearchRepository:
    """Dispatches on the opaque ID prefix. One ownership model, two storage backends."""

    def __init__(self, *, snaptrade: PortfolioResearchRepository, local: PortfolioResearchRepository) -> None:
        self._snaptrade = snaptrade
        self._local = local

    def _pick(self, portfolio_id: str) -> PortfolioResearchRepository:
        if portfolio_id.startswith(SNAPTRADE_PORTFOLIO_PREFIX):
            return self._snaptrade
        if portfolio_id.startswith(LOCAL_PORTFOLIO_PREFIX):
            return self._local
        raise portfolio_not_found()

    async def get_owned_portfolio(self, *, tenant_id, user_id, portfolio_id) -> Portfolio:
        return await self._pick(portfolio_id).get_owned_portfolio(
            tenant_id=tenant_id, user_id=user_id, portfolio_id=portfolio_id
        )

    async def get_owned_accounts(self, *, tenant_id, user_id, portfolio_id, selected_account_ids):
        return await self._pick(portfolio_id).get_owned_accounts(
            tenant_id=tenant_id,
            user_id=user_id,
            portfolio_id=portfolio_id,
            selected_account_ids=selected_account_ids,
        )

    async def get_owned_positions(self, *, tenant_id, user_id, portfolio_id, selected_account_ids):
        return await self._pick(portfolio_id).get_owned_positions(
            tenant_id=tenant_id,
            user_id=user_id,
            portfolio_id=portfolio_id,
            selected_account_ids=selected_account_ids,
        )

    async def get_owned_cash_balances(self, *, tenant_id, user_id, portfolio_id, selected_account_ids):
        return await self._pick(portfolio_id).get_owned_cash_balances(
            tenant_id=tenant_id,
            user_id=user_id,
            portfolio_id=portfolio_id,
            selected_account_ids=selected_account_ids,
        )


__all__ = [
    "CompositePortfolioResearchRepository",
    "LocalPortfolioResearchRepository",
    "LocalPortfolioStore",
    "PortfolioResearchRepository",
    "PortfolioSource",
    "opaque_id",
    "safe_display_name",
    "select_accounts",
]

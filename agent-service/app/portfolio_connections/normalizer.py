from __future__ import annotations

import hashlib
import json
import re
from dataclasses import dataclass, field
from datetime import datetime
from decimal import Decimal
from typing import Any

from app.portfolio_connections.provider import (
    ProviderAccount,
    ProviderBalance,
    ProviderPosition,
)

# Maps are exact on purpose: unsupported account types are never guessed.
_ACCOUNT_TYPES = {
    "TFSA": "TFSA",
    "RRSP": "RRSP",
    "FHSA": "FHSA",
    "RESP": "RESP",
    "LIRA": "LIRA",
    "INDIVIDUAL": "PERSONAL",
    "MARGIN": "PERSONAL",
    "PERSONAL": "PERSONAL",
    "NON-REGISTERED": "PERSONAL",
    "CRYPTO": "CRYPTO",
    "CASH": "CASH",
}

_SECURITY_TYPES = {
    "cs": "EQUITY",
    "ad": "EQUITY",
    "et": "EXCHANGE_TRADED_FUND",
    "oef": "MUTUAL_FUND",
    "cef": "MUTUAL_FUND",
    "bnd": "FIXED_INCOME",
    "crypto": "CRYPTO",
}

_MASK_CHARS = set("*•xX#.-_ ")


def map_account_type(raw_type: str | None) -> str:
    if not raw_type:
        return "UNKNOWN"
    return _ACCOUNT_TYPES.get(raw_type.strip().upper(), "UNKNOWN")


def mask_account_number(number: str | None) -> str:
    """Server-side masking. Never reconstructs digits a provider already masked."""
    if not number:
        return ""
    visible = [c for c in number.strip() if c not in _MASK_CHARS and c.isalnum()]
    if not visible:
        return ""
    return "••••" + "".join(visible[-4:])


def map_security_type(code: str | None) -> str:
    if not code:
        return "OTHER"
    return _SECURITY_TYPES.get(code.strip().lower(), "OTHER")


def _norm_name(name: str | None) -> str:
    return re.sub(r"\s+", " ", (name or "").strip().lower())


def security_identity(p: ProviderPosition) -> str | None:
    """Provider ID -> MIC+ticker+currency -> exchange+ticker+currency -> name+currency.

    Tickers alone never identify a security.
    """
    cur = (p.currency or "").upper()
    ticker = (p.symbol or "").strip().upper()
    if p.provider_security_id:
        return f"provider:{p.provider_security_id}"
    if p.mic and ticker and cur:
        return f"mic:{p.mic.upper()}:{ticker}:{cur}"
    if p.exchange and ticker and cur:
        return f"exch:{p.exchange.upper()}:{ticker}:{cur}"
    name = _norm_name(p.security_name)
    if name and cur:
        return f"name:{name}:{cur}"
    return None


def _s(value: Decimal | None) -> str | None:
    return None if value is None else format(value, "f")


@dataclass(frozen=True)
class NormalizedAccount:
    provider_account_id: str
    display_name: str
    account_type: str
    provider_category: str | None
    institution_name: str | None
    masked_account_number: str
    currency: str | None
    status: str | None

    def to_dict(self) -> dict[str, Any]:
        return {
            "providerAccountId": self.provider_account_id,
            "displayName": self.display_name,
            "accountType": self.account_type,
            "providerCategory": self.provider_category,
            "institutionName": self.institution_name,
            "maskedAccountNumber": self.masked_account_number,
            "currency": self.currency,
            "status": self.status,
        }


@dataclass(frozen=True)
class NormalizedPosition:
    provider_position_id: str
    account_id: str
    provider_security_id: str | None
    security_identity: str
    symbol: str | None
    security_name: str
    exchange: str | None
    mic: str | None
    quantity: Decimal
    market_price: Decimal | None
    market_value: Decimal | None
    currency: str | None
    average_cost: Decimal | None
    security_type: str

    def to_dict(self) -> dict[str, Any]:
        return {
            "providerPositionId": self.provider_position_id,
            "accountId": self.account_id,
            "providerSecurityId": self.provider_security_id,
            "securityIdentity": self.security_identity,
            "symbol": self.symbol,
            "securityName": self.security_name,
            "exchange": self.exchange,
            "mic": self.mic,
            "quantity": _s(self.quantity),
            "marketPrice": _s(self.market_price),
            "marketValue": _s(self.market_value),
            "currency": self.currency,
            "averageCost": _s(self.average_cost),
            "securityType": self.security_type,
        }


@dataclass(frozen=True)
class NormalizedCashBalance:
    account_id: str
    currency: str
    amount: Decimal

    def to_dict(self) -> dict[str, Any]:
        return {"accountId": self.account_id, "currency": self.currency, "amount": _s(self.amount)}


@dataclass(frozen=True)
class NormalizedPortfolio:
    accounts: list[NormalizedAccount]
    positions: list[NormalizedPosition]
    cash_balances: list[NormalizedCashBalance]
    warnings: list[str] = field(default_factory=list)

    def fingerprint(self) -> str:
        canonical = json.dumps(
            {
                "a": sorted(a.to_dict()["providerAccountId"] for a in self.accounts),
                "p": sorted(
                    (json.dumps(p.to_dict(), sort_keys=True) for p in self.positions)
                ),
                "c": sorted(
                    (json.dumps(c.to_dict(), sort_keys=True) for c in self.cash_balances)
                ),
            },
            sort_keys=True,
            separators=(",", ":"),
        )
        return hashlib.sha256(canonical.encode()).hexdigest()

    def to_dict(self, *, snapshot_id: str, effective_at: datetime) -> dict[str, Any]:
        return {
            "snapshotId": snapshot_id,
            "effectiveAt": effective_at.isoformat(),
            "accounts": [a.to_dict() for a in self.accounts],
            "positions": [p.to_dict() for p in self.positions],
            "cashBalances": [c.to_dict() for c in self.cash_balances],
            "warnings": list(self.warnings),
        }


def normalize_account(a: ProviderAccount) -> NormalizedAccount:
    return NormalizedAccount(
        provider_account_id=a.provider_account_id,
        display_name=a.display_name,
        account_type=map_account_type(a.raw_type),
        provider_category=(a.raw_type or None),
        institution_name=a.institution_name,
        masked_account_number=mask_account_number(a.account_number),
        currency=a.currency,
        status=a.status,
    )


def normalize_position(
    account: ProviderAccount, p: ProviderPosition, warnings: list[str]
) -> NormalizedPosition | None:
    currency = p.currency or account.currency
    probe = ProviderPosition(
        provider_position_id=p.provider_position_id,
        provider_security_id=p.provider_security_id,
        broker_symbol_id=p.broker_symbol_id,
        symbol=p.symbol,
        security_name=p.security_name,
        exchange=p.exchange,
        mic=p.mic,
        currency=currency,
        security_type_code=p.security_type_code,
        quantity=p.quantity,
        price=p.price,
        average_cost=p.average_cost,
    )
    identity = security_identity(probe)
    if identity is None:
        warnings.append("POSITION_SKIPPED_NO_IDENTITY")
        return None
    market_value = p.quantity * p.price if p.price is not None else None
    position_id = p.provider_position_id or "pos_" + hashlib.sha256(
        f"{account.provider_account_id}|{identity}".encode()
    ).hexdigest()[:24]
    return NormalizedPosition(
        provider_position_id=position_id,
        account_id=account.provider_account_id,
        provider_security_id=p.provider_security_id,
        security_identity=identity,
        symbol=(p.symbol.upper() if p.symbol else None),
        security_name=p.security_name or p.symbol or "Unknown security",
        exchange=p.exchange,
        mic=p.mic,
        quantity=p.quantity,
        market_price=p.price,
        market_value=market_value,
        currency=currency,
        average_cost=p.average_cost,
        security_type=map_security_type(p.security_type_code),
    )


def normalize_cash(account_id: str, b: ProviderBalance) -> NormalizedCashBalance:
    return NormalizedCashBalance(account_id=account_id, currency=b.currency, amount=b.amount)

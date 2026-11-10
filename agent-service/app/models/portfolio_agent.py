"""Safe, normalized portfolio models for read-only agent research.

Nothing in this module can carry provider credentials, provider IDs, raw account
numbers, account-number hashes, raw CSV data, file URIs, or device identifiers.
All identifiers are opaque EasyMoney identifiers. Every model rejects unknown
fields so a client or model cannot smuggle extra data in.

There is intentionally no order, trade, transfer or rebalance model here.
"""

from __future__ import annotations

from datetime import datetime
from decimal import Decimal
from enum import Enum
from typing import Annotated

from pydantic import (
    BaseModel,
    ConfigDict,
    Field,
    PlainSerializer,
    field_validator,
    model_validator,
)

PORTFOLIO_DISCLAIMER = (
    "This analysis is based on a read-only portfolio snapshot. No trades or account "
    "changes were made. Values may be delayed or incomplete."
)

PORTFOLIO_REPORT_STATEMENT = (
    "Portfolio information reflects the selected snapshot. No trades or account "
    "changes were made."
)

MAX_SELECTED_ACCOUNTS = 20
MAX_POSITIONS = 50
DEFAULT_POSITIONS = 20
MAX_TOP_POSITIONS = 10
MAX_LOCAL_PAYLOAD_BYTES = 256 * 1024

# Decimal values are serialized as canonical (non-scientific) strings in JSON.
DecimalStr = Annotated[
    Decimal,
    PlainSerializer(lambda value: format(value, "f"), return_type=str, when_used="json"),
]


class PortfolioSource(str, Enum):
    WEALTHSIMPLE_CSV = "WEALTHSIMPLE_CSV"
    SNAPTRADE_SANDBOX = "SNAPTRADE_SANDBOX"
    MANUAL = "MANUAL"
    DEMO = "DEMO"
    # SNAPTRADE_LIVE is deliberately absent: it is not enabled in this milestone.


class PortfolioResearchDataCategory(str, Enum):
    ACCOUNTS = "ACCOUNTS"
    POSITIONS = "POSITIONS"
    CASH_BALANCES = "CASH_BALANCES"
    BOOK_VALUES = "BOOK_VALUES"


class PortfolioWarningCode(str, Enum):
    STALE_SNAPSHOT = "STALE_SNAPSHOT"
    MIXED_CURRENCIES = "MIXED_CURRENCIES"
    MISSING_MARKET_VALUES = "MISSING_MARKET_VALUES"
    POSITIONS_EXCLUDED_FROM_ALLOCATION = "POSITIONS_EXCLUDED_FROM_ALLOCATION"
    MISSING_BOOK_VALUES = "MISSING_BOOK_VALUES"
    EMPTY_PORTFOLIO = "EMPTY_PORTFOLIO"
    NO_POSITIONS_FOR_CURRENCY = "NO_POSITIONS_FOR_CURRENCY"
    SNAPSHOT_SOURCE_IS_CSV = "SNAPSHOT_SOURCE_IS_CSV"
    SNAPSHOT_SOURCE_IS_SANDBOX = "SNAPSHOT_SOURCE_IS_SANDBOX"
    PARTIAL_POSITION_LIST = "PARTIAL_POSITION_LIST"
    UNKNOWN_SECURITY_TYPE = "UNKNOWN_SECURITY_TYPE"
    # Extra deterministic code: several distinct securities share one ticker.
    AMBIGUOUS_SYMBOL = "AMBIGUOUS_SYMBOL"


class PortfolioSortBy(str, Enum):
    MARKET_VALUE_DESC = "MARKET_VALUE_DESC"
    SYMBOL_ASC = "SYMBOL_ASC"
    SECURITY_NAME_ASC = "SECURITY_NAME_ASC"
    UNREALIZED_RETURN_DESC = "UNREALIZED_RETURN_DESC"


def _safe_symbol(value: str) -> str:
    normalized = value.strip().upper()
    if not normalized:
        raise ValueError("symbol must not be blank")
    allowed = set("ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789.-")
    if any(ch not in allowed for ch in normalized):
        raise ValueError("symbol contains unsupported characters")
    return normalized


def _safe_currency(value: str) -> str:
    normalized = value.strip().upper()
    if len(normalized) != 3 or not normalized.isalpha():
        raise ValueError("currency must be a 3-letter code")
    return normalized


def _clean_account_ids(value: list[str] | None) -> list[str] | None:
    if value is None:
        return None
    cleaned: list[str] = []
    for item in value:
        item = item.strip()
        if not item:
            raise ValueError("account id must not be blank")
        if item not in cleaned:
            cleaned.append(item)
    return cleaned


# ---------------------------------------------------------------------------
# Internal normalized domain records (what a repository returns)
# ---------------------------------------------------------------------------


class _Strict(BaseModel):
    model_config = ConfigDict(extra="forbid")


class Portfolio(_Strict):
    portfolio_id: str
    source: PortfolioSource
    snapshot_at: datetime
    last_synced_at: datetime | None = None


class PortfolioAccount(_Strict):
    account_id: str
    account_type: str
    display_name: str
    masked_account_number: str | None = None


class PortfolioPosition(_Strict):
    position_id: str
    account_id: str
    account_type: str
    # Opaque hash of the normalized security identity; never a provider ID.
    security_key: str
    symbol: str | None = None
    security_name: str
    security_type: str
    quantity: Decimal
    market_price: Decimal | None = None
    market_value: Decimal | None = None
    currency: str
    book_value: Decimal | None = None


class PortfolioCashBalance(_Strict):
    account_id: str
    account_type: str
    currency: str
    amount: Decimal


# ---------------------------------------------------------------------------
# Context models
# ---------------------------------------------------------------------------


class PortfolioContextRequest(_Strict):
    """Client-supplied pointer to a server-owned portfolio. No holdings, no secrets."""

    portfolio_id: str = Field(min_length=1, max_length=64)
    source: PortfolioSource
    selected_account_ids: list[str] = Field(default_factory=list, max_length=MAX_SELECTED_ACCOUNTS)
    include_positions: bool = True
    maximum_positions: int = Field(default=DEFAULT_POSITIONS, ge=1, le=MAX_POSITIONS)
    include_cash: bool = True
    include_book_value: bool = False
    consent_id: str = Field(min_length=1, max_length=64)

    @field_validator("selected_account_ids")
    @classmethod
    def _accounts(cls, value: list[str]) -> list[str]:
        return _clean_account_ids(value) or []


class PortfolioResearchContext(_Strict):
    """Server-validated research context built from an owned portfolio."""

    portfolio_id: str = Field(min_length=1, max_length=64)
    source: PortfolioSource
    snapshot_at: datetime
    last_synced_at: datetime | None = None
    selected_account_ids: list[str] = Field(default_factory=list, max_length=MAX_SELECTED_ACCOUNTS)
    include_positions: bool = True
    maximum_positions: int = Field(default=DEFAULT_POSITIONS, ge=1, le=MAX_POSITIONS)
    include_cash: bool = True
    include_book_value: bool = False


class LocalPositionInput(_Strict):
    """Minimum normalized fields for one request-scoped CSV position."""

    symbol: str | None = Field(default=None, max_length=15)
    security_name: str = Field(min_length=1, max_length=120)
    security_type: str = Field(min_length=1, max_length=32)
    account_type: str = Field(min_length=1, max_length=32)
    quantity: Decimal
    market_value: Decimal | None = None
    currency: str
    book_value: Decimal | None = None

    @field_validator("symbol")
    @classmethod
    def _symbol(cls, value: str | None) -> str | None:
        if value is None or not value.strip():
            return None
        return _safe_symbol(value)

    @field_validator("currency")
    @classmethod
    def _currency(cls, value: str) -> str:
        return _safe_currency(value)

    @field_validator("security_name", "security_type", "account_type")
    @classmethod
    def _text(cls, value: str) -> str:
        normalized = " ".join(value.split())
        if not normalized:
            raise ValueError("must not be blank")
        return normalized

    @field_validator("quantity", "market_value", "book_value")
    @classmethod
    def _finite(cls, value: Decimal | None) -> Decimal | None:
        if value is not None and not value.is_finite():
            raise ValueError("must be finite")
        return value


class LocalPortfolioContext(_Strict):
    """Request-scoped normalized portfolio built on-device from a CSV import.

    The original CSV, file name, file URI, raw account numbers, account hashes and
    device identifiers are never part of this model.
    """

    source: PortfolioSource = PortfolioSource.WEALTHSIMPLE_CSV
    snapshot_at: datetime
    positions: list[LocalPositionInput] = Field(min_length=1, max_length=MAX_POSITIONS)

    @model_validator(mode="after")
    def _csv_only(self) -> "LocalPortfolioContext":
        if self.source not in (PortfolioSource.WEALTHSIMPLE_CSV, PortfolioSource.MANUAL):
            raise ValueError("unsupported local source")
        return self


# ---------------------------------------------------------------------------
# Consent
# ---------------------------------------------------------------------------


class PortfolioResearchConsent(_Strict):
    consent_id: str
    tenant_id: str
    user_id: str
    portfolio_id: str
    request_id: str | None = None
    granted_at: datetime
    expires_at: datetime
    included_data_categories: list[PortfolioResearchDataCategory]
    revoked_at: datetime | None = None


class ResearchConsentRequest(_Strict):
    included_data_categories: list[PortfolioResearchDataCategory] = Field(min_length=1, max_length=4)


class ResearchConsentResponse(_Strict):
    """Never exposes the user ID or tenant ID."""

    consent_id: str
    portfolio_id: str
    granted_at: datetime
    expires_at: datetime
    included_data_categories: list[PortfolioResearchDataCategory]
    request_id: str | None = None


# ---------------------------------------------------------------------------
# Safe result models
# ---------------------------------------------------------------------------


class PortfolioWarning(_Strict):
    code: PortfolioWarningCode
    currency: str | None = None
    count: int | None = None


class PortfolioAccountSummary(_Strict):
    account_id: str
    account_type: str
    display_name: str
    masked_account_number: str | None = None
    currencies: list[str] = Field(default_factory=list)
    position_count: int = Field(ge=0)
    cash_balance_count: int = Field(ge=0)


class PortfolioPositionSummary(_Strict):
    position_id: str
    symbol: str | None = None
    security_name: str
    security_type: str
    account_type: str
    quantity: DecimalStr
    market_price: DecimalStr | None = None
    market_value: DecimalStr | None = None
    currency: str
    book_value: DecimalStr | None = None
    unrealized_return: DecimalStr | None = None
    snapshot_at: datetime


class PortfolioCashSummary(_Strict):
    account_type: str
    currency: str
    amount: DecimalStr


class PortfolioCurrencySummary(_Strict):
    currency: str
    investment_market_value: DecimalStr
    # None when cash balances were not read (never silently zero).
    cash_value: DecimalStr | None = None
    book_value: DecimalStr | None = None
    unrealized_return: DecimalStr | None = None
    position_count: int = Field(ge=0)


class PortfolioSummaryResult(_Strict):
    portfolio_id: str
    source: PortfolioSource
    snapshot_at: datetime
    last_synced_at: datetime | None = None
    accounts: list[PortfolioAccountSummary]
    currency_summaries: list[PortfolioCurrencySummary]
    warnings: list[PortfolioWarning] = Field(default_factory=list)
    informational_disclaimer: str = PORTFOLIO_DISCLAIMER


class PortfolioPositionsResult(_Strict):
    portfolio_id: str
    source: PortfolioSource
    snapshot_at: datetime
    positions: list[PortfolioPositionSummary]
    total_matching_positions: int = Field(ge=0)
    returned_position_count: int = Field(ge=0)
    truncated: bool = False
    warnings: list[PortfolioWarning] = Field(default_factory=list)
    informational_disclaimer: str = PORTFOLIO_DISCLAIMER


class PortfolioSecurityMatch(_Strict):
    security_key: str
    security_name: str
    currency: str
    positions: list[PortfolioPositionSummary]


class PortfolioPositionResult(_Strict):
    portfolio_id: str
    source: PortfolioSource
    snapshot_at: datetime
    symbol: str
    securities: list[PortfolioSecurityMatch]
    warnings: list[PortfolioWarning] = Field(default_factory=list)
    informational_disclaimer: str = PORTFOLIO_DISCLAIMER


class PortfolioWeightedPosition(_Strict):
    symbol: str | None = None
    security_name: str
    market_value: DecimalStr
    weight_percent: DecimalStr


class PortfolioAllocationItem(_Strict):
    label: str
    market_value: DecimalStr
    weight_percent: DecimalStr


class PortfolioConcentrationResult(_Strict):
    portfolio_id: str
    source: PortfolioSource
    currency: str
    snapshot_at: datetime
    eligible_market_value: DecimalStr
    excluded_position_count: int = Field(ge=0)
    largest_position: PortfolioWeightedPosition | None = None
    largest_position_weight_percent: DecimalStr | None = None
    top_positions: list[PortfolioWeightedPosition] = Field(default_factory=list)
    top_positions_weight_percent: DecimalStr | None = None
    account_allocations: list[PortfolioAllocationItem] = Field(default_factory=list)
    security_type_allocations: list[PortfolioAllocationItem] = Field(default_factory=list)
    cash_allocation_percent: DecimalStr | None = None
    warnings: list[PortfolioWarning] = Field(default_factory=list)
    informational_disclaimer: str = PORTFOLIO_DISCLAIMER


# ---------------------------------------------------------------------------
# Tool input models (model-controlled; no identity, tenant or consent fields)
# ---------------------------------------------------------------------------


class _ToolInput(BaseModel):
    model_config = ConfigDict(extra="forbid")


class GetPortfolioSummaryInput(_ToolInput):
    portfolio_id: str = Field(min_length=1, max_length=64)
    selected_account_ids: list[str] | None = Field(default=None, max_length=MAX_SELECTED_ACCOUNTS)
    include_cash: bool = True
    include_book_value: bool = False

    @field_validator("selected_account_ids")
    @classmethod
    def _accounts(cls, value: list[str] | None) -> list[str] | None:
        return _clean_account_ids(value)


class GetPortfolioPositionsInput(_ToolInput):
    portfolio_id: str = Field(min_length=1, max_length=64)
    selected_account_ids: list[str] | None = Field(default=None, max_length=MAX_SELECTED_ACCOUNTS)
    currency: str | None = None
    security_type: str | None = Field(default=None, max_length=32)
    maximum_positions: int = Field(default=DEFAULT_POSITIONS, ge=1, le=MAX_POSITIONS)
    sort_by: PortfolioSortBy = PortfolioSortBy.MARKET_VALUE_DESC

    @field_validator("selected_account_ids")
    @classmethod
    def _accounts(cls, value: list[str] | None) -> list[str] | None:
        return _clean_account_ids(value)

    @field_validator("currency")
    @classmethod
    def _currency(cls, value: str | None) -> str | None:
        return None if value is None else _safe_currency(value)

    @field_validator("security_type")
    @classmethod
    def _security_type(cls, value: str | None) -> str | None:
        return None if value is None else value.strip().upper() or None


class GetPortfolioPositionInput(_ToolInput):
    portfolio_id: str = Field(min_length=1, max_length=64)
    symbol: str = Field(min_length=1, max_length=15)
    selected_account_ids: list[str] | None = Field(default=None, max_length=MAX_SELECTED_ACCOUNTS)

    @field_validator("symbol")
    @classmethod
    def _symbol(cls, value: str) -> str:
        return _safe_symbol(value)

    @field_validator("selected_account_ids")
    @classmethod
    def _accounts(cls, value: list[str] | None) -> list[str] | None:
        return _clean_account_ids(value)


class AnalyzePortfolioConcentrationInput(_ToolInput):
    portfolio_id: str = Field(min_length=1, max_length=64)
    selected_account_ids: list[str] | None = Field(default=None, max_length=MAX_SELECTED_ACCOUNTS)
    currency: str
    maximum_top_positions: int = Field(default=5, ge=1, le=MAX_TOP_POSITIONS)

    @field_validator("selected_account_ids")
    @classmethod
    def _accounts(cls, value: list[str] | None) -> list[str] | None:
        return _clean_account_ids(value)

    @field_validator("currency")
    @classmethod
    def _currency(cls, value: str) -> str:
        return _safe_currency(value)

"""Deterministic portfolio calculations. No LLM, no network, no FX.

Every figure is computed per currency. CAD and USD are never combined: this
milestone has no timestamped FX rate input. Missing values stay missing (None);
they are never converted to zero.
"""

from __future__ import annotations

from collections import defaultdict
from datetime import UTC, datetime, timedelta
from decimal import ROUND_HALF_UP, Decimal

from app.models.portfolio_agent import (
    Portfolio,
    PortfolioAccount,
    PortfolioAccountSummary,
    PortfolioAllocationItem,
    PortfolioCashBalance,
    PortfolioConcentrationResult,
    PortfolioCurrencySummary,
    PortfolioPosition,
    PortfolioPositionSummary,
    PortfolioSortBy,
    PortfolioSource,
    PortfolioWarning,
    PortfolioWarningCode,
    PortfolioWeightedPosition,
)

STALE_AFTER = timedelta(hours=24)
_ONE_DECIMAL = Decimal("0.1")
_HUNDRED = Decimal(100)
_UNKNOWN_TYPES = {"OTHER", "UNKNOWN"}


def round_percent(value: Decimal) -> Decimal:
    return value.quantize(_ONE_DECIMAL, rounding=ROUND_HALF_UP)


def percent_of(numerator: Decimal | None, denominator: Decimal | None) -> Decimal | None:
    """numerator / denominator * 100, one decimal place. None if not computable."""
    if numerator is None or denominator is None or denominator == 0:
        return None
    return round_percent(numerator / denominator * _HUNDRED)


def unrealized_return(position: PortfolioPosition) -> Decimal | None:
    if position.market_value is None or position.book_value is None:
        return None
    return position.market_value - position.book_value


def position_summary(
    position: PortfolioPosition, snapshot_at: datetime, *, include_book_value: bool
) -> PortfolioPositionSummary:
    return PortfolioPositionSummary(
        position_id=position.position_id,
        symbol=position.symbol,
        security_name=position.security_name,
        security_type=position.security_type,
        account_type=position.account_type,
        quantity=position.quantity,
        market_price=position.market_price,
        market_value=position.market_value,
        currency=position.currency,
        book_value=position.book_value if include_book_value else None,
        unrealized_return=unrealized_return(position) if include_book_value else None,
        snapshot_at=snapshot_at,
    )


def build_account_summaries(
    accounts: list[PortfolioAccount],
    positions: list[PortfolioPosition],
    cash: list[PortfolioCashBalance],
) -> list[PortfolioAccountSummary]:
    summaries: list[PortfolioAccountSummary] = []
    for account in sorted(accounts, key=lambda a: (a.display_name, a.account_id)):
        owned_positions = [p for p in positions if p.account_id == account.account_id]
        owned_cash = [c for c in cash if c.account_id == account.account_id]
        currencies = sorted({p.currency for p in owned_positions} | {c.currency for c in owned_cash})
        summaries.append(
            PortfolioAccountSummary(
                account_id=account.account_id,
                account_type=account.account_type,
                display_name=account.display_name,
                masked_account_number=account.masked_account_number,
                currencies=currencies,
                position_count=len(owned_positions),
                cash_balance_count=len(owned_cash),
            )
        )
    return summaries


def build_currency_summaries(
    positions: list[PortfolioPosition],
    cash: list[PortfolioCashBalance] | None,
    *,
    include_book_value: bool,
) -> list[PortfolioCurrencySummary]:
    currencies = {p.currency for p in positions}
    if cash is not None:
        currencies |= {c.currency for c in cash}

    summaries: list[PortfolioCurrencySummary] = []
    for currency in sorted(currencies):
        in_currency = [p for p in positions if p.currency == currency]
        market_values = [p.market_value for p in in_currency if p.market_value is not None]
        investment = sum(market_values, Decimal(0))

        cash_value: Decimal | None = None
        if cash is not None:
            cash_value = sum((c.amount for c in cash if c.currency == currency), Decimal(0))

        book_value: Decimal | None = None
        unrealized: Decimal | None = None
        if include_book_value:
            books = [p.book_value for p in in_currency if p.book_value is not None]
            book_value = sum(books, Decimal(0)) if books else None
            returns = [r for r in (unrealized_return(p) for p in in_currency) if r is not None]
            unrealized = sum(returns, Decimal(0)) if returns else None

        summaries.append(
            PortfolioCurrencySummary(
                currency=currency,
                investment_market_value=investment,
                cash_value=cash_value,
                book_value=book_value,
                unrealized_return=unrealized,
                position_count=len(in_currency),
            )
        )
    return summaries


def _source_warning(source: PortfolioSource) -> PortfolioWarning | None:
    if source == PortfolioSource.WEALTHSIMPLE_CSV:
        return PortfolioWarning(code=PortfolioWarningCode.SNAPSHOT_SOURCE_IS_CSV)
    if source == PortfolioSource.SNAPTRADE_SANDBOX:
        return PortfolioWarning(code=PortfolioWarningCode.SNAPSHOT_SOURCE_IS_SANDBOX)
    return None


def _is_stale(snapshot_at: datetime, now: datetime) -> bool:
    reference = snapshot_at if snapshot_at.tzinfo else snapshot_at.replace(tzinfo=UTC)
    return now - reference > STALE_AFTER


def summary_warnings(
    portfolio: Portfolio,
    positions: list[PortfolioPosition],
    cash: list[PortfolioCashBalance] | None,
    *,
    include_book_value: bool,
    now: datetime | None = None,
) -> list[PortfolioWarning]:
    now = now or datetime.now(UTC)
    warnings: list[PortfolioWarning] = []

    if not positions and not cash:
        warnings.append(PortfolioWarning(code=PortfolioWarningCode.EMPTY_PORTFOLIO))
    if _is_stale(portfolio.snapshot_at, now):
        warnings.append(PortfolioWarning(code=PortfolioWarningCode.STALE_SNAPSHOT))

    currencies = {p.currency for p in positions} | {c.currency for c in (cash or [])}
    if len(currencies) > 1:
        warnings.append(PortfolioWarning(code=PortfolioWarningCode.MIXED_CURRENCIES))

    missing_market = sum(1 for p in positions if p.market_value is None)
    if missing_market:
        warnings.append(
            PortfolioWarning(code=PortfolioWarningCode.MISSING_MARKET_VALUES, count=missing_market)
        )
        warnings.append(
            PortfolioWarning(
                code=PortfolioWarningCode.POSITIONS_EXCLUDED_FROM_ALLOCATION, count=missing_market
            )
        )
    if include_book_value:
        missing_book = sum(1 for p in positions if p.book_value is None)
        if missing_book:
            warnings.append(
                PortfolioWarning(code=PortfolioWarningCode.MISSING_BOOK_VALUES, count=missing_book)
            )
    if any(p.security_type.upper() in _UNKNOWN_TYPES for p in positions):
        warnings.append(PortfolioWarning(code=PortfolioWarningCode.UNKNOWN_SECURITY_TYPE))

    source = _source_warning(portfolio.source)
    if source is not None:
        warnings.append(source)
    return warnings


def filter_positions(
    positions: list[PortfolioPosition],
    *,
    currency: str | None = None,
    security_type: str | None = None,
) -> list[PortfolioPosition]:
    result = positions
    if currency is not None:
        result = [p for p in result if p.currency == currency]
    if security_type is not None:
        result = [p for p in result if p.security_type.upper() == security_type.upper()]
    return result


def sort_positions(
    positions: list[PortfolioPosition], sort_by: PortfolioSortBy
) -> list[PortfolioPosition]:
    """Deterministic ordering; missing values always sort last."""

    def tie(p: PortfolioPosition) -> tuple[str, str]:
        return (p.security_name.lower(), p.position_id)

    if sort_by == PortfolioSortBy.SYMBOL_ASC:
        return sorted(positions, key=lambda p: (p.symbol is None, p.symbol or "", *tie(p)))
    if sort_by == PortfolioSortBy.SECURITY_NAME_ASC:
        return sorted(positions, key=lambda p: (*tie(p),))
    if sort_by == PortfolioSortBy.UNREALIZED_RETURN_DESC:
        return sorted(
            positions,
            key=lambda p: (
                unrealized_return(p) is None,
                -(unrealized_return(p) or Decimal(0)),
                *tie(p),
            ),
        )
    return sorted(
        positions,
        key=lambda p: (p.market_value is None, -(p.market_value or Decimal(0)), *tie(p)),
    )


def analyze_concentration(
    portfolio: Portfolio,
    accounts: list[PortfolioAccount],
    positions: list[PortfolioPosition],
    cash: list[PortfolioCashBalance] | None,
    *,
    currency: str,
    maximum_top_positions: int,
    now: datetime | None = None,
) -> PortfolioConcentrationResult:
    """Concentration within ONE currency. A "position" is a security aggregated
    across the selected accounts. Neutral output only: no risk labels."""
    now = now or datetime.now(UTC)
    in_currency = [p for p in positions if p.currency == currency]
    eligible = [p for p in in_currency if p.market_value is not None]
    excluded = len(in_currency) - len(eligible)
    total = sum((p.market_value for p in eligible if p.market_value is not None), Decimal(0))

    warnings: list[PortfolioWarning] = []
    if not in_currency:
        warnings.append(
            PortfolioWarning(code=PortfolioWarningCode.NO_POSITIONS_FOR_CURRENCY, currency=currency)
        )
    if excluded:
        warnings.append(
            PortfolioWarning(
                code=PortfolioWarningCode.POSITIONS_EXCLUDED_FROM_ALLOCATION,
                currency=currency,
                count=excluded,
            )
        )
        warnings.append(
            PortfolioWarning(
                code=PortfolioWarningCode.MISSING_MARKET_VALUES, currency=currency, count=excluded
            )
        )
    if len({p.currency for p in positions} | {c.currency for c in (cash or [])}) > 1:
        warnings.append(PortfolioWarning(code=PortfolioWarningCode.MIXED_CURRENCIES))
    if _is_stale(portfolio.snapshot_at, now):
        warnings.append(PortfolioWarning(code=PortfolioWarningCode.STALE_SNAPSHOT))
    if any(p.security_type.upper() in _UNKNOWN_TYPES for p in in_currency):
        warnings.append(PortfolioWarning(code=PortfolioWarningCode.UNKNOWN_SECURITY_TYPE))
    source = _source_warning(portfolio.source)
    if source is not None:
        warnings.append(source)

    # Aggregate by security identity (not by ticker alone).
    by_security: dict[str, list[PortfolioPosition]] = defaultdict(list)
    for position in eligible:
        by_security[position.security_key].append(position)
    holdings = []
    for key, group in by_security.items():
        value = sum((p.market_value for p in group if p.market_value is not None), Decimal(0))
        first = sorted(group, key=lambda p: p.position_id)[0]
        holdings.append((value, first.security_name, first.symbol, key))
    holdings.sort(key=lambda h: (-h[0], h[1].lower(), h[3]))

    def weighted(h: tuple[Decimal, str, str | None, str]) -> PortfolioWeightedPosition:
        return PortfolioWeightedPosition(
            symbol=h[2],
            security_name=h[1],
            market_value=h[0],
            weight_percent=percent_of(h[0], total) or Decimal("0.0"),
        )

    top = holdings[:maximum_top_positions]
    top_models = [weighted(h) for h in top] if total != 0 else []
    largest = top_models[0] if top_models else None
    top_share = percent_of(sum((h[0] for h in top), Decimal(0)), total) if top_models else None

    account_labels = {a.account_id: a.display_name for a in accounts}
    by_account: dict[str, Decimal] = defaultdict(Decimal)
    by_type: dict[str, Decimal] = defaultdict(Decimal)
    for position in eligible:
        assert position.market_value is not None
        by_account[position.account_id] += position.market_value
        by_type[position.security_type] += position.market_value

    def allocations(groups: dict[str, Decimal], label_of) -> list[PortfolioAllocationItem]:
        if total == 0:
            return []
        items = [
            PortfolioAllocationItem(
                label=label_of(key),
                market_value=value,
                weight_percent=percent_of(value, total) or Decimal("0.0"),
            )
            for key, value in groups.items()
        ]
        return sorted(items, key=lambda i: (-i.market_value, i.label))

    cash_allocation: Decimal | None = None
    if cash is not None:
        cash_total = sum((c.amount for c in cash if c.currency == currency), Decimal(0))
        cash_allocation = percent_of(cash_total, cash_total + total)

    return PortfolioConcentrationResult(
        portfolio_id=portfolio.portfolio_id,
        source=portfolio.source,
        currency=currency,
        snapshot_at=portfolio.snapshot_at,
        eligible_market_value=total,
        excluded_position_count=excluded,
        largest_position=largest,
        largest_position_weight_percent=None if largest is None else largest.weight_percent,
        top_positions=top_models,
        top_positions_weight_percent=top_share,
        account_allocations=allocations(by_account, lambda k: account_labels.get(k, "Account")),
        security_type_allocations=allocations(by_type, lambda k: k),
        cash_allocation_percent=cash_allocation,
        warnings=warnings,
    )

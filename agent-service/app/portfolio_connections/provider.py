from __future__ import annotations

from abc import ABC, abstractmethod
from dataclasses import dataclass, field
from datetime import datetime
from decimal import Decimal


@dataclass(frozen=True)
class ProviderUserCredentials:
    """Provider user identity. The secret is never repr()'d, logged or returned."""

    provider_user_id: str
    user_secret: str = field(repr=False)


@dataclass(frozen=True)
class ConnectionPortal:
    portal_url: str = field(repr=False)  # short-lived; never persisted or logged
    expires_at: datetime


@dataclass(frozen=True)
class ProviderConnection:
    provider_connection_id: str
    institution_name: str
    disabled: bool


@dataclass(frozen=True)
class ProviderAccount:
    provider_account_id: str
    provider_connection_id: str | None
    display_name: str
    raw_type: str | None
    account_number: str | None = field(repr=False, default=None)
    institution_name: str | None = None
    currency: str | None = None
    status: str | None = None


@dataclass(frozen=True)
class ProviderBalance:
    currency: str
    amount: Decimal = field(repr=False)


@dataclass(frozen=True)
class ProviderPosition:
    provider_position_id: str | None
    provider_security_id: str | None
    broker_symbol_id: str | None
    symbol: str | None
    security_name: str | None
    exchange: str | None
    mic: str | None
    currency: str | None
    security_type_code: str | None
    quantity: Decimal = field(repr=False, default=Decimal(0))
    price: Decimal | None = field(repr=False, default=None)
    average_cost: Decimal | None = field(repr=False, default=None)


class PortfolioConnectionProvider(ABC):
    """Read-only provider contract.

    Deliberately has no order, trade, transfer, deposit, withdrawal or
    rebalancing methods, and none may be added in this milestone.
    """

    @abstractmethod
    async def create_provider_user(self, provider_user_id: str) -> ProviderUserCredentials: ...

    @abstractmethod
    async def create_connection_portal(
        self,
        user: ProviderUserCredentials,
        *,
        reconnect_provider_connection_id: str | None = None,
    ) -> ConnectionPortal: ...

    @abstractmethod
    async def get_connections(self, user: ProviderUserCredentials) -> list[ProviderConnection]: ...

    @abstractmethod
    async def get_accounts(self, user: ProviderUserCredentials) -> list[ProviderAccount]: ...

    @abstractmethod
    async def get_balances(
        self, user: ProviderUserCredentials, provider_account_id: str
    ) -> list[ProviderBalance]: ...

    @abstractmethod
    async def get_positions(
        self, user: ProviderUserCredentials, provider_account_id: str
    ) -> list[ProviderPosition]: ...

    @abstractmethod
    async def refresh_connection(
        self, user: ProviderUserCredentials, provider_connection_id: str
    ) -> None: ...

    @abstractmethod
    async def disconnect_connection(
        self, user: ProviderUserCredentials, provider_connection_id: str
    ) -> None: ...

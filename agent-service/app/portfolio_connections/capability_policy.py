from __future__ import annotations

from enum import Enum

from app.portfolio_connections.errors import PortfolioConnectionError, forbidden_capability


class Capability(str, Enum):
    # Allowed (read-only)
    CREATE_READ_CONNECTION = "CREATE_READ_CONNECTION"
    READ_CONNECTION_STATUS = "READ_CONNECTION_STATUS"
    READ_ACCOUNTS = "READ_ACCOUNTS"
    READ_BALANCES = "READ_BALANCES"
    READ_POSITIONS = "READ_POSITIONS"
    REFRESH_CONNECTION = "REFRESH_CONNECTION"
    DISCONNECT_CONNECTION = "DISCONNECT_CONNECTION"
    DELETE_IMPORTED_DATA = "DELETE_IMPORTED_DATA"

    # Forbidden. These exist ONLY so the policy can name and reject them.
    # No provider method, DTO, route or tool is ever implemented for them.
    PLACE_ORDER = "PLACE_ORDER"
    CANCEL_ORDER = "CANCEL_ORDER"
    REPLACE_ORDER = "REPLACE_ORDER"
    TRANSFER = "TRANSFER"
    DEPOSIT = "DEPOSIT"
    WITHDRAW = "WITHDRAW"
    TRADE_OPTIONS = "TRADE_OPTIONS"
    TRADE_CRYPTO = "TRADE_CRYPTO"


class PortfolioProviderCapabilityPolicy:
    ALLOWED = frozenset(
        {
            Capability.CREATE_READ_CONNECTION,
            Capability.READ_CONNECTION_STATUS,
            Capability.READ_ACCOUNTS,
            Capability.READ_BALANCES,
            Capability.READ_POSITIONS,
            Capability.REFRESH_CONNECTION,
            Capability.DISCONNECT_CONNECTION,
            Capability.DELETE_IMPORTED_DATA,
        }
    )
    FORBIDDEN = frozenset(set(Capability) - set(ALLOWED))

    @classmethod
    def is_allowed(cls, capability: Capability) -> bool:
        return capability in cls.ALLOWED

    @classmethod
    def require(cls, capability: Capability) -> None:
        if capability not in cls.ALLOWED:
            raise forbidden_capability()


__all__ = ["Capability", "PortfolioProviderCapabilityPolicy", "PortfolioConnectionError"]

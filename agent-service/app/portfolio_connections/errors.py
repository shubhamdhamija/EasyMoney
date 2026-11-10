from __future__ import annotations


class PortfolioConnectionError(Exception):
    """Safe, client-facing failure: stable code + message only."""

    def __init__(self, *, code: str, safe_message: str, status_code: int) -> None:
        super().__init__(code)
        self.code = code
        self.safe_message = safe_message
        self.status_code = status_code


def _err(code: str, message: str, status: int) -> PortfolioConnectionError:
    return PortfolioConnectionError(code=code, safe_message=message, status_code=status)


def snaptrade_disabled() -> PortfolioConnectionError:
    return _err(
        "SNAPTRADE_DISABLED",
        "Automatic portfolio connection is currently unavailable.",
        503,
    )


def invalid_request() -> PortfolioConnectionError:
    return _err("INVALID_REQUEST", "The request is not valid.", 400)


def connection_not_found() -> PortfolioConnectionError:
    # Non-disclosing: identical for unknown, cross-user and cross-tenant.
    return _err("CONNECTION_NOT_FOUND", "This portfolio connection is no longer available.", 404)


def session_not_found() -> PortfolioConnectionError:
    return _err("SESSION_NOT_FOUND", "This portfolio connection is no longer available.", 404)


def session_expired() -> PortfolioConnectionError:
    return _err("SESSION_EXPIRED", "The portfolio connection session expired.", 410)


def session_consumed() -> PortfolioConnectionError:
    return _err("SESSION_CONSUMED", "The portfolio connection session was already used.", 409)


def connection_incomplete() -> PortfolioConnectionError:
    return _err("CONNECTION_INCOMPLETE", "The demo brokerage was not connected.", 409)


def reconnect_required() -> PortfolioConnectionError:
    return _err("RECONNECT_REQUIRED", "Reconnect the portfolio to continue updating it.", 409)


def sync_in_progress() -> PortfolioConnectionError:
    return _err("SYNC_IN_PROGRESS", "A portfolio update is already running.", 409)


def sync_timeout() -> PortfolioConnectionError:
    return _err("SYNC_TIMEOUT", "The portfolio update timed out.", 504)


def provider_unavailable() -> PortfolioConnectionError:
    return _err(
        "PROVIDER_UNAVAILABLE",
        "Portfolio synchronization is temporarily unavailable.",
        503,
    )


def provider_rate_limited() -> PortfolioConnectionError:
    return _err(
        "PROVIDER_RATE_LIMITED",
        "Portfolio synchronization is temporarily unavailable.",
        503,
    )


def no_snapshot() -> PortfolioConnectionError:
    return _err("NO_SNAPSHOT", "No synchronized portfolio is available yet.", 404)


def forbidden_capability() -> PortfolioConnectionError:
    return _err("CAPABILITY_FORBIDDEN", "This operation is not supported.", 403)

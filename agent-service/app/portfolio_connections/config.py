from __future__ import annotations

from dataclasses import dataclass, field

from pydantic import SecretStr

from app.config.settings import Settings
from app.portfolio_connections.errors import snaptrade_disabled


@dataclass(frozen=True)
class SnapTradeConfig:
    client_id: str
    consumer_key: SecretStr = field(repr=False)
    encryption_key: SecretStr = field(repr=False)
    environment: str
    redirect_uri: str
    connection_type: str
    broker: str
    timeout_seconds: float


def assert_read_only_configuration(settings: Settings) -> None:
    """Startup assertion. Fails fast even while the feature is disabled."""
    if settings.snaptrade_connection_type.strip().lower() != "read":
        raise RuntimeError("SNAPTRADE_CONNECTION_TYPE must be 'read'")
    if settings.snaptrade_environment.strip().lower() != "sandbox":
        raise RuntimeError("SNAPTRADE_ENVIRONMENT must be 'sandbox' in this milestone")
    if settings.snaptrade_broker.strip().upper() != "SANDBOX":
        raise RuntimeError("SNAPTRADE_BROKER must be 'SANDBOX' in this milestone")


def load_snaptrade_config(settings: Settings) -> SnapTradeConfig:
    """Return the validated config, or raise SNAPTRADE_DISABLED (503).

    Missing credentials are reported as "disabled" so no detail about
    configuration reaches the client.
    """
    assert_read_only_configuration(settings)
    if not settings.snaptrade_is_enabled:
        raise snaptrade_disabled()
    consumer_key = settings.snaptrade_consumer_key
    encryption_key = settings.snaptrade_encryption_key
    if (
        not settings.snaptrade_client_id.strip()
        or consumer_key is None
        or not consumer_key.get_secret_value().strip()
        or encryption_key is None
        or not encryption_key.get_secret_value().strip()
    ):
        raise snaptrade_disabled()
    return SnapTradeConfig(
        client_id=settings.snaptrade_client_id.strip(),
        consumer_key=consumer_key,
        encryption_key=encryption_key,
        environment="sandbox",
        redirect_uri=settings.snaptrade_redirect_uri.strip(),
        connection_type="read",
        broker="SANDBOX",
        timeout_seconds=float(settings.snaptrade_timeout_seconds),
    )

from __future__ import annotations

from functools import lru_cache

from pydantic import Field, SecretStr
from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    """Environment-driven configuration.

    GROQ_API_KEY is a SecretStr so it is masked in repr() and logs.
    """

    model_config = SettingsConfigDict(
        env_file=".env",
        env_file_encoding="utf-8",
        extra="ignore",
    )

    agent_live_calls_enabled: str = "false"
    # Use real quote/news providers instead of the fixed fake data set.
    agent_live_market_data: str = "false"
    groq_api_key: SecretStr | None = None
    groq_base_url: str = "https://api.groq.com/openai/v1"
    groq_model: str = "openai/gpt-oss-20b"
    groq_timeout_seconds: float = Field(default=30, gt=0, le=300)

    agent_jwt_secret: SecretStr | None = Field(
        default=SecretStr("test-only-jwt-secret-not-for-production"),
    )
    agent_jwt_issuer: str = "easy-money-agent"
    agent_jwt_audience: str = "easy-money-agent"
    agent_jwt_algorithm: str = "HS256"

    agent_approval_ttl_seconds: int = 900
    agent_workflow_ttl_seconds: int = 86400
    agent_completed_retention_seconds: int = 604800

    # SnapTrade Sandbox (development-only, read-only). Disabled by default.
    # Secrets are SecretStr so they are masked in repr() and logs.
    snaptrade_enabled: str = "false"
    snaptrade_client_id: str = ""
    snaptrade_consumer_key: SecretStr | None = None
    snaptrade_environment: str = "sandbox"
    snaptrade_redirect_uri: str = "easymoney://portfolio/snaptrade"
    snaptrade_connection_type: str = "read"
    snaptrade_broker: str = "SANDBOX"
    snaptrade_timeout_seconds: float = Field(default=20, gt=0, le=120)
    # Fernet key (urlsafe base64, 32 bytes). Encrypts provider user secrets at rest
    # and keys the opaque provider-user-ID hash.
    snaptrade_encryption_key: SecretStr | None = None
    portfolio_db_path: str = ""

    @property
    def live_market_data_enabled(self) -> bool:
        return self.agent_live_market_data == "true"

    @property
    def live_calls_enabled(self) -> bool:
        # Only the exact lower-case string "true" enables live calls.
        return self.agent_live_calls_enabled == "true"

    @property
    def snaptrade_is_enabled(self) -> bool:
        return self.snaptrade_enabled == "true"


@lru_cache
def get_settings() -> Settings:
    return Settings()

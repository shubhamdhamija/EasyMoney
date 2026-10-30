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
    groq_api_key: SecretStr | None = None
    groq_base_url: str = "https://api.groq.com/openai/v1"
    groq_model: str = "openai/gpt-oss-20b"
    groq_timeout_seconds: float = Field(default=30, gt=0, le=300)

    @property
    def live_calls_enabled(self) -> bool:
        # Only the exact lower-case string "true" enables live calls.
        return self.agent_live_calls_enabled == "true"


@lru_cache
def get_settings() -> Settings:
    return Settings()


from app.config.settings import Settings


def make_settings(**overrides) -> Settings:
    return Settings(_env_file=None, **overrides)


def clear_env(monkeypatch) -> None:
    for name in (
        "AGENT_LIVE_CALLS_ENABLED",
        "GROQ_API_KEY",
        "GROQ_BASE_URL",
        "GROQ_MODEL",
        "GROQ_TIMEOUT_SECONDS",
    ):
        monkeypatch.delenv(name, raising=False)


def test_defaults(monkeypatch) -> None:
    clear_env(monkeypatch)

    settings = make_settings()

    assert settings.agent_live_calls_enabled == "false"
    assert settings.live_calls_enabled is False
    assert settings.groq_api_key is None
    assert settings.groq_base_url == "https://api.groq.com/openai/v1"
    assert settings.groq_model == "openai/gpt-oss-20b"
    assert settings.groq_timeout_seconds == 30


def test_reads_environment(monkeypatch) -> None:
    clear_env(monkeypatch)
    monkeypatch.setenv("AGENT_LIVE_CALLS_ENABLED", "true")
    monkeypatch.setenv("GROQ_API_KEY", "test-key")
    monkeypatch.setenv("GROQ_BASE_URL", "https://example.test/v1")
    monkeypatch.setenv("GROQ_MODEL", "some-model")
    monkeypatch.setenv("GROQ_TIMEOUT_SECONDS", "12.5")

    settings = make_settings()

    assert settings.live_calls_enabled is True
    assert settings.groq_api_key.get_secret_value() == "test-key"
    assert settings.groq_base_url == "https://example.test/v1"
    assert settings.groq_model == "some-model"
    assert settings.groq_timeout_seconds == 12.5


def test_live_calls_require_exact_lowercase_true(monkeypatch) -> None:
    clear_env(monkeypatch)

    for value in ("True", "TRUE", "1", "yes", " true", "true ", ""):
        assert make_settings(agent_live_calls_enabled=value).live_calls_enabled is False

    assert make_settings(agent_live_calls_enabled="true").live_calls_enabled is True


def test_api_key_is_not_exposed_in_repr(monkeypatch) -> None:
    clear_env(monkeypatch)

    settings = make_settings(groq_api_key="super-secret-value")

    assert "super-secret-value" not in repr(settings)
    assert "super-secret-value" not in str(settings)


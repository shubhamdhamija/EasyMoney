import sys
from pathlib import Path

PROJECT_ROOT = Path(__file__).resolve().parents[1]
if str(PROJECT_ROOT) not in sys.path:
    sys.path.insert(0, str(PROJECT_ROOT))



import pytest


@pytest.fixture(autouse=True)
def no_live_agent_calls(monkeypatch):
    """Guarantee automated tests never pick up real credentials or enable live calls."""
    from app.config.settings import get_settings

    monkeypatch.setenv("AGENT_LIVE_CALLS_ENABLED", "false")
    monkeypatch.delenv("GROQ_API_KEY", raising=False)
    get_settings.cache_clear()
    yield
    get_settings.cache_clear()

import pytest

from app.errors.decision_provider_error import DecisionProviderError
from app.models.research_state import ResearchState
from app.services.disabled_decision_model import DisabledDecisionModel


def test_provider_error_fields_and_safe_string() -> None:
    cause = RuntimeError("secret internal detail")
    error = DecisionProviderError(
        code="DECISION_PROVIDER_TIMEOUT",
        safe_message="The decision provider timed out.",
        status_code=504,
        provider_status=None,
        cause=cause,
    )

    assert error.code == "DECISION_PROVIDER_TIMEOUT"
    assert error.safe_message == "The decision provider timed out."
    assert error.status_code == 504
    assert error.provider_status is None
    assert error.cause is cause
    assert str(error) == "The decision provider timed out."
    assert "secret internal detail" not in str(error)


@pytest.mark.asyncio
async def test_disabled_decision_model_raises_safe_error() -> None:
    with pytest.raises(DecisionProviderError) as raised:
        await DisabledDecisionModel().decide(ResearchState(symbol="AAPL"), [])

    assert raised.value.code == "AGENT_LIVE_CALLS_DISABLED"
    assert raised.value.status_code == 503
    assert raised.value.safe_message == "Agent research is currently disabled."


class AgentConfigurationError(Exception):
    """Invalid agent configuration. The message is safe to expose."""

    code = "AGENT_CONFIGURATION_INVALID"

    def __init__(self, safe_message: str) -> None:
        super().__init__(safe_message)
        self.safe_message = safe_message


from typing import Any, Literal

from pydantic import BaseModel, Field, model_validator


class PlannerDecision(BaseModel):
    action: Literal[
        "call_tool",
        "finish",
    ]

    tool_name: str | None = None

    arguments: dict[str, Any] = Field(default_factory=dict)

    reason: str = Field(min_length=1, max_length=500)

    @model_validator(mode="after")
    def validate_action_fields(self) -> "PlannerDecision":
        if self.action == "call_tool":
            if self.tool_name is None or not self.tool_name.strip():
                raise ValueError(
                    "tool_name is required when action is call_tool"
                )

        if self.action == "finish":
            if self.tool_name is not None:
                raise ValueError(
                    "tool_name must be absent when action is finish"
                )

            if self.arguments:
                raise ValueError(
                    "arguments must be empty when action is finish"
                )

        return self


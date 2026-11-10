import hashlib
import json
from typing import Any

from pydantic import ValidationError

from app.errors.portfolio_agent_error import consent_required
from app.models.tool_definition import ToolDefinition
from app.models.tool_execution_context import ToolExecutionContext
from app.tools.approved_execution import ApprovedExecutionContext
from app.tools.tool_errors import ToolError


class ToolRegistry:
    def __init__(self) -> None:
        self._tools: dict[str, Any] = {}

    def register(self, tool: Any) -> None:
        self._tools[tool.name] = tool

    async def execute(
        self,
        tool_name: str,
        arguments: dict[str, Any],
        *,
        approval: ApprovedExecutionContext | None = None,
        context: ToolExecutionContext | None = None,
    ) -> dict[str, Any]:
        tool = self._tools.get(tool_name)

        if tool is None:
            return {
                "ok": False,
                "error": {
                    "code": "UNKNOWN_TOOL",
                    "message": "The requested tool is not available.",
                },
            }

        # Approval comes only from the trusted context object, never from the
        # model-controlled arguments.
        if self.requires_approval(tool_name) and not (
            isinstance(approval, ApprovedExecutionContext)
            and approval.tool_name == tool_name
        ):
            return {
                "ok": False,
                "error": {
                    "code": "APPROVAL_REQUIRED",
                    "message": "This tool requires explicit human approval.",
                },
            }

        # Read-only does not mean public: portfolio tools need the trusted,
        # application-issued context (authenticated principal + consent).
        needs_context = getattr(tool, "requires_trusted_context", False) is True
        if needs_context and not (
            isinstance(context, ToolExecutionContext)
            and context.authenticated
            and context.portfolio_consent_granted
        ):
            denied = consent_required()
            return {"ok": False, "error": {"code": denied.code, "message": denied.message}}

        try:
            validated_input = tool.input_model.model_validate(arguments)
        except ValidationError:
            return {
                "ok": False,
                "error": {
                    "code": "INVALID_TOOL_ARGUMENTS",
                    "message": "The tool arguments are invalid.",
                },
            }

        try:
            if needs_context:
                result = await tool.execute(validated_input, context)
            else:
                result = await tool.execute(validated_input)
        except ToolError as error:
            return {
                "ok": False,
                "error": {
                    "code": error.code,
                    "message": error.message,
                },
            }

        return {
            "ok": True,
            "result": result.model_dump(mode="json"),
        }

    def validate_arguments(
        self,
        tool_name: str,
        arguments: dict[str, Any],
    ) -> dict[str, Any] | None:
        """Normalized arguments, or None if the tool is unknown or input is invalid."""
        tool = self._tools.get(tool_name)

        if tool is None:
            return None

        try:
            return tool.input_model.model_validate(arguments).model_dump(mode="json")
        except ValidationError:
            return None

    def requires_approval(self, tool_name: str) -> bool:
        tool = self._tools.get(tool_name)
        return tool is not None and getattr(tool, "requires_approval", False) is True

    def contains(self, tool_name: str) -> bool:
        normalized_name = tool_name.strip().lower()
        return normalized_name in self._tools

    def available_tool_names(self) -> list[str]:
        return sorted(self._tools.keys())

    def tool_definitions(self) -> list[ToolDefinition]:
        return [
            ToolDefinition(
                name=tool.name,
                description=tool.description,
                input_schema=tool.input_model.model_json_schema(),
                requires_approval=getattr(tool, "requires_approval", False) is True,
            )
            for _, tool in sorted(self._tools.items())
        ]

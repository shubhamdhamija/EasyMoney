from typing import Any

from pydantic import ValidationError

from app.models.tool_definition import ToolDefinition
from app.tools.tool_errors import ToolError


class ToolRegistry:
    def __init__(self) -> None:
        self._tools: dict[str, Any] = {}

    def register(self, tool: Any) -> None:
        self._tools[tool.name] = tool

    async def execute(self, tool_name: str, arguments: dict[str, Any]) -> dict[str, Any]:
        tool = self._tools.get(tool_name)

        if tool is None:
            return {
                "ok": False,
                "error": {
                    "code": "UNKNOWN_TOOL",
                    "message": "The requested tool is not available.",
                },
            }

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
            )
            for _, tool in sorted(self._tools.items())
        ]

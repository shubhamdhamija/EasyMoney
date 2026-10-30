from typing import Any

from pydantic import BaseModel, Field


class ToolDefinition(BaseModel):
    """Model-visible description of one approved tool.

    Deliberately contains no module names, paths, or callables.
    """

    name: str = Field(min_length=1)
    description: str = Field(min_length=1)
    input_schema: dict[str, Any]


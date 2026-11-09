from typing import Any

from pydantic import BaseModel, Field


class ToolDefinition(BaseModel):
    """Model-visible description of one approved tool.

    Deliberately contains no module names, paths, or callables.
    """

    name: str = Field(min_length=1)
    description: str = Field(min_length=1)
    input_schema: dict[str, Any]
    # Set by the application from the registered tool. A model can neither
    # supply nor override it.
    requires_approval: bool = False

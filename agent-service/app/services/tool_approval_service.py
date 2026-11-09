import uuid
from datetime import datetime, timezone
from typing import Any

from pydantic import BaseModel, Field

from app.models.tool_execution_context import ToolExecutionContext
from app.services.tool_approval_hash import build_tool_arguments_hash


class PreparedToolApproval(BaseModel):
    approval_id: str = Field(min_length=1, max_length=64)
    tool_name: str = Field(min_length=1)
    arguments_hash: str = Field(min_length=64, max_length=64)
    safe_arguments: dict[str, Any]
    created_at: datetime


class ToolApprovalService:
    def prepare(
        self,
        *,
        tool_name: str,
        validated_arguments: dict[str, Any],
    ) -> PreparedToolApproval:
        safe_arguments: dict[str, Any] = {}
        if tool_name == "save_research_report":
            safe_arguments = {
                "symbol": str(validated_arguments.get("symbol", "")).strip().upper(),
                "report_summary": str(validated_arguments.get("report_summary", "")).strip()[:2000],
            }

        approval_id = str(uuid.uuid4())
        arguments_hash = build_tool_arguments_hash(
            tool_name=tool_name,
            arguments=safe_arguments,
        )
        return PreparedToolApproval(
            approval_id=approval_id,
            tool_name=tool_name,
            arguments_hash=arguments_hash,
            safe_arguments=safe_arguments,
            created_at=datetime.now(timezone.utc),
        )

    def approve(
        self,
        *,
        prepared: PreparedToolApproval,
    ) -> ToolExecutionContext:
        return ToolExecutionContext(
            approval_granted=True,
            approval_id=prepared.approval_id,
            approved_tool_name=prepared.tool_name,
            approved_arguments_hash=prepared.arguments_hash,
        )

import hashlib
import json
from datetime import datetime
from typing import Any

from pydantic import BaseModel, ConfigDict, Field, StrictBool

# Longest report summary shown to a reviewer.
MAXIMUM_DISPLAY_SUMMARY_LENGTH = 500


class ToolApprovalRequest(BaseModel):
    """What a human reviewer sees. Built only by application code."""

    approval_id: str = Field(min_length=1, max_length=64)
    thread_id: str = Field(min_length=1, max_length=64)
    tool_name: str = Field(min_length=1)
    safe_arguments: dict[str, Any]
    reason: str = Field(min_length=1, max_length=500)
    created_at: datetime


class ToolApprovalDecision(BaseModel):
    """The only thing a human may submit: approve or reject the prepared request.

    Extra fields are forbidden, so a reviewer cannot replace the tool name,
    arguments, thread ID, or approval ID.
    """

    model_config = ConfigDict(extra="forbid")

    approval_id: str = Field(min_length=1, max_length=64)
    approved: StrictBool
    reviewer_note: str | None = Field(default=None, max_length=500)


class PendingApproval(BaseModel):
    """Checkpointed record of one prepared, not-yet-executed write."""

    request: ToolApprovalRequest
    prepared_arguments: dict[str, Any]
    fingerprint: str = Field(min_length=64, max_length=64)


def fingerprint_tool_call(tool_name: str, arguments: dict[str, Any]) -> str:
    canonical = json.dumps(
        {"tool_name": tool_name, "arguments": arguments},
        sort_keys=True,
        separators=(",", ":"),
        default=str,
    )
    return hashlib.sha256(canonical.encode("utf-8")).hexdigest()


def project_safe_arguments(tool_name: str, arguments: dict[str, Any]) -> dict[str, Any]:
    """Deterministic allowlist of fields that may be shown to a reviewer."""
    if tool_name == "save_research_report":
        summary = str(arguments.get("report_summary", ""))
        return {
            "symbol": str(arguments.get("symbol", "")),
            "report_summary": summary[:MAXIMUM_DISPLAY_SUMMARY_LENGTH],
        }

    return {}


def approval_reason(tool_name: str) -> str:
    """Application-written reason. Model-provided reasons are never displayed."""
    if tool_name == "save_research_report":
        return "Save the completed research report."

    return "This action requires human approval."


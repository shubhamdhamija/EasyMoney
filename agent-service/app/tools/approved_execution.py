"""Trusted execution context for approval-required tools.

Only application code that has validated a human approval may create one. It is
passed to ToolRegistry.execute as a separate parameter, never inside the
model-controlled arguments, so no model output can forge it.
"""

from dataclasses import dataclass, field

_ISSUER = object()


@dataclass(frozen=True)
class ApprovedExecutionContext:
    tool_name: str
    approval_id: str
    _issuer: object = field(default=None, repr=False, compare=False)

    def __post_init__(self) -> None:
        if self._issuer is not _ISSUER:
            raise PermissionError(
                "ApprovedExecutionContext can only be issued by application code."
            )


def issue_approved_execution_context(
    *,
    tool_name: str,
    approval_id: str,
) -> ApprovedExecutionContext:
    """Call only after a human approval has been validated and consumed."""
    return ApprovedExecutionContext(
        tool_name=tool_name,
        approval_id=approval_id,
        _issuer=_ISSUER,
    )


class ApprovalError(Exception):
    """Safe, client-facing approval failure.

    Carries a stable code and message only. It never includes the thread ID,
    approval ID, tool arguments, or checkpoint contents.
    """

    def __init__(self, *, code: str, safe_message: str, status_code: int) -> None:
        super().__init__(code)
        self.code = code
        self.safe_message = safe_message
        self.status_code = status_code


def unknown_thread() -> ApprovalError:
    return ApprovalError(
        code="UNKNOWN_THREAD",
        safe_message="The requested workflow was not found.",
        status_code=404,
    )


def unknown_approval() -> ApprovalError:
    return ApprovalError(
        code="UNKNOWN_APPROVAL",
        safe_message="The requested approval was not found.",
        status_code=404,
    )


def approval_already_used() -> ApprovalError:
    return ApprovalError(
        code="APPROVAL_ALREADY_USED",
        safe_message="This approval was already used or is no longer valid.",
        status_code=409,
    )


def invalid_approval() -> ApprovalError:
    return ApprovalError(
        code="INVALID_APPROVAL",
        safe_message="The approval request is malformed.",
        status_code=400,
    )


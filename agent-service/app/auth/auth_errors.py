from __future__ import annotations


class AuthError(Exception):
    def __init__(self, *, code: str, message: str, status_code: int) -> None:
        super().__init__(message)
        self.code = code
        self.message = message
        self.safe_message = message
        self.status_code = status_code


class AuthenticationRequiredError(AuthError):
    def __init__(self) -> None:
        super().__init__(
            code="AUTHENTICATION_REQUIRED",
            message="Authentication is required.",
            status_code=401,
        )


class InvalidAccessTokenError(AuthError):
    def __init__(self) -> None:
        super().__init__(
            code="INVALID_ACCESS_TOKEN",
            message="The access token is invalid or expired.",
            status_code=401,
        )


class InsufficientScopeError(AuthError):
    def __init__(self, required: str | None = None) -> None:
        scope_text = f" {required}" if required else ""
        super().__init__(
            code="INSUFFICIENT_SCOPE",
            message=f"The token is missing the required scope{scope_text}.",
            status_code=403,
        )


class WorkflowAccessDeniedError(AuthError):
    def __init__(self) -> None:
        super().__init__(
            code="WORKFLOW_ACCESS_DENIED",
            message="This workflow is not accessible for the current tenant and user.",
            status_code=403,
        )

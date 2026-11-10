"""Safe portfolio-agent errors.

Cross-user and cross-tenant access deliberately maps to the same non-disclosing
"not found" response as a missing portfolio.
"""

from app.tools.tool_errors import ToolError

_NOT_AVAILABLE = "This portfolio is no longer available."


class PortfolioAgentError(ToolError):
    """A ToolError (so the registry reports it safely) that also carries an HTTP status."""

    def __init__(self, *, code: str, message: str, status_code: int) -> None:
        super().__init__(code=code, message=message)
        self.status_code = status_code

    @property
    def safe_message(self) -> str:
        return self.message


def portfolio_not_found() -> PortfolioAgentError:
    return PortfolioAgentError(code="PORTFOLIO_NOT_FOUND", message=_NOT_AVAILABLE, status_code=404)


def portfolio_access_denied() -> PortfolioAgentError:
    # 404 on purpose: never disclose that another user's portfolio exists.
    return PortfolioAgentError(code="PORTFOLIO_ACCESS_DENIED", message=_NOT_AVAILABLE, status_code=404)


def consent_required() -> PortfolioAgentError:
    return PortfolioAgentError(
        code="PORTFOLIO_CONSENT_REQUIRED",
        message="Confirm portfolio research before continuing.",
        status_code=403,
    )


def consent_expired() -> PortfolioAgentError:
    return PortfolioAgentError(
        code="PORTFOLIO_CONSENT_EXPIRED",
        message="Portfolio research permission expired. Confirm again to continue.",
        status_code=403,
    )


def portfolio_empty() -> PortfolioAgentError:
    return PortfolioAgentError(
        code="PORTFOLIO_EMPTY",
        message="No portfolio positions are available for research.",
        status_code=409,
    )


def context_too_large() -> PortfolioAgentError:
    return PortfolioAgentError(
        code="PORTFOLIO_CONTEXT_TOO_LARGE",
        message="Select fewer portfolio positions and try again.",
        status_code=413,
    )


def currency_required() -> PortfolioAgentError:
    return PortfolioAgentError(
        code="PORTFOLIO_CURRENCY_REQUIRED",
        message="Choose one currency for concentration analysis.",
        status_code=422,
    )

def invalid_context() -> PortfolioAgentError:
    return PortfolioAgentError(
        code="PORTFOLIO_CONTEXT_INVALID",
        message="The portfolio information could not be used. Check it and try again.",
        status_code=422,
    )

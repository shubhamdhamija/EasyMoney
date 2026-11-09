import logging

from fastapi import FastAPI, HTTPException, Request
from fastapi.responses import JSONResponse

from app.api.agent_research_routes import router as agent_research_router
from app.api.congress_routes import router as congress_router
from app.api.research_routes import router as research_router
from app.errors.agent_configuration_error import AgentConfigurationError
from app.errors.approval_error import ApprovalError
from app.errors.decision_provider_error import DecisionProviderError
from app.middleware.request_id import REQUEST_ID_HEADER, RequestIdMiddleware
from app.auth.auth_errors import AuthError
from app.auth.auth_models import AuthenticatedPrincipal

logger = logging.getLogger(__name__)


app = FastAPI(
    title="EasyMoney Agent Service",
    version="0.1.0",
)

app.add_middleware(RequestIdMiddleware)


def _request_id(request: Request) -> str | None:
    return request.scope.get("state", {}).get("request_id")


def _error_response(
    request: Request,
    *,
    status_code: int,
    code: str,
    message: str,
) -> JSONResponse:
    request_id = _request_id(request)
    headers = {REQUEST_ID_HEADER: request_id} if request_id else None

    # Only the stable code, a safe message, and the opaque request ID reach the
    # client. The request ID carries no meaning; it only correlates with logs.
    return JSONResponse(
        status_code=status_code,
        content={
            "error": {"code": code, "message": message},
            "request_id": request_id,
        },
        headers=headers,
    )


@app.exception_handler(DecisionProviderError)
async def handle_decision_provider_error(
    request: Request,
    error: DecisionProviderError,
) -> JSONResponse:
    logger.warning(
        "decision_provider_error request_id=%s code=%s provider_status=%s",
        _request_id(request),
        error.code,
        error.provider_status,
    )
    return _error_response(
        request,
        status_code=error.status_code,
        code=error.code,
        message=error.safe_message,
    )


@app.exception_handler(AgentConfigurationError)
async def handle_agent_configuration_error(
    request: Request,
    error: AgentConfigurationError,
) -> JSONResponse:
    logger.warning(
        "agent_configuration_error request_id=%s code=%s",
        _request_id(request),
        error.code,
    )
    return _error_response(
        request,
        status_code=503,
        code=error.code,
        message=error.safe_message,
    )


@app.exception_handler(ApprovalError)
async def handle_approval_error(request: Request, error: ApprovalError) -> JSONResponse:
    # Log the stable code only: never thread IDs, approval IDs, or arguments.
    logger.warning(
        "approval_error request_id=%s code=%s",
        _request_id(request),
        error.code,
    )
    return _error_response(
        request,
        status_code=error.status_code,
        code=error.code,
        message=error.safe_message,
    )


@app.exception_handler(AuthError)
async def handle_auth_error(request: Request, error: AuthError) -> JSONResponse:
    # Log the stable code only: never thread IDs, approval IDs, or arguments.
    logger.warning(
        "auth_error request_id=%s code=%s",
        _request_id(request),
        error.code,
    )
    return _error_response(
        request,
        status_code=error.status_code,
        code=error.code,
        message=error.safe_message,
    )


@app.exception_handler(Exception)
async def handle_unexpected_error(request: Request, error: Exception) -> JSONResponse:
    # Log the exception type only: messages can embed prompts or provider data.
    logger.error(
        "unexpected_error request_id=%s type=%s",
        _request_id(request),
        type(error).__name__,
    )
    return _error_response(
        request,
        status_code=500,
        code="INTERNAL_ERROR",
        message="An unexpected error occurred.",
    )


@app.get("/health")
async def health() -> dict[str, str]:
    return {
        "status": "healthy",
        "service": "easymoney-agent-service",
    }


app.include_router(research_router)
app.include_router(agent_research_router)
app.include_router(congress_router)

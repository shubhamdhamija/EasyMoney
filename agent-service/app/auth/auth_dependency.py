from __future__ import annotations

from collections.abc import Callable
from typing import Any

from fastapi import Depends, HTTPException, Request

from app.auth.auth_errors import (
    AuthenticationRequiredError,
    InsufficientScopeError,
    InvalidAccessTokenError,
    WorkflowAccessDeniedError,
)
from app.auth.auth_models import AuthenticatedPrincipal
from app.auth.jwt_service import JwtService
from app.config.settings import get_settings


class AuthContext:
    def __init__(self, principal: AuthenticatedPrincipal) -> None:
        self.principal = principal


def _jwt_service() -> JwtService:
    settings = get_settings()
    secret = settings.agent_jwt_secret
    if secret is None or not secret.get_secret_value().strip():
        raise RuntimeError("AGENT_JWT_SECRET is not configured")
    return JwtService(
        secret=secret,
        issuer=settings.agent_jwt_issuer,
        audience=settings.agent_jwt_audience,
        algorithm=settings.agent_jwt_algorithm,
    )


def _extract_bearer_token(request: Request) -> str:
    auth_header = request.headers.get("Authorization")
    if auth_header is None:
        raise AuthenticationRequiredError()
    parts = auth_header.split(" ")
    if len(parts) != 2 or parts[0].lower() != "bearer" or not parts[1].strip():
        raise AuthenticationRequiredError()
    return parts[1].strip()


def require_auth(scopes: set[str] | None = None) -> Callable[..., AuthenticatedPrincipal]:
    async def dependency(request: Request) -> AuthenticatedPrincipal:
        try:
            token = _extract_bearer_token(request)
            principal = _jwt_service().decode(token)
        except AuthenticationRequiredError as exc:
            raise HTTPException(status_code=401, detail={"code": "AUTHENTICATION_REQUIRED"}) from exc
        except InvalidAccessTokenError as exc:
            raise HTTPException(status_code=401, detail={"code": "INVALID_ACCESS_TOKEN"}) from exc

        if scopes:
            missing = sorted(scopes - principal.scopes)
            if missing:
                raise HTTPException(status_code=403, detail={"code": "INSUFFICIENT_SCOPE"})

        request.state.principal = principal
        return principal

    return dependency


def require_workflow_owner(
    principal: AuthenticatedPrincipal,
    *,
    tenant_id: str,
    user_id: str,
) -> AuthenticatedPrincipal:
    if principal.tenant_id != tenant_id or principal.user_id != user_id:
        raise WorkflowAccessDeniedError()
    return principal

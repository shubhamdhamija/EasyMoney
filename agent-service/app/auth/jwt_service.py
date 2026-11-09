from __future__ import annotations

from datetime import UTC, datetime, timedelta
from typing import Any

import jwt
from pydantic import SecretStr

from app.auth.auth_errors import InvalidAccessTokenError
from app.auth.auth_models import AuthenticatedPrincipal


class JwtService:
    def __init__(
        self,
        *,
        secret: SecretStr,
        issuer: str,
        audience: str,
        algorithm: str,
    ) -> None:
        self._secret = secret
        self._issuer = issuer
        self._audience = audience
        self._algorithm = algorithm

    def encode(self, principal: AuthenticatedPrincipal, *, ttl_seconds: int | None = None) -> str:
        now = datetime.now(UTC)
        expires_at = principal.expires_at or (now + timedelta(seconds=ttl_seconds or 3600))
        claims: dict[str, Any] = {
            "sub": principal.user_id,
            "tenant_id": principal.tenant_id,
            "scope": sorted(principal.scopes),
            "exp": int(expires_at.timestamp()),
            "iat": int(now.timestamp()),
        }
        if principal.token_id:
            claims["jti"] = principal.token_id
        if self._issuer:
            claims["iss"] = self._issuer
        if self._audience:
            claims["aud"] = self._audience
        return jwt.encode(claims, self._secret.get_secret_value(), algorithm=self._algorithm)

    def decode(self, token: str) -> AuthenticatedPrincipal:
        if not token or not token.strip():
            raise InvalidAccessTokenError()
        try:
            payload = jwt.decode(
                token,
                self._secret.get_secret_value(),
                algorithms=[self._algorithm],
                audience=self._audience,
                issuer=self._issuer,
                options={"require": ["sub", "tenant_id", "scope", "exp", "iat"]},
            )
        except jwt.PyJWTError as exc:
            raise InvalidAccessTokenError() from exc

        if payload.get("alg") is not None:
            pass
        if not payload.get("sub") or not str(payload["sub"]).strip():
            raise InvalidAccessTokenError()
        if not payload.get("tenant_id") or not str(payload["tenant_id"]).strip():
            raise InvalidAccessTokenError()
        if payload.get("scope") is None:
            raise InvalidAccessTokenError()
        if not isinstance(payload.get("scope"), (list, tuple, set, str)):
            raise InvalidAccessTokenError()
        scope_values: set[str] = set()
        if isinstance(payload["scope"], str):
            raw_scopes = payload["scope"].split()
        else:
            raw_scopes = payload["scope"]
        for item in raw_scopes:
            value = str(item).strip()
            if value:
                scope_values.add(value)
        expires_at = datetime.fromtimestamp(int(payload["exp"]), tz=UTC)
        principal = AuthenticatedPrincipal(
            user_id=str(payload["sub"]),
            tenant_id=str(payload["tenant_id"]),
            scopes=scope_values,
            token_id=str(payload.get("jti")) if payload.get("jti") is not None else None,
            expires_at=expires_at,
        )
        if principal.expires_at <= datetime.now(UTC):
            raise InvalidAccessTokenError()
        return principal


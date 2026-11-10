"""Mint a short-lived DEVELOPMENT JWT (scopes: portfolio:connect portfolio:read).

Run from agent-service/ with the venv active:  python -m scripts.mint_dev_jwt
Prints a single `AGENT_DEV_JWT=...` line to paste into the git-ignored ../local.properties.
Uses the same JwtService and settings as the server. Never commit the output.
"""
from __future__ import annotations

import uuid
from datetime import UTC, datetime, timedelta

from app.auth.auth_models import AuthenticatedPrincipal
from app.auth.jwt_service import JwtService
from app.config.settings import Settings

TTL_MINUTES = 45


def main() -> None:
    s = Settings()
    svc = JwtService(
        secret=s.agent_jwt_secret,
        issuer=s.agent_jwt_issuer,
        audience=s.agent_jwt_audience,
        algorithm=s.agent_jwt_algorithm,
    )
    principal = AuthenticatedPrincipal(
        user_id="local-sandbox-user",
        tenant_id="local-development",
        scopes=frozenset({"portfolio:connect", "portfolio:read"}),
        expires_at=datetime.now(UTC) + timedelta(minutes=TTL_MINUTES),
        token_id=str(uuid.uuid4()),
    )
    print(f"AGENT_DEV_JWT={svc.encode(principal)}")


if __name__ == "__main__":
    main()

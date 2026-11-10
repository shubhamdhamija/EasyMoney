from __future__ import annotations

from typing import Any

from fastapi import APIRouter, Body, Depends, Query
from pydantic import BaseModel, ConfigDict, Field, ValidationError

from app.auth.auth_dependency import require_auth
from app.auth.auth_models import AuthenticatedPrincipal
from app.config.settings import get_settings
from app.persistence.database import initialize_database
from app.portfolio_connections.config import load_snaptrade_config
from app.portfolio_connections.errors import invalid_request
from app.portfolio_connections.service import PortfolioConnectionService
from app.portfolio_connections.snaptrade_provider import SnapTradePortfolioConnectionProvider
from app.request_context import get_request_id

SCOPE_CONNECT = "portfolio:connect"
SCOPE_READ = "portfolio:read"

router = APIRouter(prefix="/portfolio/connections", tags=["portfolio-connections"])


class CreateSessionRequest(BaseModel):
    # Only the redirect URI is accepted. Connection type, broker, user, tenant and
    # provider credentials are application-controlled and are rejected if sent.
    model_config = ConfigDict(extra="forbid")
    redirectUri: str = Field(min_length=1, max_length=256)


_service: PortfolioConnectionService | None = None


async def get_portfolio_connection_service() -> PortfolioConnectionService:
    """Async on purpose: the SQLite connection must live on the event-loop thread."""
    global _service
    config = load_snaptrade_config(get_settings())  # raises SNAPTRADE_DISABLED (503)
    if _service is None:
        settings = get_settings()
        db = initialize_database(settings.portfolio_db_path or None)
        _service = PortfolioConnectionService(
            config=config,
            provider=SnapTradePortfolioConnectionProvider(config),
            db=db,
        )
    return _service


def _with_request_id(body: dict[str, Any]) -> dict[str, Any]:
    return {**body, "requestId": get_request_id()}


@router.post("/snaptrade/session")
async def create_snaptrade_session(
    payload: dict[str, Any] = Body(...),
    principal: AuthenticatedPrincipal = Depends(require_auth({SCOPE_CONNECT})),
    service: PortfolioConnectionService = Depends(get_portfolio_connection_service),
) -> Any:
    try:
        request = CreateSessionRequest.model_validate(payload)
    except ValidationError:
        raise invalid_request() from None
    return _with_request_id(await service.create_session(principal, request.redirectUri))


@router.post("/snaptrade/{session_id}/complete")
async def complete_snaptrade_connection(
    session_id: str,
    principal: AuthenticatedPrincipal = Depends(require_auth({SCOPE_CONNECT})),
    service: PortfolioConnectionService = Depends(get_portfolio_connection_service),
) -> Any:
    return _with_request_id(await service.complete(principal, session_id))


@router.get("")
async def list_connections(
    principal: AuthenticatedPrincipal = Depends(require_auth({SCOPE_READ})),
    service: PortfolioConnectionService = Depends(get_portfolio_connection_service),
) -> Any:
    return _with_request_id({"connections": service.list_connections(principal)})


@router.get("/{connection_id}/portfolio")
async def get_connection_portfolio(
    connection_id: str,
    principal: AuthenticatedPrincipal = Depends(require_auth({SCOPE_READ})),
    service: PortfolioConnectionService = Depends(get_portfolio_connection_service),
) -> Any:
    return _with_request_id(service.get_portfolio(principal, connection_id))


@router.post("/{connection_id}/sync")
async def sync_connection(
    connection_id: str,
    principal: AuthenticatedPrincipal = Depends(require_auth({SCOPE_READ})),
    service: PortfolioConnectionService = Depends(get_portfolio_connection_service),
) -> Any:
    return _with_request_id(await service.sync(principal, connection_id))


@router.post("/{connection_id}/reconnect-session")
async def create_reconnect_session(
    connection_id: str,
    principal: AuthenticatedPrincipal = Depends(require_auth({SCOPE_CONNECT})),
    service: PortfolioConnectionService = Depends(get_portfolio_connection_service),
) -> Any:
    return _with_request_id(await service.create_reconnect_session(principal, connection_id))


@router.delete("/{connection_id}")
async def disconnect_connection(
    connection_id: str,
    principal: AuthenticatedPrincipal = Depends(require_auth({SCOPE_CONNECT})),
    service: PortfolioConnectionService = Depends(get_portfolio_connection_service),
) -> Any:
    await service.disconnect(principal, connection_id)
    return _with_request_id({"status": "DISCONNECTED"})


@router.delete("/{connection_id}/data")
async def delete_synchronized_data(
    connection_id: str,
    confirm: bool = Query(default=False),
    principal: AuthenticatedPrincipal = Depends(require_auth({SCOPE_CONNECT})),
    service: PortfolioConnectionService = Depends(get_portfolio_connection_service),
) -> Any:
    service.delete_data(principal, connection_id, confirm=confirm)
    return _with_request_id({"status": "DELETED"})

"""Process-level wiring for portfolio research (consent, local context, repository, audit)."""

from __future__ import annotations

from dataclasses import dataclass

from app.persistence.audit_repository import AuditRepository
from app.portfolio_connections.research_repository import SnapTradePortfolioResearchRepository
from app.portfolio_connections.store import PortfolioConnectionStore
from app.services.portfolio_audit import PortfolioAudit
from app.services.portfolio_consent_service import PortfolioConsentStore
from app.services.portfolio_research_repository import (
    CompositePortfolioResearchRepository,
    LocalPortfolioResearchRepository,
    LocalPortfolioStore,
    PortfolioResearchRepository,
)


@dataclass
class PortfolioResearchRuntime:
    repository: PortfolioResearchRepository
    consent_store: PortfolioConsentStore
    local_store: LocalPortfolioStore
    audit: PortfolioAudit


def build_runtime(
    *,
    snaptrade_repository: PortfolioResearchRepository,
    consent_store: PortfolioConsentStore | None = None,
    local_store: LocalPortfolioStore | None = None,
    audit: PortfolioAudit | None = None,
) -> PortfolioResearchRuntime:
    local = local_store or LocalPortfolioStore()
    return PortfolioResearchRuntime(
        repository=CompositePortfolioResearchRepository(
            snaptrade=snaptrade_repository, local=LocalPortfolioResearchRepository(local)
        ),
        consent_store=consent_store or PortfolioConsentStore(),
        local_store=local,
        audit=audit or PortfolioAudit(),
    )


_runtime: PortfolioResearchRuntime | None = None


async def get_portfolio_runtime() -> PortfolioResearchRuntime:
    """Async on purpose: the SQLite connection must live on the event-loop thread."""
    global _runtime
    if _runtime is None:
        from app.config.settings import get_settings
        from app.persistence.database import initialize_database

        db = initialize_database(get_settings().portfolio_db_path or None)
        _runtime = build_runtime(
            snaptrade_repository=SnapTradePortfolioResearchRepository(PortfolioConnectionStore(db)),
            audit=PortfolioAudit(AuditRepository(db)),
        )
    return _runtime

from typing import TYPE_CHECKING

from fastapi import FastAPI

from imdb_agent.web.concierge import create_concierge_router
from imdb_agent.web.health import create_health_router

if TYPE_CHECKING:
    from imdb_agent.concierge.service import ConciergeService


def create_web_app(
    *, service_name: str, version: str, concierge_service: ConciergeService
) -> FastAPI:
    """Create the inbound web Adapter without configuring outbound Adapters."""

    app = FastAPI(
        title="IMDb Clone Movie Concierge",
        version=version,
        docs_url=None,
        redoc_url=None,
        openapi_url=None,
    )
    app.include_router(create_health_router(service_name=service_name, version=version))
    app.include_router(create_concierge_router(concierge_service))
    return app

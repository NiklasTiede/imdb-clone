from fastapi import FastAPI

from imdb_agent.web.health import create_health_router


def create_web_app(*, service_name: str, version: str) -> FastAPI:
    """Create the inbound web Adapter without configuring outbound Adapters."""

    app = FastAPI(
        title="IMDb Clone Movie Concierge",
        version=version,
        docs_url=None,
        redoc_url=None,
        openapi_url=None,
    )
    app.include_router(create_health_router(service_name=service_name, version=version))
    return app

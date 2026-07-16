from __future__ import annotations

from typing import TYPE_CHECKING

from imdb_agent.adapters.http_observability import install_http_observability
from imdb_agent.adapters.logging import configure_logging
from imdb_agent.settings import Settings, load_settings
from imdb_agent.web.app import create_web_app

if TYPE_CHECKING:
    from fastapi import FastAPI


def create_app(settings: Settings | None = None) -> FastAPI:
    """Compose the process without import-time I/O or external dependencies."""

    resolved_settings = settings or load_settings()
    configure_logging(json_output=resolved_settings.json_logs)
    app = create_web_app(
        service_name=resolved_settings.service_name,
        version=resolved_settings.version,
    )
    install_http_observability(app, resolved_settings)
    return app

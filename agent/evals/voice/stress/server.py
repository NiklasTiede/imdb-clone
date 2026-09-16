"""Local stress-test process: real app, same settings, structured payload-safe logs."""

from __future__ import annotations

from typing import TYPE_CHECKING

import structlog

if TYPE_CHECKING:
    from fastapi import FastAPI

from popcorn_society_agent.adapters.logging import configure_logging
from popcorn_society_agent.bootstrap import create_app


def create_stress_app() -> FastAPI:
    app = create_app()
    configure_logging(json_output=True)
    structlog.get_logger().info("stress_backend_ready")
    return app

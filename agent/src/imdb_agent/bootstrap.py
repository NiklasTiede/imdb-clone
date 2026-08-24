from __future__ import annotations

import atexit
from typing import TYPE_CHECKING

from imdb_agent.adapters.agent_observability import create_agent_metrics
from imdb_agent.adapters.fakes import FakeConciergeRunner
from imdb_agent.adapters.http_observability import (
    create_http_metrics,
    install_http_observability,
)
from imdb_agent.adapters.logging import configure_logging
from imdb_agent.adapters.memory import InMemoryConversationStore, InMemoryCostLedger
from imdb_agent.adapters.profiling import configure_profiling
from imdb_agent.adapters.pydantic_ai_runner import PydanticAIConciergeRunner
from imdb_agent.adapters.telemetry import configure_telemetry
from imdb_agent.concierge.service import ConciergeService
from imdb_agent.settings import ModelBackend, Settings, load_runtime_secrets, load_settings
from imdb_agent.web.app import create_web_app

if TYPE_CHECKING:
    from fastapi import FastAPI

    from imdb_agent.concierge.ports import ConciergeRunner


def create_app(settings: Settings | None = None, runner: ConciergeRunner | None = None) -> FastAPI:
    """Compose the process without import-time I/O or external dependencies."""

    resolved_settings = settings or load_settings()
    configure_logging(json_output=resolved_settings.json_logs)
    profiling = configure_profiling(resolved_settings)
    telemetry = configure_telemetry(
        resolved_settings,
        profiling_enabled=profiling.enabled,
    )
    resolved_runner = runner or _create_runner(resolved_settings)
    http_metrics = create_http_metrics(resolved_settings)
    observer = create_agent_metrics(http_metrics.registry, resolved_settings)
    concierge_service = ConciergeService(
        runner=resolved_runner,
        conversations=InMemoryConversationStore(
            max_conversations=resolved_settings.max_conversations,
        ),
        cost_ledger=InMemoryCostLedger(
            project_limit_usd=resolved_settings.project_cost_limit_usd,
            per_run_limit_usd=resolved_settings.run_cost_limit_usd,
        ),
        observer=observer,
        max_concurrent_runs=resolved_settings.max_concurrent_runs,
    )
    app = create_web_app(
        service_name=resolved_settings.service_name,
        version=resolved_settings.version,
        concierge_service=concierge_service,
        allowed_hosts=tuple(resolved_settings.allowed_hosts),
        max_request_body_bytes=resolved_settings.max_request_body_bytes,
    )
    install_http_observability(app, resolved_settings, http_metrics)
    telemetry.instrument_app(app)
    if profiling.enabled:
        atexit.register(profiling.shutdown)
    if telemetry.enabled:
        atexit.register(telemetry.shutdown)
    return app


def _create_runner(settings: Settings) -> ConciergeRunner:
    if settings.model_backend is ModelBackend.FAKE:
        return FakeConciergeRunner()
    return PydanticAIConciergeRunner(
        settings=settings,
        secrets=load_runtime_secrets(settings),
    )

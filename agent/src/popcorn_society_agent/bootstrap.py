from __future__ import annotations

import atexit
from typing import TYPE_CHECKING

from popcorn_society_agent.adapters.agent_observability import create_agent_metrics
from popcorn_society_agent.adapters.fakes import FakeConciergeRunner
from popcorn_society_agent.adapters.http_observability import (
    create_http_metrics,
    install_http_observability,
)
from popcorn_society_agent.adapters.live_voice import LiveVoiceRunner
from popcorn_society_agent.adapters.logging import configure_logging
from popcorn_society_agent.adapters.memory import InMemoryConversationStore, InMemoryCostLedger
from popcorn_society_agent.adapters.personal_tools import McpDelegationVerifier
from popcorn_society_agent.adapters.profiling import configure_profiling
from popcorn_society_agent.adapters.pydantic_ai_runner import PydanticAIConciergeRunner
from popcorn_society_agent.adapters.realtime_voice import RealtimeVoiceRunner
from popcorn_society_agent.adapters.telemetry import configure_telemetry
from popcorn_society_agent.adapters.voice_quota import SqliteVoiceQuota
from popcorn_society_agent.concierge.service import ConciergeService
from popcorn_society_agent.concierge.voice_quota import MemoryVoiceQuota
from popcorn_society_agent.settings import (
    ModelBackend,
    RuntimeSecrets,
    Settings,
    load_runtime_live_key,
    load_runtime_secrets,
    load_runtime_voice_secrets,
    load_settings,
)
from popcorn_society_agent.web.app import create_web_app

if TYPE_CHECKING:
    from fastapi import FastAPI

    from popcorn_society_agent.concierge.ports import ConciergeRunner


def create_app(settings: Settings | None = None, runner: ConciergeRunner | None = None) -> FastAPI:
    """Compose the process without import-time I/O or external dependencies."""

    resolved_settings = settings or load_settings()
    if resolved_settings.model_backend is ModelBackend.OPENAI:
        # Every text/voice MCP adapter must use the same mounted workload identity.
        resolved_settings = resolved_settings.model_copy(
            update={"mcp_bearer_token": load_runtime_secrets(resolved_settings).mcp_bearer_token}
        )
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
    live_runner = None
    if resolved_settings.voice_live_enabled:
        key = load_runtime_live_key(resolved_settings)
        live_runner = LiveVoiceRunner(
            key=key,
            backend=PydanticAIConciergeRunner(
                settings=resolved_settings,
                secrets=RuntimeSecrets(
                    openai_api_key=key, mcp_bearer_token=resolved_settings.mcp_bearer_token
                ),
            ),
            verifier=McpDelegationVerifier(resolved_settings),
        )
    app = create_web_app(
        service_name=resolved_settings.service_name,
        version=resolved_settings.version,
        concierge_service=concierge_service,
        delegation_verifier=McpDelegationVerifier(resolved_settings),
        allowed_hosts=tuple(resolved_settings.allowed_hosts),
        max_request_body_bytes=resolved_settings.max_request_body_bytes,
        voice_runner=RealtimeVoiceRunner(
            settings=resolved_settings, secrets=load_runtime_voice_secrets(resolved_settings)
        )
        if resolved_settings.voice_enabled
        else None,
        voice_allowed_origins=tuple(resolved_settings.voice_allowed_origins),
        live_voice_runner=live_runner,
        voice_session_seconds=resolved_settings.voice_session_seconds,
        voice_browser_seconds=resolved_settings.voice_browser_seconds,
        voice_shared_seconds=resolved_settings.voice_shared_seconds,
        voice_quota=SqliteVoiceQuota(
            resolved_settings.voice_quota_database,
            resolved_settings.voice_browser_seconds,
            resolved_settings.voice_shared_seconds,
        )
        if resolved_settings.voice_quota_database is not None
        and (resolved_settings.voice_enabled or resolved_settings.voice_live_enabled)
        else MemoryVoiceQuota(
            resolved_settings.voice_browser_seconds, resolved_settings.voice_shared_seconds
        ),
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

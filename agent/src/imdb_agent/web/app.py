from typing import TYPE_CHECKING

from fastapi import FastAPI

from imdb_agent.web.concierge import create_concierge_router
from imdb_agent.web.guardrails import PathScopedTrustedHostMiddleware, RequestBodyLimitMiddleware
from imdb_agent.web.health import create_health_router
from imdb_agent.web.voice import create_voice_router

if TYPE_CHECKING:
    from imdb_agent.concierge.personal import DelegationVerifier
    from imdb_agent.concierge.service import ConciergeService
    from imdb_agent.concierge.voice import VoiceRunner


def create_web_app(
    *,
    service_name: str,
    version: str,
    concierge_service: ConciergeService,
    allowed_hosts: tuple[str, ...],
    max_request_body_bytes: int,
    delegation_verifier: DelegationVerifier | None = None,
    voice_runner: VoiceRunner | None = None,
    voice_allowed_origins: tuple[str, ...] = (),
    voice_session_seconds: float = 300,
    voice_max_sessions: int = 20,
) -> FastAPI:
    """Create the inbound web Adapter without configuring outbound Adapters."""

    app = FastAPI(
        title="IMDb Clone Movie Concierge",
        version=version,
        docs_url=None,
        redoc_url=None,
        openapi_url=None,
    )
    app.add_middleware(
        RequestBodyLimitMiddleware,
        max_body_bytes=max_request_body_bytes,
        path_prefix="/v1/",
    )
    app.add_middleware(
        PathScopedTrustedHostMiddleware,
        allowed_hosts=allowed_hosts,
        path_prefix="/v1/",
    )
    app.include_router(create_health_router(service_name=service_name, version=version))
    app.include_router(create_concierge_router(concierge_service, delegation_verifier))
    app.include_router(
        create_voice_router(
            voice_runner,
            verifier=delegation_verifier,
            allowed_origins=voice_allowed_origins,
            session_seconds=voice_session_seconds,
            max_sessions=voice_max_sessions,
        )
    )
    return app

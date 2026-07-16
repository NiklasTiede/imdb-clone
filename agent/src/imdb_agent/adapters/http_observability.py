from __future__ import annotations

import re
from dataclasses import dataclass
from time import perf_counter
from typing import TYPE_CHECKING, Final
from uuid import uuid4

import structlog
from fastapi import FastAPI, Request, Response
from prometheus_client import (
    CONTENT_TYPE_LATEST,
    CollectorRegistry,
    Counter,
    Gauge,
    Histogram,
    generate_latest,
)

if TYPE_CHECKING:
    from starlette.middleware.base import RequestResponseEndpoint

    from imdb_agent.settings import Settings

REQUEST_ID_PATTERN: Final = re.compile(r"^[A-Za-z0-9][A-Za-z0-9._:-]{0,63}$")


@dataclass(frozen=True, slots=True)
class HttpMetrics:
    registry: CollectorRegistry
    requests: Counter
    duration: Histogram
    in_progress: Gauge


def create_http_metrics(settings: Settings) -> HttpMetrics:
    registry = CollectorRegistry(auto_describe=True)
    build_info = Gauge(
        "imdb_agent_build_info",
        "Build and deployment identity for the Movie Concierge.",
        ("service", "environment", "version"),
        registry=registry,
    )
    build_info.labels(
        service=settings.service_name,
        environment=settings.environment.value,
        version=settings.version,
    ).set(1)

    return HttpMetrics(
        registry=registry,
        requests=Counter(
            "imdb_agent_http_requests",
            "Completed Movie Concierge HTTP requests.",
            ("method", "route", "status"),
            registry=registry,
        ),
        duration=Histogram(
            "imdb_agent_http_request_duration_seconds",
            "Movie Concierge HTTP request duration.",
            ("method", "route", "status"),
            registry=registry,
        ),
        in_progress=Gauge(
            "imdb_agent_http_requests_in_progress",
            "Movie Concierge HTTP requests currently in progress.",
            ("method",),
            registry=registry,
        ),
    )


def install_http_observability(app: FastAPI, settings: Settings) -> None:
    """Install bounded HTTP metrics and safe correlation at the composition root."""

    metrics = create_http_metrics(settings)
    logger = structlog.get_logger()

    async def metrics_endpoint() -> Response:
        return Response(
            content=generate_latest(metrics.registry),
            media_type=CONTENT_TYPE_LATEST,
        )

    app.add_api_route(
        "/metrics",
        metrics_endpoint,
        methods=["GET"],
        include_in_schema=False,
    )

    async def observe_request(
        request: Request,
        call_next: RequestResponseEndpoint,
    ) -> Response:
        if request.url.path == "/metrics":
            return await call_next(request)

        method = request.method
        request_id = valid_request_id(request.headers.get("X-Request-ID"))
        started_at = perf_counter()
        status_code = 500
        metrics.in_progress.labels(method=method).inc()
        structlog.contextvars.clear_contextvars()
        structlog.contextvars.bind_contextvars(request_id=request_id)

        try:
            response = await call_next(request)
            status_code = response.status_code
            response.headers["X-Request-ID"] = request_id
            return response
        finally:
            duration_seconds = perf_counter() - started_at
            route = route_template(request)
            status = str(status_code)
            metrics.in_progress.labels(method=method).dec()
            metrics.requests.labels(method=method, route=route, status=status).inc()
            metrics.duration.labels(method=method, route=route, status=status).observe(
                duration_seconds
            )
            log_method = logger.error if status_code >= 500 else logger.info
            log_method(
                "http_request_completed",
                duration_ms=round(duration_seconds * 1000, 3),
                environment=settings.environment.value,
                method=method,
                outcome="error" if status_code >= 500 else "success",
                request_id=request_id,
                route=route,
                service=settings.service_name,
                status_code=status_code,
                version=settings.version,
            )
            structlog.contextvars.clear_contextvars()

    app.middleware("http")(observe_request)


def valid_request_id(candidate: str | None) -> str:
    if candidate is not None and REQUEST_ID_PATTERN.fullmatch(candidate) is not None:
        return candidate
    return uuid4().hex


def route_template(request: Request) -> str:
    route = request.scope.get("route")
    path = getattr(route, "path", None)
    return path if isinstance(path, str) else "unmatched"

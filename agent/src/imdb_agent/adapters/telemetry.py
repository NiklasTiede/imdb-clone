from __future__ import annotations

import re
from dataclasses import dataclass
from typing import TYPE_CHECKING

import structlog
from opentelemetry.exporter.otlp.proto.http.trace_exporter import OTLPSpanExporter
from opentelemetry.instrumentation.fastapi import (  # pyright: ignore[reportMissingTypeStubs]
    FastAPIInstrumentor,
)
from opentelemetry.instrumentation.httpx import HTTPXClientInstrumentor
from opentelemetry.sdk.resources import Resource
from opentelemetry.sdk.trace import TracerProvider
from opentelemetry.sdk.trace.export import BatchSpanProcessor
from opentelemetry.sdk.trace.sampling import ParentBased, TraceIdRatioBased
from pydantic_ai import Agent
from pydantic_ai.models.instrumented import InstrumentationSettings

if TYPE_CHECKING:
    from fastapi import FastAPI
    from opentelemetry.trace import Span

    from imdb_agent.settings import Settings


_CONVERSATION_MESSAGE_PATH = re.compile(r"^/v1/conversations/[^/]+/messages$")
_SAFE_STATIC_PATHS = frozenset({"/healthz", "/metrics", "/readyz", "/v1/conversations"})


@dataclass(frozen=True, slots=True)
class TelemetryRuntime:
    """Process-owned OpenTelemetry lifecycle kept outside the product Module."""

    tracer_provider: TracerProvider | None = None

    @property
    def enabled(self) -> bool:
        return self.tracer_provider is not None

    def instrument_app(self, app: FastAPI) -> None:
        if self.tracer_provider is None:
            return
        FastAPIInstrumentor.instrument_app(
            app,
            tracer_provider=self.tracer_provider,
            excluded_urls="healthz,readyz,metrics",
            exclude_spans=["receive", "send"],
            server_request_hook=privacy_safe_server_request_hook,
        )

    def shutdown(self) -> None:
        if self.tracer_provider is None:
            return
        try:
            self.tracer_provider.force_flush(timeout_millis=5_000)
            self.tracer_provider.shutdown()
        except Exception:  # Observability must not break graceful application shutdown.
            structlog.get_logger().warning(
                "telemetry_shutdown_failed",
                error_code="otel_shutdown",
            )


def configure_telemetry(settings: Settings) -> TelemetryRuntime:
    """Configure privacy-safe tracing without introducing a hosted telemetry dependency."""

    if not settings.otel_tracing_enabled:
        return TelemetryRuntime()

    endpoint = settings.otel_exporter_otlp_traces_endpoint
    if endpoint is None:
        return TelemetryRuntime()

    provider: TracerProvider | None = None
    try:
        exporter = OTLPSpanExporter(
            endpoint=endpoint,
            timeout=settings.otel_export_timeout_seconds,
        )
        provider = TracerProvider(
            resource=Resource.create(
                {
                    "deployment.environment.name": settings.environment.value,
                    "service.name": settings.service_name,
                    "service.version": settings.version,
                }
            ),
            sampler=ParentBased(TraceIdRatioBased(settings.otel_trace_sample_ratio)),
        )
        provider.add_span_processor(
            BatchSpanProcessor(
                exporter,
                max_export_batch_size=512,
                schedule_delay_millis=5_000,
            )
        )
        Agent.instrument_all(privacy_safe_instrumentation_settings(provider))
        HTTPXClientInstrumentor().instrument(tracer_provider=provider)
        structlog.get_logger().info(
            "telemetry_initialized",
            environment=settings.environment.value,
            service=settings.service_name,
            version=settings.version,
        )
        return TelemetryRuntime(tracer_provider=provider)
    except Exception:  # Export setup is deliberately fail-open for user requests.
        if provider is not None:
            provider.shutdown()
        structlog.get_logger().warning(
            "telemetry_initialization_failed",
            error_code="otel_setup",
        )
        return TelemetryRuntime()


def privacy_safe_instrumentation_settings(
    tracer_provider: TracerProvider,
) -> InstrumentationSettings:
    """Keep structural agent spans while excluding user and tool content."""

    return InstrumentationSettings(
        tracer_provider=tracer_provider,
        include_binary_content=False,
        include_content=False,
        include_model_request_parameters=False,
    )


def privacy_safe_server_request_hook(span: Span, scope: dict[str, object]) -> None:
    """Remove conversation IDs and client fingerprints from inbound HTTP spans."""

    if not span.is_recording():
        return
    raw_path = scope.get("path")
    path = raw_path if isinstance(raw_path, str) else ""
    if _CONVERSATION_MESSAGE_PATH.fullmatch(path):
        safe_path = "/v1/conversations/{conversation_id}/messages"
    elif path in _SAFE_STATIC_PATHS:
        safe_path = path
    else:
        safe_path = "/unmatched"
    raw_method = scope.get("method")
    method = raw_method if isinstance(raw_method, str) and raw_method.isalpha() else "HTTP"
    method = method.upper()

    span.update_name(f"{method} {safe_path}")
    span.set_attribute("http.route", safe_path)
    span.set_attribute("http.target", safe_path)
    span.set_attribute("http.url", safe_path)
    span.set_attribute("url.path", safe_path)
    span.set_attribute("url.query", "")
    span.set_attribute("url.full", safe_path)
    span.set_attribute("client.address", "redacted")
    span.set_attribute("client.port", 0)
    span.set_attribute("net.peer.ip", "redacted")
    span.set_attribute("net.peer.port", 0)
    span.set_attribute("http.user_agent", "redacted")
    span.set_attribute("user_agent.original", "redacted")

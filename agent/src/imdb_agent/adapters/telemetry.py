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
from opentelemetry.sdk.trace import ReadableSpan, TracerProvider
from opentelemetry.sdk.trace.export import (
    BatchSpanProcessor,
    SpanExporter,
    SpanExportResult,
)
from opentelemetry.sdk.trace.sampling import ParentBased, TraceIdRatioBased
from pydantic_ai import Agent
from pydantic_ai.models.instrumented import InstrumentationSettings
from pyroscope.otel import (  # pyright: ignore[reportMissingTypeStubs]
    PyroscopeSpanProcessor,
)

if TYPE_CHECKING:
    from collections.abc import Sequence

    from fastapi import FastAPI
    from opentelemetry.trace import Span

    from imdb_agent.settings import Settings


_CONVERSATION_MESSAGE_PATH = re.compile(r"^/v1/conversations/[^/]+/messages$")
_SAFE_STATIC_PATHS = frozenset({"/healthz", "/metrics", "/readyz", "/v1/conversations"})
_PRIVATE_TRACE_ATTRIBUTE_KEYS = frozenset({"gen_ai.conversation.id"})


class AttributeFilteringSpanExporter(SpanExporter):
    """Remove private span attributes immediately before they leave the process."""

    def __init__(
        self,
        delegate: SpanExporter,
        *,
        excluded_attribute_keys: frozenset[str],
    ) -> None:
        self._delegate = delegate
        self._excluded_attribute_keys = excluded_attribute_keys

    def export(self, spans: Sequence[ReadableSpan]) -> SpanExportResult:
        sanitized_spans = tuple(
            _without_attributes(span, self._excluded_attribute_keys) for span in spans
        )
        return self._delegate.export(sanitized_spans)

    def shutdown(self) -> None:
        self._delegate.shutdown()

    def force_flush(self, timeout_millis: int = 30_000) -> bool:
        return self._delegate.force_flush(timeout_millis)


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


def configure_telemetry(
    settings: Settings,
    *,
    profiling_enabled: bool = False,
) -> TelemetryRuntime:
    """Configure privacy-safe tracing without introducing a hosted telemetry dependency."""

    if not settings.otel_tracing_enabled:
        return TelemetryRuntime()

    endpoint = settings.otel_exporter_otlp_traces_endpoint
    if endpoint is None:
        return TelemetryRuntime()

    provider: TracerProvider | None = None
    try:
        exporter = AttributeFilteringSpanExporter(
            OTLPSpanExporter(
                endpoint=endpoint,
                timeout=settings.otel_export_timeout_seconds,
            ),
            excluded_attribute_keys=_PRIVATE_TRACE_ATTRIBUTE_KEYS,
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
        add_profile_correlation(provider, enabled=profiling_enabled)
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


def add_profile_correlation(provider: TracerProvider, *, enabled: bool) -> None:
    """Tag root spans so Grafana can query their matching profile samples."""

    if enabled:
        provider.add_span_processor(PyroscopeSpanProcessor())


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


def _without_attributes(
    span: ReadableSpan,
    excluded_attribute_keys: frozenset[str],
) -> ReadableSpan:
    attributes = span.attributes
    if attributes is None or excluded_attribute_keys.isdisjoint(attributes):
        return span

    safe_attributes = {
        key: value for key, value in attributes.items() if key not in excluded_attribute_keys
    }
    return ReadableSpan(
        name=span.name,
        context=span.context,
        parent=span.parent,
        resource=span.resource,
        attributes=safe_attributes,
        events=span.events,
        links=span.links,
        kind=span.kind,
        status=span.status,
        start_time=span.start_time,
        end_time=span.end_time,
        instrumentation_scope=span.instrumentation_scope,
    )

from __future__ import annotations

from opentelemetry.sdk.trace import TracerProvider
from opentelemetry.sdk.trace.export import SimpleSpanProcessor
from opentelemetry.sdk.trace.export.in_memory_span_exporter import InMemorySpanExporter

from imdb_agent.adapters.telemetry import (
    TelemetryRuntime,
    configure_telemetry,
    privacy_safe_instrumentation_settings,
    privacy_safe_server_request_hook,
)
from imdb_agent.settings import DeploymentEnvironment, Settings


def test_telemetry_is_disabled_without_configuring_exporters() -> None:
    runtime = configure_telemetry(
        Settings(
            environment=DeploymentEnvironment.TEST,
            otel_tracing_enabled=False,
        )
    )

    assert runtime == TelemetryRuntime()
    assert runtime.enabled is False


def test_pydantic_ai_instrumentation_excludes_user_and_tool_content() -> None:
    provider = TracerProvider()

    settings = privacy_safe_instrumentation_settings(provider)

    assert settings.include_content is False
    assert settings.include_binary_content is False
    assert settings.include_model_request_parameters is False
    provider.shutdown()


def test_http_instrumentation_redacts_conversation_and_client_data() -> None:
    exporter = InMemorySpanExporter()
    provider = TracerProvider()
    provider.add_span_processor(SimpleSpanProcessor(exporter))
    tracer = provider.get_tracer("test")

    with tracer.start_as_current_span("unsafe") as span:
        span.set_attribute(
            "http.url",
            "https://example.test/v1/conversations/private-id/messages?token=private",
        )
        privacy_safe_server_request_hook(
            span,
            {
                "method": "POST",
                "path": "/v1/conversations/private-id/messages",
            },
        )

    finished_span = exporter.get_finished_spans()[0]
    attributes = finished_span.attributes
    assert attributes is not None
    assert finished_span.name == "POST /v1/conversations/{conversation_id}/messages"
    assert attributes["http.url"] == "/v1/conversations/{conversation_id}/messages"
    assert attributes["client.address"] == "redacted"
    assert "private-id" not in str(attributes)
    assert "private" not in str(attributes)
    provider.shutdown()

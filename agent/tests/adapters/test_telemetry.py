from __future__ import annotations

from typing import TYPE_CHECKING

import pytest
from opentelemetry.sdk.trace import ReadableSpan, TracerProvider
from opentelemetry.sdk.trace.export import (
    SimpleSpanProcessor,
    SpanExporter,
    SpanExportResult,
)
from opentelemetry.sdk.trace.export.in_memory_span_exporter import InMemorySpanExporter
from pydantic_ai import Agent, ModelRetry
from pydantic_ai.capabilities import Instrumentation
from pydantic_ai.exceptions import UnexpectedModelBehavior
from pydantic_ai.messages import ModelResponse, ToolCallPart
from pydantic_ai.models.function import AgentInfo, FunctionModel

from popcorn_society_agent.adapters.telemetry import (
    AttributeFilteringSpanExporter,
    TelemetryRuntime,
    add_profile_correlation,
    configure_telemetry,
    privacy_safe_instrumentation_settings,
    privacy_safe_server_request_hook,
)
from popcorn_society_agent.settings import DeploymentEnvironment, Settings

if TYPE_CHECKING:
    from collections.abc import Sequence

    from pydantic_ai.messages import ModelMessage


class _RecordingSpanExporter(SpanExporter):
    def __init__(self) -> None:
        self.spans: tuple[ReadableSpan, ...] = ()
        self.shutdown_called = False
        self.flush_timeout_millis: int | None = None

    def export(self, spans: Sequence[ReadableSpan]) -> SpanExportResult:
        self.spans = tuple(spans)
        return SpanExportResult.SUCCESS

    def shutdown(self) -> None:
        self.shutdown_called = True

    def force_flush(self, timeout_millis: int = 30_000) -> bool:
        self.flush_timeout_millis = timeout_millis
        return True


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


@pytest.mark.parametrize(
    ("failure", "run_error"), [(ValueError, ValueError), (ModelRetry, UnexpectedModelBehavior)]
)
def test_tool_error_traces_exclude_sensitive_content(
    failure: type[Exception], run_error: type[Exception]
) -> None:
    marker = "synthetic-private-tool-result"
    exporter = InMemorySpanExporter()
    provider = TracerProvider()
    provider.add_span_processor(SimpleSpanProcessor(exporter))

    def call_tool(messages: list[ModelMessage], info: AgentInfo) -> ModelResponse:
        return ModelResponse(parts=[ToolCallPart("sensitive_tool", {})])

    agent: Agent[None, str] = Agent(
        FunctionModel(call_tool),
        retries=0,
        capabilities=[Instrumentation(settings=privacy_safe_instrumentation_settings(provider))],
    )

    def sensitive_tool() -> str:
        raise failure(marker)

    agent.tool_plain(sensitive_tool)

    try:
        with pytest.raises(run_error):
            agent.run_sync("Run the synthetic tool.")

        spans = exporter.get_finished_spans()
        assert spans
        exception_events = [
            event for span in spans for event in span.events if event.name == "exception"
        ]
        assert exception_events
        assert any("exception.type" in (event.attributes or {}) for event in exception_events)
        for span in spans:
            assert marker not in repr(span.attributes)
            assert marker not in repr(span.status.description)
            for event in span.events:
                assert marker not in repr(event.attributes)
    finally:
        provider.shutdown()


def test_trace_export_removes_conversation_id_without_mutating_other_processors() -> None:
    filtered_delegate = _RecordingSpanExporter()
    original_exporter = InMemorySpanExporter()
    provider = TracerProvider()
    provider.add_span_processor(
        SimpleSpanProcessor(
            AttributeFilteringSpanExporter(
                filtered_delegate,
                excluded_attribute_keys=frozenset({"gen_ai.conversation.id"}),
            )
        )
    )
    provider.add_span_processor(SimpleSpanProcessor(original_exporter))

    with provider.get_tracer("test").start_as_current_span("agent run") as span:
        span.set_attribute("gen_ai.conversation.id", "private-conversation-id")
        span.set_attribute("gen_ai.tool.name", "search_movies")

    exported_attributes = filtered_delegate.spans[0].attributes
    original_attributes = original_exporter.get_finished_spans()[0].attributes
    assert exported_attributes is not None
    assert original_attributes is not None
    assert "gen_ai.conversation.id" not in exported_attributes
    assert exported_attributes["gen_ai.tool.name"] == "search_movies"
    assert original_attributes["gen_ai.conversation.id"] == "private-conversation-id"
    provider.shutdown()


def test_trace_attribute_filter_delegates_exporter_lifecycle() -> None:
    delegate = _RecordingSpanExporter()
    exporter = AttributeFilteringSpanExporter(
        delegate,
        excluded_attribute_keys=frozenset({"gen_ai.conversation.id"}),
    )

    assert exporter.force_flush(1_234) is True
    exporter.shutdown()

    assert delegate.flush_timeout_millis == 1_234
    assert delegate.shutdown_called is True


def test_profile_correlation_tags_root_spans() -> None:
    exporter = InMemorySpanExporter()
    provider = TracerProvider()
    add_profile_correlation(provider, enabled=True)
    provider.add_span_processor(SimpleSpanProcessor(exporter))

    with provider.get_tracer("test").start_as_current_span("root"):
        pass

    attributes = exporter.get_finished_spans()[0].attributes
    assert attributes is not None
    assert attributes["pyroscope.profile.id"]
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

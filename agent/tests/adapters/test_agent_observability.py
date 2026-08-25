from opentelemetry.sdk.trace import TracerProvider
from opentelemetry.sdk.trace.export import SimpleSpanProcessor
from opentelemetry.sdk.trace.export.in_memory_span_exporter import InMemorySpanExporter
from prometheus_client import CollectorRegistry

from imdb_agent.adapters.agent_observability import create_agent_metrics
from imdb_agent.settings import DeploymentEnvironment, Settings


def test_ui_action_decisions_use_bounded_metrics_and_trace_attributes() -> None:
    registry = CollectorRegistry()
    metrics = create_agent_metrics(
        registry,
        Settings(environment=DeploymentEnvironment.TEST),
    )
    exporter = InMemorySpanExporter()
    provider = TracerProvider()
    provider.add_span_processor(SimpleSpanProcessor(exporter))

    with provider.get_tracer("test").start_as_current_span("concierge request"):
        metrics.ui_action(action="open_movie", outcome="emitted")

    assert (
        registry.get_sample_value(
            "imdb_agent_ui_actions_total",
            {"action": "open_movie", "outcome": "emitted"},
        )
        == 1
    )
    span = exporter.get_finished_spans()[0]
    assert len(span.events) == 1
    event = span.events[0]
    assert event.name == "imdb.concierge.ui_action"
    assert event.attributes is not None
    assert event.attributes == {
        "imdb.concierge.ui_action.type": "open_movie",
        "imdb.concierge.ui_action.outcome": "emitted",
    }
    serialized_attributes = " ".join(event.attributes)
    assert "movie_id" not in serialized_attributes
    assert "conversation" not in serialized_attributes
    provider.shutdown()

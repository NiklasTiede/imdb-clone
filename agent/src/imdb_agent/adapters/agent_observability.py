from __future__ import annotations

from dataclasses import dataclass
from typing import TYPE_CHECKING

import structlog
from opentelemetry import trace
from prometheus_client import CollectorRegistry, Counter, Gauge, Histogram

if TYPE_CHECKING:
    from decimal import Decimal

    from imdb_agent.concierge.events import UsageSummary
    from imdb_agent.concierge.tools import ToolName
    from imdb_agent.settings import Settings


@dataclass(frozen=True, slots=True)
class AgentMetrics:
    runs: Counter
    duration: Histogram
    first_event_duration: Histogram
    process_budget_committed: Gauge
    active: Gauge
    tool_calls: Counter
    ui_actions: Counter
    tokens: Counter
    estimated_cost: Counter
    disconnects: Counter

    def started(self) -> None:
        self.active.inc()
        structlog.get_logger().info("concierge_run_started")

    def tool_called(self, tool_name: ToolName) -> None:
        self.tool_calls.labels(tool=tool_name.value).inc()
        structlog.get_logger().info("concierge_tool_called", tool=tool_name.value)

    def ui_action(self, *, action: str, outcome: str) -> None:
        self.ui_actions.labels(action=action, outcome=outcome).inc()
        span = trace.get_current_span()
        if span.is_recording():
            span.add_event(
                "imdb.concierge.ui_action",
                attributes={
                    "imdb.concierge.ui_action.type": action,
                    "imdb.concierge.ui_action.outcome": outcome,
                },
            )
        structlog.get_logger().info(
            "concierge_ui_action_decided",
            action=action,
            outcome=outcome,
        )

    def first_event(self, duration_seconds: float) -> None:
        self.first_event_duration.observe(duration_seconds)

    def budget_committed(self, amount_usd: Decimal) -> None:
        self.process_budget_committed.set(float(amount_usd))

    def finished(
        self,
        *,
        outcome: str,
        duration_seconds: float,
        usage: UsageSummary | None,
    ) -> None:
        self.active.dec()
        self.runs.labels(outcome=outcome).inc()
        self.duration.labels(outcome=outcome).observe(duration_seconds)
        if usage is not None:
            self.tokens.labels(model=usage.model, direction="input").inc(usage.input_tokens)
            self.tokens.labels(model=usage.model, direction="cache_read").inc(
                usage.cache_read_tokens
            )
            self.tokens.labels(model=usage.model, direction="cache_write").inc(
                usage.cache_write_tokens
            )
            self.tokens.labels(model=usage.model, direction="output").inc(usage.output_tokens)
            self.estimated_cost.labels(model=usage.model).inc(float(usage.estimated_cost_usd))
        usage_fields: dict[str, object] = {}
        if usage is not None:
            usage_fields = {
                "estimated_cost_usd": str(usage.estimated_cost_usd),
                "input_tokens": usage.input_tokens,
                "model": usage.model,
                "output_tokens": usage.output_tokens,
                "requests": usage.requests,
                "tool_calls": usage.tool_calls,
            }
        structlog.get_logger().info(
            "concierge_run_completed",
            duration_ms=round(duration_seconds * 1000, 3),
            outcome=outcome,
            **usage_fields,
        )

    def disconnected(self) -> None:
        self.disconnects.inc()


def create_agent_metrics(registry: CollectorRegistry, settings: Settings) -> AgentMetrics:
    guardrail_limit = Gauge(
        "imdb_agent_guardrail_limit",
        "Configured process guardrail limits by bounded kind.",
        ("limit",),
        registry=registry,
    )
    for limit, value in (
        ("max_concurrent_runs", settings.max_concurrent_runs),
        ("max_input_tokens", settings.max_input_tokens),
        ("max_output_tokens", settings.max_output_tokens),
        ("project_cost_usd", float(settings.project_cost_limit_usd)),
        ("run_cost_usd", float(settings.run_cost_limit_usd)),
    ):
        guardrail_limit.labels(limit=limit).set(value)

    process_budget_committed = Gauge(
        "imdb_agent_process_budget_committed_usd",
        "Pessimistic cost committed by the process-local inference budget ledger.",
        registry=registry,
    )
    process_budget_committed.set(0)

    return AgentMetrics(
        runs=Counter(
            "imdb_agent_runs",
            "Completed Movie Concierge runs.",
            ("outcome",),
            registry=registry,
        ),
        duration=Histogram(
            "imdb_agent_run_duration_seconds",
            "Movie Concierge run duration.",
            ("outcome",),
            registry=registry,
        ),
        first_event_duration=Histogram(
            "imdb_agent_first_event_duration_seconds",
            "Time from accepted run to the first model or tool event.",
            registry=registry,
        ),
        process_budget_committed=process_budget_committed,
        active=Gauge(
            "imdb_agent_runs_active",
            "Movie Concierge runs currently active.",
            registry=registry,
        ),
        tool_calls=Counter(
            "imdb_agent_tool_calls",
            "Observed Movie Concierge tool calls.",
            ("tool",),
            registry=registry,
        ),
        ui_actions=Counter(
            "imdb_agent_ui_actions",
            "Grounded Movie Concierge UI action decisions.",
            ("action", "outcome"),
            registry=registry,
        ),
        tokens=Counter(
            "imdb_agent_model_tokens",
            "Provider-reported Movie Concierge tokens.",
            ("model", "direction"),
            registry=registry,
        ),
        estimated_cost=Counter(
            "imdb_agent_model_estimated_cost_usd",
            "Best-effort Movie Concierge inference cost in USD.",
            ("model",),
            registry=registry,
        ),
        disconnects=Counter(
            "imdb_agent_sse_disconnects",
            "Movie Concierge SSE streams cancelled by clients.",
            registry=registry,
        ),
    )

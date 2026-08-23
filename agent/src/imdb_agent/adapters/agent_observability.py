from __future__ import annotations

from dataclasses import dataclass
from typing import TYPE_CHECKING

from prometheus_client import CollectorRegistry, Counter, Gauge, Histogram

if TYPE_CHECKING:
    from decimal import Decimal

    from imdb_agent.concierge.events import UsageSummary
    from imdb_agent.settings import Settings


@dataclass(frozen=True, slots=True)
class AgentMetrics:
    runs: Counter
    duration: Histogram
    first_event_duration: Histogram
    process_budget_committed: Gauge
    active: Gauge
    tool_calls: Counter
    tokens: Counter
    estimated_cost: Counter
    disconnects: Counter

    def started(self) -> None:
        self.active.inc()

    def tool_called(self, tool_name: str) -> None:
        self.tool_calls.labels(tool=tool_name).inc()

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

from __future__ import annotations

from dataclasses import dataclass
from typing import TYPE_CHECKING

from prometheus_client import CollectorRegistry, Counter, Gauge, Histogram

if TYPE_CHECKING:
    from imdb_agent.concierge.events import UsageSummary


@dataclass(frozen=True, slots=True)
class AgentMetrics:
    runs: Counter
    duration: Histogram
    active: Gauge
    tool_calls: Counter
    tokens: Counter
    estimated_cost: Counter
    disconnects: Counter

    def started(self) -> None:
        self.active.inc()

    def tool_called(self, tool_name: str) -> None:
        self.tool_calls.labels(tool=tool_name).inc()

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


def create_agent_metrics(registry: CollectorRegistry) -> AgentMetrics:
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

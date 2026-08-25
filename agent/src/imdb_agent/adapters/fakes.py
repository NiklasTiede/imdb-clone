from __future__ import annotations

from decimal import Decimal
from typing import TYPE_CHECKING

from imdb_agent.concierge.events import (
    GroundedMovie,
    MovieCardEvent,
    TextEvent,
    ToolCallEvent,
    UsageEvent,
    UsageSummary,
)
from imdb_agent.concierge.tools import ToolName

if TYPE_CHECKING:
    from collections.abc import AsyncIterator

    from imdb_agent.concierge.ports import RunRequest


class FakeConciergeRunner:
    """Deterministic provider/MCP double used by tests and explicitly selected local demos."""

    def __init__(self, *, estimated_cost_usd: Decimal = Decimal(0)) -> None:
        self._estimated_cost_usd = estimated_cost_usd

    async def stream(
        self, request: RunRequest
    ) -> AsyncIterator[ToolCallEvent | TextEvent | MovieCardEvent | UsageEvent]:
        normalized = request.message.casefold()
        if "watchlist" in normalized or "rate " in normalized:
            yield TextEvent(
                delta="This release is read-only, so I cannot change watchlists or ratings."
            )
        else:
            yield ToolCallEvent(
                tool=ToolName.SEARCH_MOVIES,
                arguments={"query": request.message},
            )
            yield MovieCardEvent(movie=fake_arrival())
            yield TextEvent(
                delta=(
                    "Arrival is a grounded catalog match: thoughtful science fiction "
                    "in 116 minutes."
                )
            )
        yield UsageEvent(
            usage=UsageSummary(
                model="deterministic-fake",
                requests=0,
                tool_calls=0 if "watchlist" in normalized or "rate " in normalized else 1,
                input_tokens=0,
                output_tokens=0,
                total_tokens=0,
                estimated_cost_usd=self._estimated_cost_usd,
                cost_available=True,
            )
        )


def fake_arrival() -> GroundedMovie:
    return GroundedMovie(
        movie_id=42,
        primary_title="Arrival",
        original_title="Arrival",
        movie_type="MOVIE",
        start_year=2016,
        runtime_minutes=116,
        genres=("DRAMA", "SCI_FI"),
        imdb_rating=7.9,
        imdb_rating_count=800_000,
        description="A linguist works to communicate with visitors from another world.",
        poster_image_token="arrival-poster",  # noqa: S106 - synthetic media token, not a secret
        explanation="A thoughtful, compact science-fiction drama.",
    )

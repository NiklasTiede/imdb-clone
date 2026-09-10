from __future__ import annotations

from dataclasses import dataclass
from decimal import Decimal  # noqa: TC003 - Pydantic resolves this annotation at runtime
from enum import StrEnum
from typing import TYPE_CHECKING, Annotated, Literal

from pydantic import BaseModel, ConfigDict, Field, TypeAdapter
from pydantic.alias_generators import to_camel

if TYPE_CHECKING:
    from imdb_agent.concierge.tools import ToolName


class EventModel(BaseModel):
    """Immutable browser contract with camel-case JSON aliases."""

    model_config = ConfigDict(
        alias_generator=to_camel,
        extra="forbid",
        frozen=True,
        populate_by_name=True,
        serialize_by_alias=True,
        strict=True,
    )


class RunStatus(StrEnum):
    THINKING = "thinking"
    SEARCHING = "searching_catalog"
    FETCHING_DETAILS = "fetching_details"
    FINDING_SIMILAR = "finding_similar"
    CHOOSING_TONIGHT = "choosing_tonight"


class CompletionOutcome(StrEnum):
    SUCCESS = "success"
    ERROR = "error"
    CANCELLED = "cancelled"


class GroundedMovie(EventModel):
    movie_id: int = Field(gt=0)
    primary_title: str = Field(min_length=1, max_length=300)
    original_title: str | None = Field(default=None, max_length=300)
    movie_type: str
    start_year: int | None = None
    runtime_minutes: int | None = Field(default=None, ge=0)
    genres: tuple[str, ...] = ()
    imdb_rating: float | None = Field(default=None, ge=0, le=10)
    imdb_rating_count: int | None = Field(default=None, ge=0)
    description: str | None = Field(default=None, max_length=600)
    poster_image_token: str | None = Field(default=None, max_length=300)
    explanation: str | None = Field(default=None, max_length=400)


class UsageSummary(EventModel):
    model: str = Field(min_length=1, max_length=100)
    requests: int = Field(ge=0)
    tool_calls: int = Field(ge=0)
    input_tokens: int = Field(ge=0)
    cache_read_tokens: int = Field(default=0, ge=0)
    cache_write_tokens: int = Field(default=0, ge=0)
    output_tokens: int = Field(ge=0)
    total_tokens: int = Field(ge=0)
    estimated_cost_usd: Decimal = Field(ge=0, decimal_places=8)
    cost_available: bool
    cost_basis: str | None = Field(default=None, min_length=1, max_length=80)


class StatusEvent(EventModel):
    type: Literal["status"] = "status"
    sequence: int = Field(default=0, ge=0)
    status: RunStatus


class TextEvent(EventModel):
    type: Literal["text"] = "text"
    sequence: int = Field(default=0, ge=0)
    delta: str = Field(min_length=1)


class MovieCardEvent(EventModel):
    type: Literal["movie-card"] = "movie-card"
    sequence: int = Field(default=0, ge=0)
    movie: GroundedMovie


class OpenMovieAction(EventModel):
    type: Literal["open_movie"] = "open_movie"
    movie_id: int = Field(gt=0)


class OpenMovieTrailerAction(EventModel):
    type: Literal["open_movie_trailer"] = "open_movie_trailer"
    movie_id: int = Field(gt=0)


class OpenWatchlistAction(EventModel):
    type: Literal["open_watchlist"] = "open_watchlist"
    operation_id: str | None = None
    movie_id: int | None = Field(default=None, gt=0)
    created: bool | None = None
    removed: bool | None = None


class OpenRatingsAction(EventModel):
    type: Literal["open_ratings"] = "open_ratings"
    operation_id: str
    movie_id: int = Field(gt=0)
    changed: bool
    score: float | None = Field(ge=0, le=10)
    previous_score: float | None = Field(ge=0, le=10)


class OpenLoginAction(EventModel):
    type: Literal["open_login"] = "open_login"


class OpenPageAction(EventModel):
    type: Literal["open_page"] = "open_page"
    destination: Literal["home", "settings", "watchlist", "ratings"]


class ShowSearchResultsAction(EventModel):
    type: Literal["show_search_results"] = "show_search_results"
    query: str = Field(max_length=200)
    genres: list[str] = Field(default_factory=list, max_length=30)
    movie_type: str | None = Field(default=None, max_length=30)
    min_start_year: int | None = Field(default=None, ge=1850, le=2030)
    max_start_year: int | None = Field(default=None, ge=1850, le=2030)
    min_runtime_minutes: int | None = Field(default=None, ge=0, le=5000)
    max_runtime_minutes: int | None = Field(default=None, ge=0, le=5000)


ApplicationAction = (
    OpenMovieAction
    | OpenMovieTrailerAction
    | OpenWatchlistAction
    | OpenRatingsAction
    | OpenLoginAction
    | OpenPageAction
    | ShowSearchResultsAction
)


class UiActionEvent(EventModel):
    type: Literal["ui-action"] = "ui-action"
    sequence: int = Field(default=0, ge=0)
    action: ApplicationAction


class ErrorEvent(EventModel):
    type: Literal["error"] = "error"
    sequence: int = Field(default=0, ge=0)
    code: str = Field(min_length=1, max_length=80)
    message: str = Field(min_length=1, max_length=300)
    retryable: bool


class UsageEvent(EventModel):
    type: Literal["usage"] = "usage"
    sequence: int = Field(default=0, ge=0)
    usage: UsageSummary


class CompletionEvent(EventModel):
    type: Literal["completion"] = "completion"
    sequence: int = Field(default=0, ge=0)
    conversation_id: str = Field(min_length=1, max_length=80)
    outcome: CompletionOutcome


@dataclass(frozen=True, slots=True)
class ToolCallEvent:
    """Internal evidence of one validated tool call; never part of the browser contract."""

    tool: ToolName
    arguments: dict[str, object]


ConciergeEvent = Annotated[
    StatusEvent
    | TextEvent
    | MovieCardEvent
    | UiActionEvent
    | ErrorEvent
    | UsageEvent
    | CompletionEvent,
    Field(discriminator="type"),
]

concierge_event_adapter: TypeAdapter[ConciergeEvent] = TypeAdapter(ConciergeEvent)


RunnerEvent = ToolCallEvent | TextEvent | MovieCardEvent | UsageEvent | UiActionEvent

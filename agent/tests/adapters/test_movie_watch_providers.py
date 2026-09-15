from __future__ import annotations

from typing import TYPE_CHECKING, Any, cast

import pytest
from pydantic import SecretStr, ValidationError
from pydantic_ai.exceptions import ToolFailed

from imdb_agent.adapters.catalog_contract import WatchProvidersResult, parse_grounded_movies
from imdb_agent.adapters.personal_tools import PersonalToolGate
from imdb_agent.concierge.events import GroundedMovie
from imdb_agent.concierge.personal import PersonalTurn
from imdb_agent.concierge.streaming import StreamingRegion
from imdb_agent.concierge.tools import ToolName

if TYPE_CHECKING:
    from pydantic_ai import RunContext


def watch_result(country: str = "CH", outcome: str = "AVAILABLE") -> dict[str, Any]:
    available = outcome in {"AVAILABLE", "NO_OFFERS"}
    return {
        "contractVersion": "1.0",
        "movieId": 6,
        "country": country,
        "outcome": outcome,
        "source": "JUSTWATCH_VIA_TMDB",
        "sourceUrl": f"https://www.themoviedb.org/movie/13/watch?locale={country}"
        if available
        else None,
        "fetchedAt": "2026-09-10T12:00:00Z" if available else None,
        "offers": {
            "subscription": ["Swiss Stream"] if outcome == "AVAILABLE" else [],
            "free": [],
            "ads": [],
            "rent": [],
            "buy": [],
        }
        if available
        else None,
    }


@pytest.mark.parametrize(
    "outcome", ["AVAILABLE", "NO_OFFERS", "DISABLED", "UNMAPPED", "UNAVAILABLE", "RATE_LIMITED"]
)
def test_watch_results_never_create_catalog_identity(outcome: str) -> None:
    assert (
        parse_grounded_movies(ToolName.GET_MOVIE_WATCH_PROVIDERS, watch_result(outcome=outcome))
        == ()
    )


@pytest.mark.parametrize(
    "field,value",
    [
        ("country", "DE"),
        ("sourceUrl", "https://attacker.invalid"),
        ("source", "invented"),
        ("offers", None),
        ("fetchedAt", None),
        ("outcome", "NO_OFFERS"),
        ("outcome", "UNAVAILABLE"),
    ],
)
def test_rejects_mismatched_or_unverifiable_availability(field: str, value: object) -> None:
    result = watch_result()
    result[field] = value
    with pytest.raises(ValidationError):
        WatchProvidersResult.model_validate(result)


@pytest.mark.asyncio
async def test_country_precedence_and_live_preference_changes_do_not_change_saved_preference() -> (
    None
):
    movie = GroundedMovie(movie_id=6, primary_title="Forrest Gump", movie_type="MOVIE")
    turn = PersonalTurn(movies=(movie,))
    region = StreamingRegion()
    gate = PersonalToolGate(SecretStr("synthetic-login"), turn, region)
    calls: list[str] = []

    async def backend(
        name: str, args: dict[str, Any], *, metadata: dict[str, Any] | None = None
    ) -> dict[str, Any]:
        assert metadata is None  # A public lookup never forwards account credentials.
        calls.append(args["country"])
        return watch_result(args["country"])

    ctx = cast("RunContext[Any]", None)
    await gate.call(ctx, backend, "get_movie_watch_providers", {"movieId": 6})
    region.country = "AT"  # A browser context update during this session.
    await gate.call(ctx, backend, "get_movie_watch_providers", {"movieId": 6, "country": None})
    await gate.call(ctx, backend, "get_movie_watch_providers", {"movieId": 6, "country": "de"})
    await gate.call(ctx, backend, "get_movie_watch_providers", {"movieId": 6})
    assert calls == ["CH", "AT", "DE", "AT"]
    assert region.country == "AT"
    assert turn.movies == (movie,)
    assert turn.receipt is None


@pytest.mark.asyncio
async def test_unknown_movies_and_invalid_country_never_reach_provider() -> None:
    calls = 0

    async def backend(
        name: str, args: dict[str, Any], *, metadata: dict[str, Any] | None = None
    ) -> dict[str, Any]:
        nonlocal calls
        calls += 1
        return watch_result()

    turn = PersonalTurn(
        movies=(GroundedMovie(movie_id=6, primary_title="Forrest Gump", movie_type="MOVIE"),)
    )
    gate = PersonalToolGate(None, turn)
    ctx = cast("RunContext[Any]", None)
    for args in [{"movieId": 999}, {"movieId": 6, "country": "../CH"}]:
        with pytest.raises(ToolFailed):
            await gate.call(ctx, backend, "get_movie_watch_providers", args)
    assert calls == 0
    with pytest.raises(ToolFailed, match="do not match"):
        await gate.call(ctx, backend, "get_movie_watch_providers", {"movieId": 6, "country": "DE"})

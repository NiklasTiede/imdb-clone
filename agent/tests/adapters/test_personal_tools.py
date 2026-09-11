from __future__ import annotations

from typing import TYPE_CHECKING, Any, cast

import pytest
from pydantic import SecretStr
from pydantic_ai.exceptions import ToolFailed

from imdb_agent.adapters.personal_tools import PersonalToolGate
from imdb_agent.concierge.events import GroundedMovie
from imdb_agent.concierge.personal import PersonalTurn

if TYPE_CHECKING:
    from pydantic_ai import RunContext


def turn() -> PersonalTurn:
    state = PersonalTurn(
        movies=(GroundedMovie(movie_id=6, primary_title="Forrest Gump", movie_type="MOVIE"),)
    )
    state.finalize("Add Forrest Gump to my watchlist")
    return state


class Backend:
    def __init__(self) -> None:
        self.calls: list[dict[str, Any]] = []
        self.fail = False

    async def __call__(
        self, name: str, args: dict[str, Any], *, metadata: dict[str, Any] | None = None
    ) -> dict[str, Any]:
        assert metadata is not None
        self.calls.append(metadata)
        if self.fail:
            raise RuntimeError("private-backend-detail")
        return {
            "contractVersion": "1.0",
            "operationId": metadata["operationId"],
            "movieId": args["movieId"],
            "created": True,
            "addedAt": "2026-09-09T12:00:00Z",
        }


@pytest.mark.asyncio
async def test_injected_credential_and_operation_are_stable_on_retry() -> None:
    state = turn()
    backend = Backend()
    gate = PersonalToolGate(SecretStr("synthetic-session-a"), state)
    ctx = cast("RunContext[Any]", None)
    for _ in range(2):
        await gate.call(ctx, backend, "add_movie_to_my_watchlist", {"movieId": 6})
    assert backend.calls[0] == backend.calls[1]
    assert backend.calls[0]["delegation"] == "synthetic-session-a"
    assert state.receipt is not None


@pytest.mark.asyncio
@pytest.mark.parametrize("mode", ["anonymous", "cancelled", "wrong_movie"])
async def test_rejected_write_never_reaches_java(mode: str) -> None:
    state = turn()
    backend = Backend()
    if mode == "cancelled":
        state.cancelled = True
    gate = PersonalToolGate(None if mode == "anonymous" else SecretStr("synthetic"), state)
    with pytest.raises(ToolFailed):
        await gate.call(
            cast("RunContext[Any]", None),
            backend,
            "add_movie_to_my_watchlist",
            {"movieId": 7 if mode == "wrong_movie" else 6},
        )
    assert not backend.calls
    assert state.receipt is None


@pytest.mark.asyncio
async def test_failed_tool_does_not_emit_receipt_or_expose_backend_details() -> None:
    state = turn()
    backend = Backend()
    backend.fail = True
    with pytest.raises(ToolFailed, match="did not confirm success") as error:
        await PersonalToolGate(SecretStr("synthetic"), state).call(
            cast("RunContext[Any]", None), backend, "add_movie_to_my_watchlist", {"movieId": 6}
        )
    assert "private-backend-detail" not in str(error.value)
    assert state.receipt is None


@pytest.mark.asyncio
@pytest.mark.parametrize(
    ("message", "name", "kind", "score"),
    [
        ("Rate Forrest Gump eight point five", "set_my_movie_rating", "rating_set", 8.5),
        ("I'd give that one an eight point five", "set_my_movie_rating", "rating_set", 8.5),
        ("eight point five", "set_my_movie_rating", "rating_set", 8.5),
        ("Remove my rating for Forrest Gump", "remove_my_movie_rating", "rating_remove", None),
        (
            "Remove Forrest Gump from my watchlist",
            "remove_movie_from_my_watchlist",
            "watchlist_remove",
            None,
        ),
    ],
)
async def test_committed_changes_emit_the_correct_page_and_previous_state(
    message: str,
    name: str,
    kind: str,
    score: float | None,
) -> None:
    from imdb_agent.concierge.personal import receipt_action

    state = turn()
    state.finalize(message)
    calls: list[dict[str, Any]] = []

    async def backend(
        name: str,
        args: dict[str, Any],
        *,
        metadata: dict[str, Any] | None = None,
    ) -> dict[str, Any]:
        assert name and args["movieId"] == 6
        assert metadata is not None
        calls.append(metadata)
        return {
            "contractVersion": "1.0",
            "operationId": metadata["operationId"],
            "movieId": 6,
            "kind": kind,
            "changed": True,
            "score": score,
            "previousScore": 7.0 if kind.startswith("rating") else None,
        }

    args: dict[str, Any] = {"movieId": 6}
    if score is not None:
        args["score"] = score
    gate = PersonalToolGate(SecretStr("synthetic-session"), state)
    for _ in range(2):
        await gate.call(cast("RunContext[Any]", None), backend, name, args)
    assert calls[0] == calls[1]
    assert state.receipt is not None
    action = receipt_action(state.receipt)
    assert action.type == ("open_watchlist" if kind == "watchlist_remove" else "open_ratings")
    assert action.movie_id == 6


@pytest.mark.asyncio
@pytest.mark.parametrize("name", ["get_my_ratings", "get_my_recommendations"])
async def test_personal_reads_use_delegation_without_creating_write_receipts(name: str) -> None:
    state = turn()
    state.finalize("Which movies did I rate highest?")
    calls: list[dict[str, Any]] = []

    async def backend(
        name: str, args: dict[str, Any], *, metadata: dict[str, Any] | None = None
    ) -> dict[str, Any]:
        assert metadata is not None
        calls.append(metadata)
        movie = {"movieId": 6, "primaryTitle": "Forrest Gump", "type": "MOVIE", "imdbRating": 8.8}
        if name == "get_my_ratings":
            return {
                "contractVersion": "1.0",
                "ratings": [{"movie": movie, "userScore": 10.0, "ratedAt": "2026-09-10T00:00:00Z"}],
                "page": 0,
                "totalElements": 21,
                "last": False,
                "averageUserScore": 8.0,
                "favoriteGenres": [],
                "favoriteDecades": [],
            }
        return {
            "contractVersion": "1.0",
            "strategy": "personal-ratings-v1",
            "outcome": "MATCHED",
            "totalRatings": 21,
            "basedOn": [{"movieId": 7, "title": "Arrival", "userScore": 9.0}],
            "movies": [movie],
        }

    ctx = cast("RunContext[Any]", None)
    with pytest.raises(ToolFailed, match="Sign in"):
        await PersonalToolGate(None, state).call(ctx, backend, name, {})
    assert not calls
    result = await PersonalToolGate(SecretStr("synthetic-session"), state).call(
        ctx, backend, name, {}
    )
    assert isinstance(result, dict)
    assert result["contractVersion"] == "1.0"
    assert calls == [{"delegation": "synthetic-session"}]
    assert state.receipt is None
    assert state.movies[0].movie_id == 6 and state.movies[0].imdb_rating == 8.8
    assert state.movies[0].user_score == (10.0 if name == "get_my_ratings" else None)


@pytest.mark.asyncio
async def test_write_rejection_reason_survives_real_logging_filter_without_private_data(
    capsys: pytest.CaptureFixture[str],
) -> None:
    import json

    from imdb_agent.adapters.logging import configure_logging

    configure_logging(json_output=True)
    state = turn()
    state.finalize("Remove it from my watchlist")
    state.cancelled = True
    with pytest.raises(ToolFailed):
        await PersonalToolGate(SecretStr("synthetic-private-token"), state).call(
            cast("RunContext[Any]", None),
            Backend(),
            "remove_movie_from_my_watchlist",
            {"movieId": 6},
        )
    output = capsys.readouterr().out
    logged: dict[str, object] = json.loads(output)
    assert logged["event"] == "personal_write_rejected"
    assert logged["error_code"] == "inactive_turn"
    assert "Forrest Gump" not in output
    assert "synthetic-private-token" not in output

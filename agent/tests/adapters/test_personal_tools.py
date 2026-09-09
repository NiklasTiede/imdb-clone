from __future__ import annotations

import asyncio
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
@pytest.mark.parametrize("mode", ["anonymous", "cancelled", "wrong_movie", "negated"])
async def test_rejected_write_never_reaches_java(mode: str) -> None:
    state = turn()
    backend = Backend()
    if mode == "cancelled":
        state.cancelled = True
    if mode == "negated":
        state.finalize("Don't add Forrest Gump to my watchlist")
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
async def test_pending_final_transcript_cannot_authorize_a_different_turn() -> None:
    state = turn()
    state.finalized.clear()
    backend = Backend()
    gate = PersonalToolGate(SecretStr("synthetic"), state)
    pending = asyncio.create_task(
        gate.call(
            cast("RunContext[Any]", None), backend, "add_movie_to_my_watchlist", {"movieId": 6}
        )
    )
    await asyncio.sleep(0)
    assert not backend.calls
    state.begin()
    state.finalize("Add Forrest Gump to my watchlist")
    with pytest.raises(ToolFailed):
        await pending
    assert not backend.calls


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
    ("message", "name", "args"),
    [
        ("Rate Forrest Gump 8.5", "set_my_movie_rating", {"movieId": 6, "score": 9}),
        ("Rate Forrest Gump 8.5", "set_my_movie_rating", {"movieId": 7, "score": 8.5}),
        ("Rate Forrest Gump 8.5", "remove_my_movie_rating", {"movieId": 6}),
        ("Remove Forrest Gump from my watchlist", "add_movie_to_my_watchlist", {"movieId": 6}),
        ("Don't delete my rating for Forrest Gump", "remove_my_movie_rating", {"movieId": 6}),
        ("Give Forrest Gump a rating", "set_my_movie_rating", {"movieId": 6, "score": 8.5}),
    ],
)
async def test_model_cannot_change_the_requested_operation_movie_or_score(
    message: str,
    name: str,
    args: dict[str, Any],
) -> None:
    state = turn()
    state.finalize(message)
    backend = Backend()
    with pytest.raises(ToolFailed):
        await PersonalToolGate(SecretStr("synthetic"), state).call(
            cast("RunContext[Any]", None),
            backend,
            name,
            args,
        )
    assert not backend.calls


@pytest.mark.asyncio
@pytest.mark.parametrize(
    ("message", "name", "kind", "score"),
    [
        ("Rate Forrest Gump eight point five", "set_my_movie_rating", "rating_set", 8.5),
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

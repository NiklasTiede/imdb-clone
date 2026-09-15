"""Session ownership and personal-mutation validation, independent of providers."""

from __future__ import annotations

import asyncio
import re
from dataclasses import dataclass, field
from decimal import Decimal
from typing import TYPE_CHECKING, Literal, Protocol
from uuid import uuid4

from imdb_agent.concierge.events import OpenRatingsAction, OpenWatchlistAction
from imdb_agent.concierge.tools import WRITE_TOOLS, ToolName

if TYPE_CHECKING:
    from pydantic import SecretStr

    from imdb_agent.concierge.events import GroundedMovie


class DelegationRejectedError(Exception):
    """A credential could not be verified against the current Java login session."""


class DelegationVerifier(Protocol):
    async def verify(self, token: SecretStr) -> str: ...


def requests_watchlist(message: str) -> bool:
    return (
        re.fullmatch(
            r"(?:(?:can|could|would) you )?(?:please )?"
            r"(?:show|open|show me|read|list|what is on|what's on|what movies are on)"
            r" my watchlist(?: for me)?(?: please)?",
            normalize(message),
        )
        is not None
    )


def normalize(value: str) -> str:
    return " ".join(re.findall(r"[\w']+", value.casefold()))


@dataclass(frozen=True)
class PersonalMutation:
    tool: ToolName
    movie_id: int
    score: Decimal | None = None


WriteRejection = Literal["inactive_turn", "ungrounded_movie", "invalid_score", "second_mutation"]


def receipt_action(receipt: dict[str, object]) -> OpenWatchlistAction | OpenRatingsAction:
    common = {"operationId": receipt["operationId"], "movieId": receipt["movieId"]}
    if "created" in receipt:
        return OpenWatchlistAction.model_validate({**common, "created": receipt["created"]})
    if receipt["kind"] == "watchlist_remove":
        return OpenWatchlistAction.model_validate({**common, "removed": receipt["changed"]})
    return OpenRatingsAction.model_validate(
        {
            **common,
            "changed": receipt["changed"],
            "score": receipt["score"],
            "previousScore": receipt["previousScore"],
        }
    )


@dataclass
class PersonalTurn:
    """Session-owned grounding, turn lifecycle and idempotent personal mutations."""

    message: str = ""
    movies: tuple[GroundedMovie, ...] = ()
    epoch: int = 0
    cancelled: bool = False
    operation_id: str = field(default_factory=lambda: str(uuid4()))
    finalized: asyncio.Event = field(default_factory=asyncio.Event)
    receipt: dict[str, object] | None = None
    watchlist_read: bool = False
    catalog_seen: bool = False
    mutation: PersonalMutation | None = None
    opened_movie_id: int | None = None

    def begin(self) -> None:
        if self.opened_movie_id is not None and not self.cancelled:
            focused = tuple(m for m in self.movies if m.movie_id == self.opened_movie_id)
            if focused:
                self.movies = focused
        self.opened_movie_id = None
        self.mutation = None
        self.epoch += 1
        self.message = ""
        self.cancelled = False
        self.operation_id = str(uuid4())
        self.receipt = None
        self.watchlist_read = False
        self.catalog_seen = False
        self.finalized.clear()

    def claim_mutation(
        self,
        tool: ToolName,
        movie_id: object,
        score: object,
    ) -> WriteRejection | None:
        """Validate a model-interpreted request without re-parsing natural language.

        Intent, title aliases and follow-up references belong to the conversational model.
        Enforce grounded IDs, valid scores and one idempotent mutation per active user turn.
        Final ASR text is retained for history, not used as an additional write permission.
        """
        if (
            self.cancelled
            or (self.epoch == 0 and not self.finalized.is_set())
            or tool not in WRITE_TOOLS
        ):
            return "inactive_turn"
        if type(movie_id) is not int or not any(m.movie_id == movie_id for m in self.movies):
            return "ungrounded_movie"
        numeric: Decimal | None = None
        if tool == ToolName.SET_MY_MOVIE_RATING:
            if isinstance(score, bool) or not isinstance(score, (int, float, Decimal)):
                return "invalid_score"
            numeric = Decimal(str(score))
            if not numeric.is_finite() or not 0 <= numeric <= 10 or numeric % Decimal("0.1") != 0:
                return "invalid_score"
        elif score is not None:
            return "invalid_score"
        candidate = PersonalMutation(tool, movie_id, numeric)
        if self.mutation is not None and self.mutation != candidate:
            return "second_mutation"
        self.mutation = candidate
        return None

    def remember_movies(self, movies: tuple[GroundedMovie, ...]) -> None:
        # Keep every candidate; display narrowing is not write authority.
        if self.catalog_seen:
            known = {movie.movie_id: movie for movie in self.movies}
            known.update({movie.movie_id: movie for movie in movies})
            self.movies = tuple(known.values())
        else:
            self.movies = movies
        self.catalog_seen = True

    def finalize(self, message: str) -> None:
        self.message = message
        self.finalized.set()

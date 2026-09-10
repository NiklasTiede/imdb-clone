"""Session authorization and explicit personal-action policy, independent of providers."""

from __future__ import annotations

import asyncio
import re
from dataclasses import dataclass, field
from decimal import Decimal
from typing import TYPE_CHECKING, Protocol
from uuid import uuid4

from imdb_agent.concierge.events import OpenRatingsAction, OpenWatchlistAction
from imdb_agent.concierge.tools import ToolName

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


def add_target(message: str, movies: tuple[GroundedMovie, ...]) -> int | None:
    """Only a complete, unconditional command and one catalog-grounded target authorize a write."""
    patterns = (
        r"(?:add|save|put) (.+?) (?:to|on|in|onto) (?:my|the) watchlist",
        r"(?:i want|i'd like|i would like) (.+?) (?:on|in|added to|saved to) my watchlist",
        r"(?:add|save) (.+?) (?:to watch|for later)",
    )
    for pattern in patterns:
        match = re.fullmatch(_POLITE + pattern + _END, _command_text(message))
        if match is not None:
            return grounded_target(match[1], movies)
    return None


def grounded_target(target: str, movies: tuple[GroundedMovie, ...]) -> int | None:
    target = normalize(target)
    if target in {
        "it",
        "this",
        "that",
        "this movie",
        "that movie",
        "this film",
        "that film",
        "the movie",
        "this one",
        "that one",
        "the film",
    }:
        return movies[0].movie_id if len(movies) == 1 else None
    matches: set[int] = set()
    for movie in movies:
        titles = {
            normalize(movie.primary_title),
            normalize(movie.original_title or movie.primary_title),
        }
        if movie.start_year:
            titles |= {f"{title} {movie.start_year}" for title in titles}
        if any(target in {title, f"the movie {title}", f"the film {title}"} for title in titles):
            matches.add(movie.movie_id)
    return next(iter(matches)) if len(matches) == 1 else None


def _command_text(message: str) -> str:
    return " ".join(message.casefold().replace("\u2019", "'").strip().rstrip(".!?").split())


_POLITE = (
    r"(?:(?:hey|okay|ok|yes|yeah|well)[, ]+)?"
    r"(?:(?:(?:can|could|would|will) you(?: please)?|please) "
    r"|(?:i want|i'd like|i would like) (?:you to |to )|let's |let us |i'd |i would )?"
)
_END = (
    r"(?: and (?:show|open)(?: me)? "
    r"(?:it|my ratings|my ratings list|the ratings page|my watchlist|the watchlist))?"
    r"(?: for me)?(?:,? please)?(?:,? thanks)?"
)
_DIGIT = r"(?:zero|one|two|three|four|five|six|seven|eight|nine)"
_SCORE = rf"(?:\d+(?:\.\d+)?|ten|{_DIGIT}(?: point {_DIGIT})?)"


def _score(value: str) -> Decimal | None:
    words = dict(
        zip(
            ["zero", "one", "two", "three", "four", "five", "six", "seven", "eight", "nine", "ten"],
            range(11),
            strict=True,
        )
    )
    numeric = value
    if value in words:
        numeric = str(words[value])
    elif " point " in value:
        whole, fraction = value.split(" point ")
        numeric = f"{words[whole]}.{words[fraction]}"
    score = Decimal(numeric)
    return score if 0 <= score <= 10 and score == score.quantize(Decimal("0.1")) else None


@dataclass(frozen=True)
class PersonalCommand:
    tool: ToolName
    movie_id: int
    score: Decimal | None = None


def personal_command(message: str, movies: tuple[GroundedMovie, ...]) -> PersonalCommand | None:
    """Bind the target and personal score from final English commands outside the model."""
    added = add_target(message, movies)
    if added is not None:
        return PersonalCommand(ToolName.ADD_MOVIE_TO_MY_WATCHLIST, added)
    # Preserve decimal points and /10 in scores; title matching retains the existing normalizer.
    normalized = _command_text(message)
    patterns = (
        (
            ToolName.REMOVE_MOVIE_FROM_MY_WATCHLIST,
            r"(?:remove|delete|take|drop) (.+?) (?:from|off|out of) (?:my|the) watchlist",
        ),
        (ToolName.REMOVE_MY_MOVIE_RATING, r"(?:remove|delete|clear) my rating (?:for|of|on) (.+?)"),
        (ToolName.REMOVE_MY_MOVIE_RATING, r"(?:unrate) (.+?)"),
        (ToolName.REMOVE_MY_MOVIE_RATING, r"(?:remove|delete|clear) (.+?)'s rating"),
        (
            ToolName.REMOVE_MOVIE_FROM_MY_WATCHLIST,
            r"(?:take|cross) (.+?) off (?:my|the) (?:watchlist|list)",
        ),
        (
            ToolName.SET_MY_MOVIE_RATING,
            rf"(?:rate|score) (.+?) (?:a |an |at |as |with |with a |with an )?"
            rf"({_SCORE})(?: out of (?:ten|10)|/10)?",
        ),
        (
            ToolName.SET_MY_MOVIE_RATING,
            rf"give (.+?) (?:a |an |a rating of )?({_SCORE})(?: out of (?:ten|10)|/10)?",
        ),
        (
            ToolName.SET_MY_MOVIE_RATING,
            rf"(.+?) gets (?:a |an )?({_SCORE})(?: out of (?:ten|10)|/10)? from me",
        ),
        (
            ToolName.SET_MY_MOVIE_RATING,
            rf"(?:set|change|update) my rating (?:for|of|on) (.+?) to ({_SCORE})"
            r"(?: out of (?:ten|10)|/10)?",
        ),
    )
    for tool, pattern in patterns:
        match = re.fullmatch(_POLITE + pattern + _END, normalized)
        if match is None:
            continue
        target = grounded_target(match[1], movies)
        score = _score(match[2]) if tool == ToolName.SET_MY_MOVIE_RATING else None
        if target is not None and (tool != ToolName.SET_MY_MOVIE_RATING or score is not None):
            return PersonalCommand(tool, target, score)
    return None


def pending_rating_target(message: str, movies: tuple[GroundedMovie, ...]) -> int | None:
    """Remember only a rating request whose sole missing detail is the personal score."""
    match = re.fullmatch(
        _POLITE + r"(?:(?:rate|score) (.+?)|give (.+?) (?:a |my )rating)" + _END,
        _command_text(message),
    )
    return grounded_target(match[1] or match[2], movies) if match else None


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
    """One run owns final speech and grounding outside model-controlled arguments."""

    message: str = ""
    movies: tuple[GroundedMovie, ...] = ()
    epoch: int = 0
    cancelled: bool = False
    operation_id: str = field(default_factory=lambda: str(uuid4()))
    finalized: asyncio.Event = field(default_factory=asyncio.Event)
    receipt: dict[str, object] | None = None
    watchlist_read: bool = False
    catalog_seen: bool = False
    pending_rating_id: int | None = None
    opened_movie_id: int | None = None

    def begin(self) -> None:
        if self.opened_movie_id is not None and not self.cancelled:
            focused = tuple(m for m in self.movies if m.movie_id == self.opened_movie_id)
            if focused:
                self.movies = focused
        self.opened_movie_id = None
        self.pending_rating_id = (
            pending_rating_target(self.message, self.movies)
            if self.finalized.is_set() and not self.cancelled and self.receipt is None
            else None
        )
        self.epoch += 1
        self.message = ""
        self.cancelled = False
        self.operation_id = str(uuid4())
        self.receipt = None
        self.watchlist_read = False
        self.catalog_seen = False
        self.finalized.clear()

    def command(self) -> PersonalCommand | None:
        direct = personal_command(self.message, self.movies)
        if direct is not None:
            return direct
        if self.pending_rating_id is None or not any(
            movie.movie_id == self.pending_rating_id for movie in self.movies
        ):
            return None
        match = re.fullmatch(
            _POLITE + rf"(?:a |an )?({_SCORE})(?: out of (?:ten|10)|/10)?" + _END,
            _command_text(self.message),
        )
        score = _score(match[1]) if match else None
        return (
            PersonalCommand(ToolName.SET_MY_MOVIE_RATING, self.pending_rating_id, score)
            if score is not None
            else None
        )

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

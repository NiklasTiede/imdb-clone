"""Provider-neutral voice contract and session-owned navigation grounding."""

from __future__ import annotations

import re
from dataclasses import dataclass, field
from typing import Literal, Protocol

from pydantic import Field, SecretStr

from imdb_agent.concierge.events import (
    ApplicationAction,
    EventModel,
    GroundedMovie,
    OpenMovieAction,
)
from imdb_agent.concierge.policy import decide_open_movie_action

SAMPLE_RATE = 24_000
PCM_BYTES_PER_SECOND = SAMPLE_RATE * 2


class VoiceCommand(EventModel):
    type: Literal["interrupt", "mute", "resume", "end"]


class VoiceEvent(EventModel):
    type: Literal[
        "ready",
        "status",
        "transcript",
        "movie-card",
        "ui-action",
        "interrupt",
        "reply-complete",
        "error",
    ]
    status: Literal["listening", "thinking", "searching", "muted"] | None = None
    speaker: Literal["user", "assistant"] | None = None
    text: str | None = Field(default=None, max_length=6_000)
    final: bool = False
    turn: int = Field(default=0, ge=0)
    movie: GroundedMovie | None = None
    action: ApplicationAction | None = None


class VoiceTransport(Protocol):
    async def receive(self) -> bytes | VoiceCommand: ...

    async def send(self, event: VoiceEvent | bytes) -> None: ...


class VoiceRunner(Protocol):
    async def run(self, transport: VoiceTransport, delegation: SecretStr | None = None) -> None: ...


class VoiceDisconnectedError(Exception):
    """The browser ended its connection."""


class VoiceSessionLimitError(Exception):
    """The bounded voice session exhausted its model or tool usage budget."""


@dataclass
class VoiceGrounding:
    """Only final speech and Java-owned cards may authorize local navigation."""

    turn: int = 0
    message: str = ""
    movies: dict[int, GroundedMovie] = field(default_factory=lambda: dict[int, GroundedMovie]())
    previous: tuple[GroundedMovie, ...] = ()
    tools_called: bool = False
    completed: bool = False
    cancelled: bool = False
    action_sent: bool = False

    def begin(self) -> None:
        if self.movies and not self.cancelled:
            self.previous = tuple(self.movies.values())
        self.turn += 1
        self.message = ""
        self.movies.clear()
        self.tools_called = False
        self.completed = False
        self.cancelled = False
        self.action_sent = False

    def action(self) -> OpenMovieAction | None:
        if not self.message or self.cancelled or self.action_sent:
            return None
        candidates = tuple(self.movies.values()) if self.tools_called else self.previous
        if not self.completed and (
            len(candidates) != 1 or not _direct_open_request(self.message, candidates[0])
        ):
            return None
        action = decide_open_movie_action(self.message, candidates).action
        if action:
            self.action_sent = True
        return action


def _direct_open_request(message: str, movie: GroundedMovie) -> bool:
    """Fast-path only complete, unconditional commands for one grounded title.

    Compound requests and conditions still wait for the full agent turn. Do not
    interpret a streaming transcript prefix as permission to navigate.
    """
    titles = {movie.primary_title, movie.original_title or movie.primary_title}
    if movie.start_year is not None:
        titles |= {f"{title} {movie.start_year}" for title in titles}

    def normalize(value: str) -> str:
        return " ".join(re.findall(r"\w+", value.casefold()))

    target = "|".join(re.escape(normalize(title)) for title in sorted(titles))
    named = rf"(?:(?:the )?(?:movie |film )?)?(?:{target})(?: movie page| page)?"
    contextual = r"(?:it|that one|that movie|that film|this movie|this film)"
    direct = rf"(?:open|show|show me|take me to|go to|navigate to) (?:{named}|{contextual})"
    find = rf"find {named} and open (?:it|its page|its movie page|the movie page)"
    return (
        re.fullmatch(
            rf"(?:(?:can|could|would) you )?(?:please )?(?:{direct}|{find})"
            r"(?: for me)?(?: please)?",
            normalize(message),
        )
        is not None
    )

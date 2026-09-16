"""Rolling voice-time budgets shared by providers, keyed by anonymous browser identity."""

from __future__ import annotations

from dataclasses import dataclass
from math import ceil
from threading import Lock
from time import time
from typing import TYPE_CHECKING, Literal, Protocol
from uuid import uuid4

if TYPE_CHECKING:
    from collections.abc import Callable

WINDOW_SECONDS = 86400
SLICE_SECONDS = 5.0


class VoiceQuotaUnavailable(Exception):
    """Admission must fail closed when the durable budget cannot be read."""


@dataclass(frozen=True)
class VoiceGrant:
    id: str
    seconds: float
    remaining: float
    scope: Literal["browser", "shared"]
    retry_seconds: int


@dataclass
class VoiceUsage:
    id: str
    browser_id: str
    started_at: float
    seconds: float


def grant_time(
    usage: list[VoiceUsage],
    browser_id: str,
    browser_seconds: float,
    shared_seconds: float,
    now: float,
) -> VoiceGrant:
    personal = [row for row in usage if row.browser_id == browser_id]
    browser_left = browser_seconds - sum(row.seconds for row in personal)
    shared_left = shared_seconds - sum(row.seconds for row in usage)
    scope = "browser" if browser_left <= shared_left else "shared"
    remaining = max(0.0, min(browser_left, shared_left))
    rows = personal if scope == "browser" else usage
    retry = max(1, ceil(min((row.started_at for row in rows), default=now) + WINDOW_SECONDS - now))
    return VoiceGrant(uuid4().hex, min(SLICE_SECONDS, remaining), remaining, scope, retry)


class VoiceQuota(Protocol):
    def reserve(self, browser_id: str) -> VoiceGrant: ...
    def settle(self, grant_id: str, used_seconds: float) -> None: ...


class MemoryVoiceQuota:
    def __init__(
        self,
        browser_seconds: float = 1500,
        shared_seconds: float = 6000,
        *,
        clock: Callable[[], float] = time,
    ) -> None:
        self.browser_seconds, self.shared_seconds, self.clock = (
            browser_seconds,
            shared_seconds,
            clock,
        )
        self.usage: list[VoiceUsage] = []
        self.lock = Lock()

    def reserve(self, browser_id: str) -> VoiceGrant:
        with self.lock:
            now = self.clock()
            self.usage = [row for row in self.usage if row.started_at > now - WINDOW_SECONDS]
            grant = grant_time(
                self.usage, browser_id, self.browser_seconds, self.shared_seconds, now
            )
            if grant.seconds > 0:
                self.usage.append(VoiceUsage(grant.id, browser_id, now, grant.seconds))
            return grant

    def settle(self, grant_id: str, used_seconds: float) -> None:
        with self.lock:
            for row in self.usage:
                if row.id == grant_id:
                    row.seconds = min(row.seconds, max(0.0, used_seconds))
            self.usage = [row for row in self.usage if row.seconds > 0]

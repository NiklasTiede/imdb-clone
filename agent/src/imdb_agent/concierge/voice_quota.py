"""Shared admission budget for both voice providers; no conversation data is retained."""

from __future__ import annotations

from collections import deque
from math import ceil
from threading import Lock
from time import time
from typing import TYPE_CHECKING, Protocol

if TYPE_CHECKING:
    from collections.abc import Callable


class VoiceQuotaUnavailable(Exception):
    """Admission must fail closed when the durable budget cannot be read."""


class VoiceQuota(Protocol):
    def reserve(self) -> int:
        """Reserve one start, returning zero; otherwise return seconds until capacity returns."""
        ...


class MemoryVoiceQuota:
    def __init__(self, maximum: int, *, clock: Callable[[], float] = time) -> None:
        self.maximum = maximum
        self.clock = clock
        self.starts: deque[float] = deque()
        self.lock = Lock()

    def reserve(self) -> int:
        with self.lock:
            return self._reserve()

    def _reserve(self) -> int:
        now = self.clock()
        while self.starts and self.starts[0] <= now - 86400:
            self.starts.popleft()
        if len(self.starts) >= self.maximum:
            return max(1, ceil(self.starts[0] + 86400 - now))
        self.starts.append(now)
        return 0

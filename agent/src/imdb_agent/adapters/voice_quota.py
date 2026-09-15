"""A tiny durable admission ledger for the single-pod voice pilot."""

from __future__ import annotations

import sqlite3
from contextlib import closing
from math import ceil
from time import time
from typing import TYPE_CHECKING

if TYPE_CHECKING:
    from collections.abc import Callable
    from pathlib import Path

from imdb_agent.concierge.voice_quota import VoiceQuotaUnavailable


class SqliteVoiceQuota:
    def __init__(self, path: Path, maximum: int, *, clock: Callable[[], float] = time) -> None:
        self.path, self.maximum, self.clock = path, maximum, clock
        try:
            path.parent.mkdir(parents=True, exist_ok=True)
            with closing(sqlite3.connect(path, timeout=1)) as connection, connection:
                connection.execute(
                    "CREATE TABLE IF NOT EXISTS voice_starts (started_at REAL NOT NULL)"
                )
            path.chmod(0o600)
        except OSError, sqlite3.Error:
            raise VoiceQuotaUnavailable from None

    def reserve(self) -> int:
        try:
            # The write lock makes check+insert atomic even across competing connections.
            with closing(sqlite3.connect(self.path, timeout=1)) as connection, connection:
                connection.execute("BEGIN IMMEDIATE")
                now = self.clock()
                connection.execute("DELETE FROM voice_starts WHERE started_at <= ?", (now - 86400,))
                count, oldest = connection.execute(
                    "SELECT COUNT(*), MIN(started_at) FROM voice_starts"
                ).fetchone()
                if count >= self.maximum:
                    return max(1, ceil(oldest + 86400 - now))
                connection.execute("INSERT INTO voice_starts VALUES (?)", (now,))
                return 0
        except sqlite3.Error:
            raise VoiceQuotaUnavailable from None

"""Durable five-second reservations; no audio, transcripts or IP addresses are retained."""

from __future__ import annotations

import sqlite3
from contextlib import closing
from time import time
from typing import TYPE_CHECKING

from imdb_agent.concierge.voice_quota import (
    WINDOW_SECONDS,
    VoiceGrant,
    VoiceQuotaUnavailable,
    VoiceUsage,
    grant_time,
)

if TYPE_CHECKING:
    from collections.abc import Callable
    from pathlib import Path


class SqliteVoiceQuota:
    def __init__(
        self,
        path: Path,
        browser_seconds: float = 1500,
        shared_seconds: float = 6000,
        *,
        clock: Callable[[], float] = time,
    ) -> None:
        self.path, self.clock = path, clock
        self.browser_seconds, self.shared_seconds = browser_seconds, shared_seconds
        try:
            path.parent.mkdir(parents=True, exist_ok=True)
            with closing(sqlite3.connect(path, timeout=1)) as connection, connection:
                # Keep the old voice_starts ledger for rollback; starts cannot be converted to time.
                connection.execute(
                    "CREATE TABLE IF NOT EXISTS voice_usage "
                    "(id TEXT PRIMARY KEY, browser_id TEXT NOT NULL, "
                    "started_at REAL NOT NULL, seconds REAL NOT NULL CHECK(seconds >= 0))"
                )
            path.chmod(0o600)
        except OSError, sqlite3.Error:
            raise VoiceQuotaUnavailable from None

    def reserve(self, browser_id: str) -> VoiceGrant:
        try:
            with closing(sqlite3.connect(self.path, timeout=1)) as connection, connection:
                connection.execute("BEGIN IMMEDIATE")
                now = self.clock()
                connection.execute(
                    "DELETE FROM voice_usage WHERE started_at <= ?", (now - WINDOW_SECONDS,)
                )
                rows = connection.execute(
                    "SELECT id, browser_id, started_at, seconds FROM voice_usage"
                ).fetchall()
                usage = [
                    VoiceUsage(str(row[0]), str(row[1]), float(row[2]), float(row[3]))
                    for row in rows
                ]
                grant = grant_time(
                    usage, browser_id, self.browser_seconds, self.shared_seconds, now
                )
                if grant.seconds > 0:
                    connection.execute(
                        "INSERT INTO voice_usage VALUES (?, ?, ?, ?)",
                        (grant.id, browser_id, now, grant.seconds),
                    )
                return grant
        except sqlite3.Error:
            raise VoiceQuotaUnavailable from None

    def settle(self, grant_id: str, used_seconds: float) -> None:
        try:
            with closing(sqlite3.connect(self.path, timeout=1)) as connection, connection:
                connection.execute(
                    "UPDATE voice_usage SET seconds = MIN(seconds, ?) WHERE id = ?",
                    (max(0.0, used_seconds), grant_id),
                )
                connection.execute(
                    "DELETE FROM voice_usage WHERE id = ? AND seconds = 0", (grant_id,)
                )
        except sqlite3.Error:
            raise VoiceQuotaUnavailable from None

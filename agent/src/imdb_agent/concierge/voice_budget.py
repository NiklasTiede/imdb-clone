"""Meter connected time without putting database IO on the realtime audio path."""

from __future__ import annotations

import asyncio
from math import ceil
from time import monotonic
from typing import TYPE_CHECKING

from imdb_agent.concierge.voice import VoiceEvent

if TYPE_CHECKING:
    from imdb_agent.concierge.voice import VoiceTransport
    from imdb_agent.concierge.voice_quota import VoiceGrant, VoiceQuota


class VoiceTimeLimitError(Exception):
    def __init__(self, grant: VoiceGrant) -> None:
        subject = (
            "This browser's voice time" if grant.scope == "browser" else "The shared voice time"
        )
        super().__init__(
            f"{subject} allowance for the last 24 hours is used up. "
            f"Try again in {ceil(grant.retry_seconds / 60)} minutes."
        )


class VoiceBudget:
    def __init__(self, quota: VoiceQuota, browser_id: str) -> None:
        self.quota, self.browser_id = quota, browser_id
        self.grant: VoiceGrant | None = None
        self.started_at: float | None = None
        self.ready = asyncio.Event()

    async def reserve(self) -> None:
        task = asyncio.create_task(asyncio.to_thread(self.quota.reserve, self.browser_id))
        try:
            grant = await asyncio.shield(task)
        except asyncio.CancelledError:
            grant = await task
            await asyncio.to_thread(self.quota.settle, grant.id, 0)
            raise
        if grant.seconds <= 0.001:
            await asyncio.to_thread(self.quota.settle, grant.id, 0)
            raise VoiceTimeLimitError(grant)
        self.grant = grant
        self.started_at = None

    def start(self) -> None:
        if not self.ready.is_set():
            self.started_at = monotonic()
            self.ready.set()

    async def monitor(self, transport: VoiceTransport) -> None:
        await self.ready.wait()
        warned = False
        while self.grant is not None and self.started_at is not None:
            if self.grant.remaining <= 60 and not warned:
                warned = True
                subject = "this browser's" if self.grant.scope == "browser" else "the shared"
                await transport.send(
                    VoiceEvent(
                        type="quota-warning",
                        text=f"Less than a minute remains in {subject} voice time allowance.",
                    )
                )
            next_start = self.started_at + self.grant.seconds
            await asyncio.sleep(max(0, next_start - monotonic()))
            # The completed slice stays charged. Reserve the next slice atomically across tabs.
            self.grant = None
            self.started_at = None
            await self.reserve()
            self.started_at = next_start

    async def close(self) -> None:
        if self.grant is not None:
            used = 0 if self.started_at is None else max(0, monotonic() - self.started_at)
            await asyncio.to_thread(self.quota.settle, self.grant.id, used)
            self.grant = None

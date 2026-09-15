import asyncio
from typing import TYPE_CHECKING

import pytest

if TYPE_CHECKING:
    from imdb_agent.concierge.voice import VoiceCommand, VoiceEvent
from imdb_agent.concierge.voice_budget import VoiceBudget, VoiceTimeLimitError
from imdb_agent.concierge.voice_quota import MemoryVoiceQuota


class Transport:
    def __init__(self) -> None:
        self.events: list[VoiceEvent | bytes] = []

    async def send(self, event: VoiceEvent | bytes) -> None:
        self.events.append(event)

    async def receive(self) -> bytes | VoiceCommand:
        raise NotImplementedError


@pytest.mark.asyncio
async def test_connected_time_consumes_slices_and_warns_once(
    monkeypatch: pytest.MonkeyPatch,
) -> None:
    now = 100.0
    original_sleep = asyncio.sleep

    async def sleep(seconds: float) -> None:
        nonlocal now
        now += seconds
        await original_sleep(0)

    monkeypatch.setattr("imdb_agent.concierge.voice_budget.monotonic", lambda: now)
    monkeypatch.setattr("imdb_agent.concierge.voice_budget.asyncio.sleep", sleep)
    quota = MemoryVoiceQuota(12, 60)
    budget = VoiceBudget(quota, "a")
    await budget.reserve()
    # Connection setup is not billed.
    now += 20
    budget.start()
    transport = Transport()
    with pytest.raises(VoiceTimeLimitError, match="browser's voice time"):
        await budget.monitor(transport)
    assert now == 132
    assert len(transport.events) == 1
    assert quota.reserve("a").seconds == 0
    assert quota.reserve("b").seconds == 5
    await budget.close()


@pytest.mark.asyncio
async def test_disconnect_refunds_unused_time_and_failed_setup_is_free(
    monkeypatch: pytest.MonkeyPatch,
) -> None:
    now = 100.0
    monkeypatch.setattr("imdb_agent.concierge.voice_budget.monotonic", lambda: now)
    quota = MemoryVoiceQuota(1200)
    budget = VoiceBudget(quota, "a")
    await budget.reserve()
    now += 30
    await budget.close()
    assert not quota.usage
    await budget.reserve()
    budget.start()
    now += 1.25
    await budget.close()
    await budget.close()
    assert sum(row.seconds for row in quota.usage) == 1.25


@pytest.mark.asyncio
async def test_budget_waits_for_provider_ready_without_billing() -> None:
    quota = MemoryVoiceQuota(1200)
    budget = VoiceBudget(quota, "a")
    await budget.reserve()
    task = asyncio.create_task(budget.monitor(Transport()))
    await asyncio.sleep(0)
    task.cancel()
    with pytest.raises(asyncio.CancelledError):
        await task
    await budget.close()
    assert not quota.usage


@pytest.mark.asyncio
async def test_cancellation_during_reservation_refunds_the_completed_write() -> None:
    from threading import Event

    entered, release = Event(), Event()

    class SlowQuota(MemoryVoiceQuota):
        def reserve(self, browser_id: str):
            entered.set()
            release.wait(timeout=2)
            return super().reserve(browser_id)

    quota = SlowQuota()
    budget = VoiceBudget(quota, "a")
    task = asyncio.create_task(budget.reserve())
    await asyncio.to_thread(entered.wait, 2)
    task.cancel()
    release.set()
    with pytest.raises(asyncio.CancelledError):
        await task
    assert not quota.usage

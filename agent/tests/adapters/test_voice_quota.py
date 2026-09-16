from concurrent.futures import ThreadPoolExecutor
from typing import TYPE_CHECKING

import pytest

from popcorn_society_agent.adapters.voice_quota import SqliteVoiceQuota
from popcorn_society_agent.concierge.voice_quota import (
    MemoryVoiceQuota,
    VoiceGrant,
    VoiceQuota,
    VoiceQuotaUnavailable,
)

if TYPE_CHECKING:
    from pathlib import Path


@pytest.fixture(params=[False, True])
def quota(request: pytest.FixtureRequest, tmp_path: Path) -> VoiceQuota:
    return (
        SqliteVoiceQuota(tmp_path / "quota.db", 1200, 6000)
        if request.param
        else MemoryVoiceQuota(1200, 6000)
    )


def test_restarts_charge_only_used_time_and_browser_has_twenty_minutes(quota: VoiceQuota) -> None:
    for _ in range(100):
        grant = quota.reserve("browser-a")
        quota.settle(grant.id, 1.5)
    for _ in range(210):
        assert quota.reserve("browser-a").seconds == 5
    denied = quota.reserve("browser-a")
    assert denied.seconds == 0
    assert denied.scope == "browser"
    assert quota.reserve("browser-b").seconds == 5


def test_failed_start_refunds_entire_reservation_and_settlement_is_idempotent(
    quota: VoiceQuota,
) -> None:
    grant = quota.reserve("browser-a")
    quota.settle(grant.id, 0)
    quota.settle(grant.id, 5)
    assert quota.reserve("browser-a").remaining == 1200


@pytest.mark.parametrize("persistent", [False, True])
def test_time_returns_in_rolling_slices(persistent: bool, tmp_path: Path) -> None:
    now = 100_000.0
    quota = (
        SqliteVoiceQuota(tmp_path / "quota.db", 10, 20, clock=lambda: now)
        if persistent
        else MemoryVoiceQuota(10, 20, clock=lambda: now)
    )
    quota.reserve("a")
    now += 60
    quota.reserve("a")
    assert quota.reserve("a").retry_seconds == 86340
    now += 86340
    assert quota.reserve("a").seconds == 5
    assert quota.reserve("a").retry_seconds == 60


def test_quota_survives_restart_and_atomic_shared_reservations(tmp_path: Path) -> None:
    path = tmp_path / "quota.db"
    quotas = [SqliteVoiceQuota(path, 1200, 40, clock=lambda: 100_000) for _ in range(16)]

    def reserve(item: tuple[int, SqliteVoiceQuota]) -> VoiceGrant:
        return item[1].reserve(str(item[0]))

    with ThreadPoolExecutor(max_workers=4) as pool:
        results = list(pool.map(reserve, enumerate(quotas)))
    assert sum(grant.seconds for grant in results) == 40
    denied = SqliteVoiceQuota(path, 1200, 40, clock=lambda: 100_001).reserve("new-browser")
    assert denied.seconds == 0
    assert denied.scope == "shared"
    assert denied.retry_seconds == 86399
    assert path.stat().st_mode & 0o777 == 0o600


def test_unavailable_ledger_never_grants_admission(tmp_path: Path) -> None:
    path = tmp_path / "quota.db"
    quota = SqliteVoiceQuota(path)
    path.write_text("invalid database")
    with pytest.raises(VoiceQuotaUnavailable):
        quota.reserve("a")
    with pytest.raises(VoiceQuotaUnavailable):
        quota.settle("missing", 0)
    with pytest.raises(VoiceQuotaUnavailable):
        SqliteVoiceQuota(path)

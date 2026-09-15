from concurrent.futures import ThreadPoolExecutor
from typing import TYPE_CHECKING

import pytest

from imdb_agent.adapters.voice_quota import SqliteVoiceQuota
from imdb_agent.concierge.voice_quota import MemoryVoiceQuota, VoiceQuotaUnavailable

if TYPE_CHECKING:
    from pathlib import Path


@pytest.mark.parametrize("persistent", [False, True])
def test_eight_starts_use_a_rolling_window(persistent: bool, tmp_path: Path) -> None:
    now = 100_000.0
    quota = (
        SqliteVoiceQuota(tmp_path / "quota.db", 8, clock=lambda: now)
        if persistent
        else MemoryVoiceQuota(8, clock=lambda: now)
    )
    assert quota.reserve() == 0
    now += 60
    assert [quota.reserve() for _ in range(7)] == [0] * 7
    assert quota.reserve() == 86340
    now += 86340
    assert quota.reserve() == 0
    assert quota.reserve() == 60


def test_quota_survives_restart_and_concurrent_connections(tmp_path: Path) -> None:
    path = tmp_path / "quota.db"
    quotas = [SqliteVoiceQuota(path, 8, clock=lambda: 100_000) for _ in range(16)]
    with ThreadPoolExecutor(max_workers=4) as pool:
        results = list(pool.map(SqliteVoiceQuota.reserve, quotas))
    assert results.count(0) == 8
    assert results.count(86400) == 8
    assert SqliteVoiceQuota(path, 8, clock=lambda: 100_001).reserve() == 86399
    assert path.stat().st_mode & 0o777 == 0o600


def test_unavailable_ledger_never_grants_admission(tmp_path: Path) -> None:
    path = tmp_path / "quota.db"
    quota = SqliteVoiceQuota(path, 8)
    path.write_text("invalid database")
    with pytest.raises(VoiceQuotaUnavailable):
        quota.reserve()
    with pytest.raises(VoiceQuotaUnavailable):
        SqliteVoiceQuota(path, 8)

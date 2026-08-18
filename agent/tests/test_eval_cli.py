from __future__ import annotations

from typing import TYPE_CHECKING

from imdb_agent.eval_cli import main

if TYPE_CHECKING:
    import pytest


def test_live_eval_requires_both_flag_and_environment_opt_in(
    monkeypatch: pytest.MonkeyPatch, capsys: pytest.CaptureFixture[str]
) -> None:
    monkeypatch.delenv("IMDB_AGENT_LIVE_EVALS_ENABLED", raising=False)

    result = main(["--live", "--case", "exact-title-search"])

    assert result == 2
    assert "Live evals are disabled" in capsys.readouterr().out

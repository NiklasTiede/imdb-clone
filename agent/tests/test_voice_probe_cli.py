from typing import TYPE_CHECKING

import pytest

from imdb_agent.settings import ConfigurationError, load_local_voice_secrets
from imdb_agent.voice_probe_cli import main

if TYPE_CHECKING:
    from pathlib import Path


def test_voice_key_is_explicit_literal_and_redacted(
    tmp_path: Path, monkeypatch: pytest.MonkeyPatch
) -> None:
    monkeypatch.setenv("XAI_API_KEY", "wrong-environment-value")
    monkeypatch.setenv("EXPANSION", "must-not-expand")
    path = tmp_path / "voice.env"
    path.write_text("XAI_API_KEY=synthetic-${EXPANSION}-value\n", encoding="utf-8")
    secret = load_local_voice_secrets(path)
    assert secret.xai_api_key.get_secret_value() == "synthetic-${EXPANSION}-value"
    assert "synthetic-" not in repr(secret)


@pytest.mark.parametrize(
    "contents", ["", "XAI_API_KEY=short", "XAI_API_KEY=synthetic-value\nOTHER=no"]
)
def test_invalid_voice_secrets_have_safe_errors(tmp_path: Path, contents: str) -> None:
    path = tmp_path / "voice.env"
    path.write_text(contents, encoding="utf-8")
    with pytest.raises(ConfigurationError) as error:
        load_local_voice_secrets(path)
    assert "synthetic-value" not in str(error.value)
    assert "short" not in str(error.value)


def test_missing_key_is_safe(tmp_path: Path) -> None:
    with pytest.raises(ConfigurationError, match="xAI credentials are unavailable"):
        load_local_voice_secrets(tmp_path / "missing.env")


def test_probe_requires_explicit_live_flag() -> None:
    assert main([]) == 2


def test_probe_requires_environment_opt_in(monkeypatch: pytest.MonkeyPatch) -> None:
    monkeypatch.setenv("IMDB_AGENT_LIVE_EVALS_ENABLED", "false")
    assert main(["--live"]) == 2

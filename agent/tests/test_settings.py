from __future__ import annotations

from typing import TYPE_CHECKING

import pytest

from imdb_agent.settings import ConfigurationError, DeploymentEnvironment, load_settings

if TYPE_CHECKING:
    from pathlib import Path


def test_settings_load_prefixed_environment(monkeypatch: pytest.MonkeyPatch) -> None:
    monkeypatch.setenv("IMDB_AGENT_ENVIRONMENT", "production")
    monkeypatch.setenv("IMDB_AGENT_PORT", "9000")

    settings = load_settings()

    assert settings.environment is DeploymentEnvironment.PRODUCTION
    assert settings.port == 9000
    assert settings.json_logs is True


def test_invalid_environment_value_is_rejected(monkeypatch: pytest.MonkeyPatch) -> None:
    monkeypatch.setenv("IMDB_AGENT_PORT", "not-a-port")

    with pytest.raises(ConfigurationError, match="invalid Movie Concierge configuration"):
        load_settings()


def test_unknown_dotenv_field_is_rejected(tmp_path: Path) -> None:
    env_file = tmp_path / ".env"
    env_file.write_text("IMDB_AGENT_UNKNOWN=value\n", encoding="utf-8")

    with pytest.raises(ConfigurationError, match="invalid Movie Concierge configuration"):
        load_settings(env_file)


def test_secret_is_redacted_from_settings_representation(
    monkeypatch: pytest.MonkeyPatch,
) -> None:
    sensitive_value = "synthetic-sensitive-value"
    monkeypatch.setenv("IMDB_AGENT_MCP_BEARER_TOKEN", sensitive_value)

    settings = load_settings()

    assert sensitive_value not in repr(settings)
    assert sensitive_value not in str(settings.model_dump())

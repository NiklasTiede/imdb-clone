from __future__ import annotations

from typing import TYPE_CHECKING

import pytest

from imdb_agent.settings import (
    ConfigurationError,
    DeploymentEnvironment,
    load_openai_secrets,
    load_settings,
)

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


def test_openai_key_is_loaded_only_from_explicit_secret_file(
    monkeypatch: pytest.MonkeyPatch, tmp_path: Path
) -> None:
    monkeypatch.setenv("OPENAI_API_KEY", "must-not-be-used")
    secret_file = tmp_path / "movie-concierge.local.env"
    secret_file.write_text("OPENAI_API_KEY=file-only-value\n", encoding="utf-8")

    secrets = load_openai_secrets(secret_file)

    assert secrets.openai_api_key.get_secret_value() == "file-only-value"
    assert "file-only-value" not in repr(secrets)


def test_openai_secret_file_rejects_unknown_or_missing_fields(tmp_path: Path) -> None:
    secret_file = tmp_path / "movie-concierge.local.env"
    secret_file.write_text("UNEXPECTED=value\n", encoding="utf-8")

    with pytest.raises(ConfigurationError, match="OpenAI credentials are unavailable"):
        load_openai_secrets(secret_file)

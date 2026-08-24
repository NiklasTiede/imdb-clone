from __future__ import annotations

from pathlib import Path

import pytest

from imdb_agent.settings import (
    ConfigurationError,
    DeploymentEnvironment,
    Settings,
    load_local_openai_secrets,
    load_runtime_secrets,
    load_settings,
)


def test_settings_load_prefixed_environment(monkeypatch: pytest.MonkeyPatch) -> None:
    monkeypatch.setenv("IMDB_AGENT_ENVIRONMENT", "production")
    monkeypatch.setenv("IMDB_AGENT_PORT", "9000")
    monkeypatch.setenv("IMDB_AGENT_SECRETS_DIRECTORY", "/run/secrets/movie-concierge")
    monkeypatch.setenv(
        "IMDB_AGENT_MCP_URL",
        "http://imdb-clone-backend.imdb-clone.svc.cluster.local:8080/mcp",
    )
    monkeypatch.setenv(
        "IMDB_AGENT_ALLOWED_HOSTS",
        '["imdb-clone.the-coding-lab.com","imdb-clone-agent.imdb-clone.svc"]',
    )

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
    secret_file.write_text("OPENAI_API_KEY=file-only-synthetic-value\n", encoding="utf-8")

    secrets = load_local_openai_secrets(secret_file)

    assert secrets.openai_api_key.get_secret_value() == "file-only-synthetic-value"
    assert "file-only-synthetic-value" not in repr(secrets)


def test_openai_secret_file_rejects_unknown_or_missing_fields(tmp_path: Path) -> None:
    secret_file = tmp_path / "movie-concierge.local.env"
    secret_file.write_text("UNEXPECTED=value\n", encoding="utf-8")

    with pytest.raises(ConfigurationError, match="OpenAI credentials are unavailable"):
        load_local_openai_secrets(secret_file)


def test_production_secrets_are_loaded_from_private_mounted_files(tmp_path: Path) -> None:
    secrets_directory = tmp_path / "movie-concierge"
    secrets_directory.mkdir()
    openai_file = secrets_directory / "openai-api-key"
    mcp_file = secrets_directory / "mcp-bearer-token"
    openai_file.write_text("synthetic-openai-production-key\n", encoding="utf-8")
    mcp_file.write_text("synthetic-mcp-production-token\n", encoding="utf-8")
    openai_file.chmod(0o440)
    mcp_file.chmod(0o440)
    settings = Settings(
        environment=DeploymentEnvironment.PRODUCTION,
        secrets_directory=secrets_directory,
        mcp_url="http://imdb-clone-backend.imdb-clone.svc.cluster.local:8080/mcp",
        allowed_hosts=["imdb-clone.the-coding-lab.com"],
    )

    secrets = load_runtime_secrets(settings)

    assert secrets.openai_api_key.get_secret_value() == "synthetic-openai-production-key"
    assert secrets.mcp_bearer_token.get_secret_value() == "synthetic-mcp-production-token"
    assert "synthetic-openai-production-key" not in repr(secrets)
    assert "synthetic-mcp-production-token" not in repr(secrets)


def test_production_rejects_world_readable_secret_files(tmp_path: Path) -> None:
    secrets_directory = tmp_path / "movie-concierge"
    secrets_directory.mkdir()
    (secrets_directory / "openai-api-key").write_text(
        "synthetic-openai-production-key\n", encoding="utf-8"
    )
    mcp_file = secrets_directory / "mcp-bearer-token"
    mcp_file.write_text("synthetic-mcp-production-token\n", encoding="utf-8")
    mcp_file.chmod(0o440)
    settings = Settings(
        environment=DeploymentEnvironment.PRODUCTION,
        secrets_directory=secrets_directory,
        mcp_url="http://imdb-clone-backend.imdb-clone.svc.cluster.local:8080/mcp",
        allowed_hosts=["imdb-clone.the-coding-lab.com"],
    )

    with pytest.raises(
        ConfigurationError, match="production Movie Concierge credentials are unavailable"
    ):
        load_runtime_secrets(settings)


@pytest.mark.parametrize(
    ("overrides", "expected_message"),
    [
        ({"secrets_directory": None}, "mounted secrets directory"),
        ({"allowed_hosts": ["*"]}, "trusted-host allowlist"),
        ({"mcp_url": "http://localhost:8080/mcp"}, "cluster-local MCP URL"),
    ],
)
def test_production_rejects_unsafe_boundaries(
    overrides: dict[str, object], expected_message: str
) -> None:
    values: dict[str, object] = {
        "environment": DeploymentEnvironment.PRODUCTION,
        "secrets_directory": Path("/run/secrets/movie-concierge"),
        "mcp_url": "http://imdb-clone-backend.imdb-clone.svc.cluster.local:8080/mcp",
        "allowed_hosts": ["imdb-clone.the-coding-lab.com"],
    }
    values.update(overrides)

    with pytest.raises(ValueError, match=expected_message):
        Settings.model_validate(values)


def test_tracing_requires_a_valid_otlp_traces_endpoint() -> None:
    with pytest.raises(ValueError, match="must end with /v1/traces"):
        Settings(
            otel_tracing_enabled=True,
            otel_exporter_otlp_traces_endpoint="http://localhost:4318",
        )


def test_production_tracing_requires_cluster_local_alloy() -> None:
    with pytest.raises(ValueError, match="cluster-local Alloy endpoint"):
        Settings(
            environment=DeploymentEnvironment.PRODUCTION,
            secrets_directory=Path("/run/secrets/movie-concierge"),
            mcp_url="http://imdb-clone-backend.imdb-clone.svc.cluster.local:8080/mcp",
            allowed_hosts=["imdb-clone.the-coding-lab.com"],
            otel_tracing_enabled=True,
            otel_exporter_otlp_traces_endpoint="https://telemetry.example/v1/traces",
        )


def test_profiling_requires_a_valid_pyroscope_server_address() -> None:
    with pytest.raises(ValueError, match="valid HTTP Pyroscope server address"):
        Settings(
            profiling_enabled=True,
            profiling_server_address="http://localhost:4040/ingest?token=private",
        )


def test_production_profiling_requires_cluster_local_pyroscope() -> None:
    with pytest.raises(ValueError, match="cluster-local Pyroscope endpoint"):
        Settings(
            environment=DeploymentEnvironment.PRODUCTION,
            secrets_directory=Path("/run/secrets/movie-concierge"),
            mcp_url="http://imdb-clone-backend.imdb-clone.svc.cluster.local:8080/mcp",
            allowed_hosts=["imdb-clone.the-coding-lab.com"],
            profiling_enabled=True,
            profiling_server_address="https://profiles.example",
        )


def test_production_enables_private_profiling_by_default() -> None:
    settings = Settings(
        environment=DeploymentEnvironment.PRODUCTION,
        secrets_directory=Path("/run/secrets/movie-concierge"),
        mcp_url="http://imdb-clone-backend.imdb-clone.svc.cluster.local:8080/mcp",
        allowed_hosts=["imdb-clone.the-coding-lab.com"],
    )

    assert settings.profiling_active is True
    assert settings.effective_profiling_server_address == (
        "http://pyroscope.observability.svc.cluster.local:4040"
    )


def test_production_profiling_can_be_disabled_explicitly() -> None:
    settings = Settings(
        environment=DeploymentEnvironment.PRODUCTION,
        secrets_directory=Path("/run/secrets/movie-concierge"),
        mcp_url="http://imdb-clone-backend.imdb-clone.svc.cluster.local:8080/mcp",
        allowed_hosts=["imdb-clone.the-coding-lab.com"],
        profiling_enabled=False,
    )

    assert settings.profiling_active is False
    assert settings.effective_profiling_server_address is None

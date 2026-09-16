from __future__ import annotations

from pathlib import Path

import pytest
from pydantic import ValidationError

from imdb_agent import bootstrap
from imdb_agent.adapters.fakes import FakeConciergeRunner
from imdb_agent.settings import (
    ConfigurationError,
    DeploymentEnvironment,
    Settings,
    load_runtime_live_key,
    load_runtime_voice_secrets,
)


def production_settings(path: Path) -> Settings:
    return Settings(
        environment=DeploymentEnvironment.PRODUCTION,
        secrets_directory=path,
        mcp_url="http://imdb-clone-backend.imdb-clone.svc.cluster.local:8080/mcp",
        allowed_hosts=["imdb-clone.the-coding-lab.com"],
        voice_allowed_origins=["https://imdb-clone.the-coding-lab.com"],
        voice_enabled=True,
        voice_live_enabled=True,
        voice_quota_database=path / "quota.db",
        voice_session_seconds=900,
        voice_browser_seconds=1500,
        voice_shared_seconds=6000,
        profiling_enabled=False,
    )


@pytest.mark.parametrize(
    "origin",
    [
        "http://imdb-clone.the-coding-lab.com",
        "https://foreign.example",
        "*",
        "null",
        "https://imdb-clone.the-coding-lab.com/path",
        "https://user@imdb-clone.the-coding-lab.com",
    ],
)
def test_public_voice_requires_explicit_trusted_https_origin(tmp_path: Path, origin: str) -> None:
    values = production_settings(tmp_path).model_dump()
    values["voice_allowed_origins"] = [origin]
    with pytest.raises(ValidationError, match="HTTPS origins"):
        Settings.model_validate(values)


@pytest.mark.parametrize("path", [None, Path("relative.db")])
def test_public_voice_requires_persistent_quota(tmp_path: Path, path: Path | None) -> None:
    values = production_settings(tmp_path).model_dump()
    values["voice_quota_database"] = path
    with pytest.raises(ValidationError, match="persistent quota"):
        Settings.model_validate(values)


@pytest.mark.parametrize("failure", [None, "missing", "readable", "empty"])
def test_voice_secrets_use_only_private_production_mounts(
    tmp_path: Path, failure: str | None
) -> None:
    settings = production_settings(tmp_path)
    for filename in ("xai-api-key", "openai-live-api-key"):
        file = tmp_path / filename
        if failure != "missing":
            file.write_text("" if failure == "empty" else f"synthetic-{filename}")
            file.chmod(0o644 if failure == "readable" else 0o440)
    if failure:
        with pytest.raises(ConfigurationError, match="production"):
            load_runtime_voice_secrets(settings)
        with pytest.raises(ConfigurationError, match="production"):
            load_runtime_live_key(settings)
    else:
        assert load_runtime_voice_secrets(settings).xai_api_key.get_secret_value() == (
            "synthetic-xai-api-key"
        )
        assert load_runtime_live_key(settings).get_secret_value() == "synthetic-openai-live-api-key"


def test_all_mcp_adapters_receive_mounted_identity(
    tmp_path: Path, monkeypatch: pytest.MonkeyPatch
) -> None:
    settings = production_settings(tmp_path)
    for filename in ("openai-api-key", "mcp-bearer-token", "xai-api-key", "openai-live-api-key"):
        file = tmp_path / filename
        file.write_text(f"synthetic-production-{filename}")
        file.chmod(0o440)

    identities: list[str] = []
    original_grok = bootstrap.RealtimeVoiceRunner
    original_backend = bootstrap.PydanticAIConciergeRunner
    original_verifier = bootstrap.McpDelegationVerifier

    def remember(settings: Settings) -> None:
        identities.append(settings.mcp_bearer_token.get_secret_value())

    from imdb_agent.settings import LocalVoiceSecrets, RuntimeSecrets

    def grok(*, settings: Settings, secrets: LocalVoiceSecrets) -> bootstrap.RealtimeVoiceRunner:
        remember(settings)
        return original_grok(settings=settings, secrets=secrets)

    def backend(
        *, settings: Settings, secrets: RuntimeSecrets
    ) -> bootstrap.PydanticAIConciergeRunner:
        remember(settings)
        assert secrets.mcp_bearer_token == settings.mcp_bearer_token
        return original_backend(settings=settings, secrets=secrets)

    def verifier(settings: Settings) -> bootstrap.McpDelegationVerifier:
        remember(settings)
        return original_verifier(settings)

    monkeypatch.setattr(bootstrap, "RealtimeVoiceRunner", grok)
    monkeypatch.setattr(bootstrap, "PydanticAIConciergeRunner", backend)
    monkeypatch.setattr(bootstrap, "McpDelegationVerifier", verifier)
    bootstrap.create_app(settings, runner=FakeConciergeRunner())
    assert identities == ["synthetic-production-mcp-bearer-token"] * 4
    assert (tmp_path / "quota.db").is_file()

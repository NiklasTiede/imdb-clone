from __future__ import annotations

from decimal import Decimal
from enum import StrEnum
from pathlib import Path
from typing import Protocol, cast

from dotenv import dotenv_values
from pydantic import BaseModel, ConfigDict, Field, SecretStr, ValidationError
from pydantic_settings import BaseSettings, SettingsConfigDict, SettingsError

from imdb_agent import __version__

LOCAL_OPENAI_SECRETS_FILE = (
    Path(__file__).resolve().parents[3] / ".secrets" / "movie-concierge.local.env"
)


class DeploymentEnvironment(StrEnum):
    LOCAL = "local"
    TEST = "test"
    PRODUCTION = "production"


class ModelBackend(StrEnum):
    OPENAI = "openai"
    FAKE = "fake"


class ConfigurationError(RuntimeError):
    """Safe process configuration failure."""


class SettingsFactory(Protocol):
    def __call__(self, *, _env_file: Path | None) -> Settings: ...


class Settings(BaseSettings):
    """Environment configuration validated at the process Seam."""

    model_config = SettingsConfigDict(
        case_sensitive=False,
        env_file=None,
        env_file_encoding="utf-8",
        env_ignore_empty=True,
        env_prefix="IMDB_AGENT_",
        extra="forbid",
        strict=True,
    )

    service_name: str = "imdb-movie-concierge"
    environment: DeploymentEnvironment = DeploymentEnvironment.LOCAL
    version: str = __version__
    host: str = "0.0.0.0"
    port: int = Field(default=8090, ge=1, le=65535)
    model_backend: ModelBackend = ModelBackend.OPENAI
    model_name: str = Field(default="gpt-5.6-luna", min_length=1, max_length=100)
    mcp_url: str = "http://localhost:8080/mcp"
    mcp_bearer_token: SecretStr = SecretStr("local-development-mcp-token")
    mcp_init_timeout_seconds: float = Field(default=5.0, gt=0, le=30)
    mcp_read_timeout_seconds: float = Field(default=12.0, gt=0, le=60)
    run_timeout_seconds: float = Field(default=30.0, gt=0, le=120)
    provider_timeout_seconds: float = Field(default=20.0, gt=0, le=60)
    max_model_requests: int = Field(default=4, ge=1, le=8)
    max_tool_calls: int = Field(default=6, ge=1, le=12)
    max_input_tokens: int = Field(default=12_000, ge=1_000, le=50_000)
    max_output_tokens: int = Field(default=1_500, ge=100, le=4_000)
    project_cost_limit_usd: Decimal = Field(default=Decimal("20.00"), gt=0, le=20)
    run_cost_limit_usd: Decimal = Field(default=Decimal("0.25"), gt=0, le=1)
    live_evals_enabled: bool = False

    @property
    def json_logs(self) -> bool:
        return self.environment is not DeploymentEnvironment.LOCAL


def load_settings(env_file: Path | None = None) -> Settings:
    """Load settings without implicitly trusting a working-directory dotenv file."""

    settings_factory = cast("SettingsFactory", Settings)
    try:
        return settings_factory(_env_file=env_file)
    except SettingsError, ValidationError:
        raise ConfigurationError("invalid Movie Concierge configuration") from None


class OpenAISecrets(BaseModel):
    """Credential shape loaded only from the dedicated ignored local file."""

    model_config = ConfigDict(extra="forbid", frozen=True, strict=True)

    openai_api_key: SecretStr = Field(alias="OPENAI_API_KEY", min_length=1)


def load_openai_secrets(path: Path = LOCAL_OPENAI_SECRETS_FILE) -> OpenAISecrets:
    try:
        values = dict(dotenv_values(path))
        return OpenAISecrets.model_validate(values)
    except OSError, ValidationError:
        raise ConfigurationError(
            "OpenAI credentials are unavailable in .secrets/movie-concierge.local.env"
        ) from None

from __future__ import annotations

from enum import StrEnum
from typing import TYPE_CHECKING, Protocol, cast

from pydantic import Field, SecretStr, ValidationError
from pydantic_settings import BaseSettings, SettingsConfigDict, SettingsError

from imdb_agent import __version__

if TYPE_CHECKING:
    from pathlib import Path


class DeploymentEnvironment(StrEnum):
    LOCAL = "local"
    TEST = "test"
    PRODUCTION = "production"


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
    mcp_bearer_token: SecretStr | None = None

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

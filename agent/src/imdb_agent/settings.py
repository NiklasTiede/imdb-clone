from __future__ import annotations

from decimal import Decimal
from enum import StrEnum
from pathlib import Path
from typing import Protocol, cast
from urllib.parse import urlsplit

from dotenv import dotenv_values
from pydantic import BaseModel, ConfigDict, Field, SecretStr, ValidationError, model_validator
from pydantic_settings import BaseSettings, SettingsConfigDict, SettingsError

from imdb_agent import __version__

LOCAL_OPENAI_SECRETS_FILE = (
    Path(__file__).resolve().parents[3] / ".secrets" / "movie-concierge.local.env"
)
OPENAI_API_KEY_SECRET_NAME = "openai-api-key"  # noqa: S105 - mounted filename, not a key
MCP_BEARER_TOKEN_SECRET_NAME = "mcp-bearer-token"  # noqa: S105 - mounted filename
PRODUCTION_PYROSCOPE_SERVER_ADDRESS = "http://pyroscope.observability.svc.cluster.local:4040"


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
    secrets_directory: Path | None = None
    allowed_hosts: list[str] = Field(
        default_factory=lambda: ["localhost", "127.0.0.1", "testserver"]
    )
    mcp_init_timeout_seconds: float = Field(default=5.0, gt=0, le=30)
    mcp_read_timeout_seconds: float = Field(default=12.0, gt=0, le=60)
    run_timeout_seconds: float = Field(default=30.0, gt=0, le=120)
    provider_timeout_seconds: float = Field(default=20.0, gt=0, le=60)
    max_concurrent_runs: int = Field(default=2, ge=1, le=8)
    max_conversations: int = Field(default=500, ge=10, le=10_000)
    max_request_body_bytes: int = Field(default=4_096, ge=1_024, le=65_536)
    max_model_requests: int = Field(default=4, ge=1, le=8)
    max_tool_calls: int = Field(default=6, ge=1, le=12)
    max_input_tokens: int = Field(default=12_000, ge=1_000, le=50_000)
    max_output_tokens: int = Field(default=1_500, ge=100, le=4_000)
    project_cost_limit_usd: Decimal = Field(default=Decimal("20.00"), gt=0, le=20)
    run_cost_limit_usd: Decimal = Field(default=Decimal("0.25"), gt=0, le=1)
    live_evals_enabled: bool = False
    otel_tracing_enabled: bool = False
    otel_exporter_otlp_traces_endpoint: str | None = None
    otel_export_timeout_seconds: float = Field(default=5.0, gt=0, le=30)
    otel_trace_sample_ratio: float = Field(default=1.0, ge=0, le=1)
    profiling_enabled: bool | None = None
    profiling_server_address: str | None = None
    profiling_sample_rate: int = Field(default=50, ge=10, le=100)
    profiling_memory_enabled: bool = True
    profiling_memory_sample_size_bytes: int = Field(
        default=512 * 1_024,
        ge=128 * 1_024,
        le=8 * 1_024 * 1_024,
    )
    profiling_upload_interval_seconds: int = Field(default=15, ge=10, le=60)

    @property
    def json_logs(self) -> bool:
        return self.environment is not DeploymentEnvironment.LOCAL

    @property
    def profiling_active(self) -> bool:
        if self.profiling_enabled is not None:
            return self.profiling_enabled
        return self.environment is DeploymentEnvironment.PRODUCTION

    @property
    def effective_profiling_server_address(self) -> str | None:
        if not self.profiling_active:
            return None
        if self.profiling_server_address is not None:
            return self.profiling_server_address
        if self.environment is DeploymentEnvironment.PRODUCTION:
            return PRODUCTION_PYROSCOPE_SERVER_ADDRESS
        return None

    @model_validator(mode="after")
    def validate_production_boundaries(self) -> Settings:
        self._validate_otel_endpoint()
        self._validate_profiling_endpoint()
        if self.environment is not DeploymentEnvironment.PRODUCTION:
            return self
        if self.model_backend is not ModelBackend.OPENAI:
            raise ValueError("production requires the OpenAI model backend")
        if self.secrets_directory is None or not self.secrets_directory.is_absolute():
            raise ValueError("production requires an absolute mounted secrets directory")
        if not self.allowed_hosts or "*" in self.allowed_hosts:
            raise ValueError("production requires an explicit trusted-host allowlist")
        if "localhost" in self.mcp_url or "127.0.0.1" in self.mcp_url:
            raise ValueError("production requires a cluster-local MCP URL")
        return self

    def _validate_otel_endpoint(self) -> None:
        endpoint = self.otel_exporter_otlp_traces_endpoint
        if not self.otel_tracing_enabled:
            return
        if endpoint is None:
            raise ValueError("OpenTelemetry tracing requires an OTLP traces endpoint")
        parsed = urlsplit(endpoint)
        if parsed.scheme not in {"http", "https"} or parsed.hostname is None:
            raise ValueError("OpenTelemetry requires a valid HTTP OTLP traces endpoint")
        if not parsed.path.endswith("/v1/traces"):
            raise ValueError("OpenTelemetry OTLP traces endpoint must end with /v1/traces")
        if self.environment is DeploymentEnvironment.PRODUCTION and parsed.hostname != (
            "alloy.observability.svc.cluster.local"
        ):
            raise ValueError("production OpenTelemetry must use the cluster-local Alloy endpoint")

    def _validate_profiling_endpoint(self) -> None:
        endpoint = self.effective_profiling_server_address
        if not self.profiling_active:
            return
        if endpoint is None:
            raise ValueError("profiling requires a Pyroscope server address")
        parsed = urlsplit(endpoint)
        if (
            parsed.scheme not in {"http", "https"}
            or parsed.hostname is None
            or parsed.username is not None
            or parsed.password is not None
            or parsed.query
            or parsed.fragment
            or parsed.path not in {"", "/"}
        ):
            raise ValueError("profiling requires a valid HTTP Pyroscope server address")
        if self.environment is DeploymentEnvironment.PRODUCTION and (
            parsed.hostname != "pyroscope.observability.svc.cluster.local" or parsed.port != 4040
        ):
            raise ValueError("production profiling must use the cluster-local Pyroscope endpoint")


def load_settings(env_file: Path | None = None) -> Settings:
    """Load settings without implicitly trusting a working-directory dotenv file."""

    settings_factory = cast("SettingsFactory", Settings)
    try:
        return settings_factory(_env_file=env_file)
    except SettingsError, ValidationError:
        raise ConfigurationError("invalid Movie Concierge configuration") from None


class RuntimeSecrets(BaseModel):
    """Credentials resolved from one environment-specific file boundary."""

    model_config = ConfigDict(extra="forbid", frozen=True, strict=True)

    openai_api_key: SecretStr = Field(min_length=12)
    mcp_bearer_token: SecretStr = Field(min_length=20)


class _LocalOpenAISecrets(BaseModel):
    model_config = ConfigDict(extra="forbid", frozen=True, strict=True)

    openai_api_key: SecretStr = Field(alias="OPENAI_API_KEY", min_length=12)


def load_runtime_secrets(settings: Settings) -> RuntimeSecrets:
    if settings.environment is DeploymentEnvironment.PRODUCTION:
        secrets_directory = settings.secrets_directory
        if secrets_directory is None:
            raise ConfigurationError("production Movie Concierge credentials are unavailable")
        try:
            return RuntimeSecrets(
                openai_api_key=SecretStr(
                    _read_mounted_secret(secrets_directory / OPENAI_API_KEY_SECRET_NAME)
                ),
                mcp_bearer_token=SecretStr(
                    _read_mounted_secret(secrets_directory / MCP_BEARER_TOKEN_SECRET_NAME)
                ),
            )
        except OSError, ValidationError:
            raise ConfigurationError(
                "production Movie Concierge credentials are unavailable"
            ) from None

    local_openai = load_local_openai_secrets()
    return RuntimeSecrets(
        openai_api_key=local_openai.openai_api_key,
        mcp_bearer_token=settings.mcp_bearer_token,
    )


def load_local_openai_secrets(
    path: Path = LOCAL_OPENAI_SECRETS_FILE,
) -> _LocalOpenAISecrets:
    try:
        values = dict(dotenv_values(path))
        return _LocalOpenAISecrets.model_validate(values)
    except OSError, ValidationError:
        raise ConfigurationError(
            "OpenAI credentials are unavailable in .secrets/movie-concierge.local.env"
        ) from None


def _read_mounted_secret(path: Path) -> str:
    mode = path.stat().st_mode
    if mode & 0o007:
        raise OSError("mounted secret must not be accessible to other users")
    return path.read_text(encoding="utf-8").strip()

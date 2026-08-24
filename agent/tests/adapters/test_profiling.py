from __future__ import annotations

from typing import TYPE_CHECKING

from imdb_agent.adapters import profiling as profiling_module
from imdb_agent.adapters.profiling import ProfilingRuntime, configure_profiling
from imdb_agent.settings import DeploymentEnvironment, Settings

if TYPE_CHECKING:
    import pytest


class FakePyroscopeClient:
    def __init__(self, *, configure_error: Exception | None = None) -> None:
        self.configure_error = configure_error
        self.configure_calls: list[dict[str, object]] = []
        self.shutdown_calls = 0

    def configure(self, **kwargs: object) -> None:
        self.configure_calls.append(dict(kwargs))
        if self.configure_error is not None:
            raise self.configure_error

    def shutdown(self) -> None:
        self.shutdown_calls += 1


def test_profiling_is_disabled_without_initializing_client(
    monkeypatch: pytest.MonkeyPatch,
) -> None:
    client = FakePyroscopeClient()
    monkeypatch.setattr(profiling_module, "_client", client)

    runtime = configure_profiling(Settings(environment=DeploymentEnvironment.TEST))
    runtime.shutdown()

    assert runtime == ProfilingRuntime()
    assert client.configure_calls == []
    assert client.shutdown_calls == 0


def test_profiling_uses_bounded_privacy_safe_settings(
    monkeypatch: pytest.MonkeyPatch,
) -> None:
    client = FakePyroscopeClient()
    monkeypatch.setattr(profiling_module, "_client", client)

    runtime = configure_profiling(
        Settings(
            environment=DeploymentEnvironment.TEST,
            service_name="movie-concierge-test",
            version="test-version",
            profiling_enabled=True,
            profiling_server_address="http://pyroscope:4040",
            profiling_sample_rate=50,
            profiling_memory_sample_size_bytes=512 * 1_024,
        )
    )
    runtime.shutdown()

    assert runtime.enabled is True
    assert client.configure_calls == [
        {
            "application_name": "movie-concierge-test",
            "server_address": "http://pyroscope:4040",
            "enable_logging": False,
            "sample_rate": 50,
            "cpu_enabled": True,
            "oncpu": True,
            "gil_only": True,
            "report_pid": False,
            "report_thread_id": False,
            "report_thread_name": False,
            "tags": {"environment": "test", "version": "test-version"},
            "upload_interval": 15,
            "mem_enabled": True,
            "mem_max_nframe": 128,
            "mem_heap_sample_size": 512 * 1_024,
            "mem_enable_mem_domain": True,
        }
    ]
    assert client.shutdown_calls == 1


def test_profiling_initialization_failure_is_fail_open(
    monkeypatch: pytest.MonkeyPatch,
) -> None:
    client = FakePyroscopeClient(configure_error=RuntimeError("unavailable"))
    monkeypatch.setattr(profiling_module, "_client", client)

    runtime = configure_profiling(
        Settings(
            environment=DeploymentEnvironment.TEST,
            profiling_enabled=True,
            profiling_server_address="http://pyroscope:4040",
        )
    )

    assert runtime.enabled is False
    assert client.shutdown_calls == 1

from __future__ import annotations

from contextlib import suppress
from dataclasses import dataclass
from typing import TYPE_CHECKING, Protocol, cast

import pyroscope  # pyright: ignore[reportMissingTypeStubs]
import structlog

if TYPE_CHECKING:
    from imdb_agent.settings import Settings


class _PyroscopeClient(Protocol):
    def configure(
        self,
        *,
        application_name: str,
        server_address: str,
        enable_logging: bool,
        sample_rate: int,
        cpu_enabled: bool,
        oncpu: bool,
        gil_only: bool,
        report_pid: bool,
        report_thread_id: bool,
        report_thread_name: bool,
        tags: dict[str, str],
        upload_interval: int,
        mem_enabled: bool,
        mem_max_nframe: int,
        mem_heap_sample_size: int,
        mem_enable_mem_domain: bool,
    ) -> object: ...

    def shutdown(self) -> object: ...


_client = cast("_PyroscopeClient", pyroscope)


@dataclass(frozen=True, slots=True)
class ProfilingRuntime:
    """Process-owned continuous-profiler lifecycle kept outside the product Module."""

    enabled: bool = False

    def shutdown(self) -> None:
        if not self.enabled:
            return
        try:
            _client.shutdown()
        except Exception:  # Profiling must not break graceful application shutdown.
            structlog.get_logger().warning(
                "profiling_shutdown_failed",
                error_code="pyroscope_shutdown",
            )


def configure_profiling(settings: Settings) -> ProfilingRuntime:
    """Start bounded CPU and allocation sampling without collecting request content."""

    if not settings.profiling_active:
        return ProfilingRuntime()

    server_address = settings.effective_profiling_server_address
    if server_address is None:
        return ProfilingRuntime()

    try:
        _client.configure(
            application_name=settings.service_name,
            server_address=server_address,
            enable_logging=False,
            sample_rate=settings.profiling_sample_rate,
            cpu_enabled=True,
            oncpu=True,
            gil_only=True,
            report_pid=False,
            report_thread_id=False,
            report_thread_name=False,
            tags={
                "environment": settings.environment.value,
                "version": settings.version,
            },
            upload_interval=settings.profiling_upload_interval_seconds,
            mem_enabled=settings.profiling_memory_enabled,
            mem_max_nframe=128,
            mem_heap_sample_size=settings.profiling_memory_sample_size_bytes,
            mem_enable_mem_domain=True,
        )
        structlog.get_logger().info(
            "profiling_initialized",
            environment=settings.environment.value,
            service=settings.service_name,
            version=settings.version,
        )
        return ProfilingRuntime(enabled=True)
    except Exception:  # Profiler setup is deliberately fail-open for user requests.
        with suppress(Exception):
            _client.shutdown()
        structlog.get_logger().warning(
            "profiling_initialization_failed",
            error_code="pyroscope_setup",
        )
        return ProfilingRuntime()

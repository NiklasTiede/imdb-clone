from enum import StrEnum

from fastapi import APIRouter
from pydantic import BaseModel, ConfigDict


class HealthStatus(StrEnum):
    OK = "ok"
    READY = "ready"


class HealthResponse(BaseModel):
    model_config = ConfigDict(extra="forbid", frozen=True, strict=True)

    service: str
    status: HealthStatus
    version: str


def create_health_router(*, service_name: str, version: str) -> APIRouter:
    router = APIRouter()

    async def health() -> HealthResponse:
        return HealthResponse(
            service=service_name,
            status=HealthStatus.OK,
            version=version,
        )

    async def readiness() -> HealthResponse:
        return HealthResponse(
            service=service_name,
            status=HealthStatus.READY,
            version=version,
        )

    router.add_api_route(
        "/healthz",
        health,
        methods=["GET"],
        response_model=HealthResponse,
    )
    router.add_api_route(
        "/readyz",
        readiness,
        methods=["GET"],
        response_model=HealthResponse,
    )
    return router

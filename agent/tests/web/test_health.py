from __future__ import annotations

from typing import TYPE_CHECKING

import structlog
from fastapi.testclient import TestClient
from pytest import fixture

from imdb_agent.bootstrap import create_app
from imdb_agent.settings import DeploymentEnvironment, ModelBackend, Settings

if TYPE_CHECKING:
    from collections.abc import Iterator

    from fastapi import FastAPI


@fixture
def app() -> FastAPI:
    settings = Settings(
        environment=DeploymentEnvironment.TEST,
        model_backend=ModelBackend.FAKE,
        version="test-version",
    )
    return create_app(settings)


@fixture
def client(app: FastAPI) -> Iterator[TestClient]:
    with TestClient(app) as test_client:
        yield test_client


def test_health_is_small_safe_and_versioned(client: TestClient) -> None:
    response = client.get("/healthz")

    assert response.status_code == 200
    assert response.json() == {
        "service": "imdb-movie-concierge",
        "status": "ok",
        "version": "test-version",
    }


def test_readiness_needs_no_external_dependency_in_foundation(client: TestClient) -> None:
    response = client.get("/readyz")

    assert response.status_code == 200
    assert response.json() == {
        "service": "imdb-movie-concierge",
        "status": "ready",
        "version": "test-version",
    }


def test_metrics_expose_identity_and_bounded_http_labels(client: TestClient) -> None:
    client.get("/healthz")

    response = client.get("/metrics")

    assert response.status_code == 200
    assert response.headers["content-type"].startswith("text/plain")
    assert "imdb_agent_build_info" in response.text
    assert 'imdb_agent_guardrail_limit{limit="max_concurrent_runs"} 2.0' in response.text
    assert "imdb_agent_process_budget_committed_usd 0.0" in response.text
    assert (
        'imdb_agent_http_requests_total{method="GET",route="/healthz",status="200"} 1.0'
        in response.text
    )


def test_direct_pod_metrics_scrape_does_not_require_public_host(client: TestClient) -> None:
    response = client.get("/metrics", headers={"Host": "10.42.0.25:8090"})

    assert response.status_code == 200
    assert "imdb_agent_build_info" in response.text


def test_request_id_is_returned_and_logged(client: TestClient) -> None:
    with structlog.testing.capture_logs() as logs:
        response = client.get("/healthz", headers={"X-Request-ID": "request-123"})

    assert response.headers["X-Request-ID"] == "request-123"
    assert any(log.get("request_id") == "request-123" for log in logs)


def test_unsafe_request_id_is_replaced(client: TestClient) -> None:
    response = client.get("/healthz", headers={"X-Request-ID": "unsafe value forged"})

    request_id = response.headers["X-Request-ID"]
    assert request_id != "unsafe value forged"
    assert len(request_id) == 32


def test_unknown_route_uses_safe_default_response(client: TestClient) -> None:
    response = client.get("/does-not-exist")

    assert response.status_code == 404
    assert response.json() == {"detail": "Not Found"}

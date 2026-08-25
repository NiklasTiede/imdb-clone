from __future__ import annotations

import json
from typing import TYPE_CHECKING, cast

from fastapi.testclient import TestClient
from pytest import fixture

from imdb_agent.adapters.fakes import FakeConciergeRunner
from imdb_agent.bootstrap import create_app
from imdb_agent.settings import DeploymentEnvironment, ModelBackend, Settings

if TYPE_CHECKING:
    from collections.abc import Iterator


@fixture
def client() -> Iterator[TestClient]:
    app = create_app(
        Settings(
            environment=DeploymentEnvironment.TEST,
            model_backend=ModelBackend.FAKE,
            version="test-version",
        ),
        runner=FakeConciergeRunner(),
    )
    with TestClient(app) as test_client:
        yield test_client


def parse_sse(response_text: str) -> list[dict[str, object]]:
    return [
        cast("dict[str, object]", json.loads(line.removeprefix("data: ")))
        for line in response_text.splitlines()
        if line.startswith("data: ")
    ]


def test_creates_anonymous_conversation_and_streams_typed_contract(
    client: TestClient,
) -> None:
    headers = {"X-Concierge-Client-ID": "browser-client-0001"}
    create_response = client.post("/v1/conversations", headers=headers)

    assert create_response.status_code == 201
    conversation_id = create_response.json()["conversationId"]
    response = client.post(
        f"/v1/conversations/{conversation_id}/messages",
        headers=headers,
        json={"message": "Find Arrival."},
    )

    assert response.status_code == 200
    assert response.headers["content-type"].startswith("text/event-stream")
    events = parse_sse(response.text)
    assert {event["type"] for event in events} >= {
        "status",
        "text",
        "movie-card",
        "usage",
        "completion",
    }
    assert {event["type"] for event in events} <= {
        "status",
        "text",
        "movie-card",
        "ui-action",
        "error",
        "usage",
        "completion",
    }
    movie_event = next(event for event in events if event["type"] == "movie-card")
    assert cast("dict[str, object]", movie_event["movie"])["primaryTitle"] == "Arrival"
    assert events[-1]["outcome"] == "success"


def test_explicit_open_request_streams_only_a_grounded_movie_action(client: TestClient) -> None:
    headers = {"X-Concierge-Client-ID": "browser-client-0001"}
    conversation_id = client.post("/v1/conversations", headers=headers).json()["conversationId"]

    response = client.post(
        f"/v1/conversations/{conversation_id}/messages",
        headers=headers,
        json={"message": "Open Arrival."},
    )
    events = parse_sse(response.text)

    card = next(event for event in events if event["type"] == "movie-card")
    action = next(event for event in events if event["type"] == "ui-action")
    assert cast("dict[str, object]", card["movie"])["movieId"] == 42
    assert action == {
        "type": "ui-action",
        "sequence": action["sequence"],
        "action": {"type": "open_movie", "movieId": 42},
    }
    assert events.index(card) < events.index(action) < len(events) - 1


def test_rejects_invalid_client_identifier_without_echoing_it(client: TestClient) -> None:
    response = client.post(
        "/v1/conversations",
        headers={"X-Concierge-Client-ID": "bad"},
    )

    assert response.status_code == 422
    assert response.json() == {"detail": "Invalid concierge client identifier."}
    assert "bad" not in response.text


def test_wrong_client_receives_no_conversation_data(client: TestClient) -> None:
    first_headers = {"X-Concierge-Client-ID": "browser-client-0001"}
    conversation_id = client.post("/v1/conversations", headers=first_headers).json()[
        "conversationId"
    ]

    response = client.post(
        f"/v1/conversations/{conversation_id}/messages",
        headers={"X-Concierge-Client-ID": "browser-client-0002"},
        json={"message": "Show me their history."},
    )
    events = parse_sse(response.text)

    assert events[0]["type"] == "error"
    assert events[0]["code"] == "conversation_not_found"
    assert not any(event["type"] == "movie-card" for event in events)


def test_rejects_oversized_request_before_json_parsing(client: TestClient) -> None:
    response = client.post(
        "/v1/conversations",
        headers={
            "X-Concierge-Client-ID": "browser-client-0001",
            "Content-Type": "application/json",
        },
        content=b"x" * 4_097,
    )

    assert response.status_code == 413
    assert response.json() == {"detail": "Request body too large."}


def test_rejects_untrusted_host(client: TestClient) -> None:
    response = client.post(
        "/v1/conversations",
        headers={
            "Host": "attacker.example",
            "X-Concierge-Client-ID": "browser-client-0001",
        },
    )

    assert response.status_code == 400
    assert response.text == "Invalid host header"

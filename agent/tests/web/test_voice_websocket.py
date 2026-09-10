from __future__ import annotations

import asyncio
from typing import TYPE_CHECKING

import pytest
from fastapi import FastAPI
from fastapi.testclient import TestClient
from starlette.websockets import WebSocketDisconnect

from imdb_agent.concierge.voice import VoiceCommand, VoiceEvent
from imdb_agent.web.voice import create_voice_router

if TYPE_CHECKING:
    from pydantic import SecretStr

    from imdb_agent.concierge.voice import VoiceTransport


class EchoVoice:
    closed = False

    def __init__(self) -> None:
        self.contexts: list[VoiceCommand] = []

    async def run(self, transport: VoiceTransport, delegation: SecretStr | None = None) -> None:
        try:
            await transport.send(VoiceEvent(type="ready"))
            while True:
                data = await transport.receive()
                if isinstance(data, VoiceCommand):
                    if data.type == "context":
                        self.contexts.append(data)
                    if data.type == "end":
                        return
                else:
                    await transport.send(data)
        finally:
            self.closed = True


def client_for(runner: EchoVoice | None, seconds: float = 10, max_sessions: int = 20) -> TestClient:
    app = FastAPI()
    app.include_router(
        create_voice_router(
            runner,
            allowed_origins=("http://localhost:3000",),
            session_seconds=seconds,
            max_sessions=max_sessions,
        )
    )
    return TestClient(app)


def test_audio_end_and_disconnect_cleanup() -> None:
    runner = EchoVoice()
    with (
        client_for(runner) as client,
        client.websocket_connect("/v1/voice", headers={"origin": "http://localhost:3000"}) as ws,
    ):
        ws.send_json({"type": "start"})
        assert ws.receive_json()["type"] == "ready"
        ws.send_bytes(b"\x00\x01" * 2400)
        assert ws.receive_bytes() == b"\x00\x01" * 2400
        ws.send_json({"type": "end"})
        with pytest.raises(WebSocketDisconnect):
            ws.receive_json()
    assert runner.closed


@pytest.mark.parametrize("origin", ["http://evil.example", "null", ""])
def test_origin_rejected_before_provider_connection(origin: str) -> None:
    runner = EchoVoice()
    with (
        client_for(runner) as client,
        pytest.raises(WebSocketDisconnect),
        client.websocket_connect("/v1/voice", headers={"origin": origin}),
    ):
        pass
    assert not runner.closed


@pytest.mark.parametrize(
    "payload", [b"\x00", b"\x00" * 9602, '{"type":"execute","url":"https://evil.example"}']
)
def test_invalid_protocol_closes_session(payload: bytes | str) -> None:
    runner = EchoVoice()
    with (
        client_for(runner) as client,
        client.websocket_connect("/v1/voice", headers={"origin": "http://localhost:3000"}) as ws,
    ):
        ws.send_json({"type": "start"})
        ws.receive_json()
        if isinstance(payload, bytes):
            ws.send_bytes(payload)
        else:
            ws.send_text(payload)
        assert ws.receive_json()["type"] == "error"
        with pytest.raises(WebSocketDisconnect):
            ws.receive_json()
    assert runner.closed


def test_disabled_voice_and_process_session_limit() -> None:
    with (
        client_for(None) as client,
        client.websocket_connect("/v1/voice", headers={"origin": "http://localhost:3000"}) as ws,
    ):
        ws.send_json({"type": "start"})
        assert ws.receive_json()["type"] == "error"
    with client_for(EchoVoice(), max_sessions=1) as client:
        for expected in ("ready", "error"):
            with client.websocket_connect(
                "/v1/voice", headers={"origin": "http://localhost:3000"}
            ) as ws:
                ws.send_json({"type": "start"})
                assert ws.receive_json()["type"] == expected
                if expected == "ready":
                    ws.send_json({"type": "end"})
                    with pytest.raises(WebSocketDisconnect):
                        ws.receive_json()


def test_deadline_cancels_runner() -> None:
    runner = EchoVoice()
    with (
        client_for(runner, seconds=0.05) as client,
        client.websocket_connect("/v1/voice", headers={"origin": "http://localhost:3000"}) as ws,
    ):
        ws.send_json({"type": "start"})
        assert ws.receive_json()["type"] == "ready"
        assert ws.receive_json()["type"] == "error"
    assert runner.closed


def test_end_cancels_pending_provider_handshake() -> None:
    class ConnectingVoice(EchoVoice):
        async def run(self, transport: VoiceTransport, delegation: SecretStr | None = None) -> None:
            try:
                await transport.send(VoiceEvent(type="status", status="thinking"))
                await asyncio.Event().wait()
            finally:
                self.closed = True

    runner = ConnectingVoice()
    with (
        client_for(runner) as client,
        client.websocket_connect("/v1/voice", headers={"origin": "http://localhost:3000"}) as ws,
    ):
        ws.send_json({"type": "start"})
        ws.receive_json()
        ws.send_json({"type": "end"})
        with pytest.raises(WebSocketDisconnect):
            ws.receive_json()
    assert runner.closed


def test_usage_limit_is_explained_without_exposing_provider_errors() -> None:
    from imdb_agent.concierge.voice import VoiceSessionLimitError

    class LimitedVoice(EchoVoice):
        async def run(self, transport: VoiceTransport, delegation: SecretStr | None = None) -> None:
            await transport.send(VoiceEvent(type="ready"))
            raise VoiceSessionLimitError("synthetic provider payload must stay private")

    with (
        client_for(LimitedVoice()) as client,
        client.websocket_connect("/v1/voice", headers={"origin": "http://localhost:3000"}) as ws,
    ):
        ws.send_json({"type": "start"})
        assert ws.receive_json()["type"] == "ready"
        event = ws.receive_json()
        assert event["type"] == "error"
        assert "usage limit" in event["text"]
        assert "synthetic" not in event["text"]
        with pytest.raises(WebSocketDisconnect):
            ws.receive_json()


def test_personal_voice_cannot_start_without_verified_delegation() -> None:
    runner = EchoVoice()
    with (
        client_for(runner) as client,
        client.websocket_connect("/v1/voice", headers={"origin": "http://localhost:3000"}) as ws,
    ):
        ws.send_json({"type": "start", "delegation": "synthetic-invalid"})
        event = ws.receive_json()
        assert event["type"] == "error"
        assert "Sign in again" in event["text"]
    assert not runner.closed


def test_context_updates_are_validated_and_delivered_in_order_without_ending_voice() -> None:
    runner = EchoVoice()
    with (
        client_for(runner) as client,
        client.websocket_connect("/v1/voice", headers={"origin": "http://localhost:3000"}) as ws,
    ):
        ws.send_json({"type": "start"})
        assert ws.receive_json()["type"] == "ready"
        for movie_id in (6, 7):
            ws.send_json({"type": "context", "context": {"page": "movie", "movieId": movie_id}})
        ws.send_bytes(b"\x00\x01" * 2400)
        assert ws.receive_bytes() == b"\x00\x01" * 2400
        assert [command.context.movie_id for command in runner.contexts if command.context] == [
            6,
            7,
        ]
        ws.send_json({"type": "end"})

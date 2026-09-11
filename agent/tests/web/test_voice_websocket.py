from __future__ import annotations

import asyncio
import json
from typing import TYPE_CHECKING

import pytest
from fastapi import FastAPI
from fastapi.testclient import TestClient
from starlette.websockets import WebSocketDisconnect

from imdb_agent.adapters.logging import configure_logging
from imdb_agent.concierge.voice import VoiceCommand, VoiceEvent
from imdb_agent.web.voice import create_voice_router

if TYPE_CHECKING:
    from pydantic import SecretStr

    from imdb_agent.concierge.voice import VoiceTransport


class EchoVoice:
    closed = False

    def __init__(self) -> None:
        self.contexts: list[VoiceCommand] = []
        self.texts: list[str] = []

    async def run(self, transport: VoiceTransport, delegation: SecretStr | None = None) -> None:
        try:
            await transport.send(VoiceEvent(type="ready"))
            while True:
                data = await transport.receive()
                if isinstance(data, VoiceCommand):
                    if data.type == "context":
                        self.contexts.append(data)
                    if data.type == "text" and data.text is not None:
                        self.texts.append(data.text)
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


def test_disabled_voice_and_process_session_limit(capsys: pytest.CaptureFixture[str]) -> None:
    configure_logging(json_output=True)
    with (
        client_for(None) as client,
        client.websocket_connect("/v1/voice", headers={"origin": "http://localhost:3000"}) as ws,
    ):
        ws.send_json({"type": "start"})
        event = ws.receive_json()
        assert event["type"] == "error"
        assert "make run-agent-voice" in event["text"]
    logs = capsys.readouterr().out
    assert '"outcome": "disabled"' in logs
    assert '"error_code": "voice_disabled"' in logs
    with client_for(EchoVoice(), max_sessions=1) as client:
        for expected in ("ready", "error"):
            with client.websocket_connect(
                "/v1/voice", headers={"origin": "http://localhost:3000"}
            ) as ws:
                ws.send_json({"type": "start"})
                event = ws.receive_json()
                assert event["type"] == expected
                if expected == "error":
                    assert "session limit" in event["text"]
                if expected == "ready":
                    ws.send_json({"type": "end"})
                    with pytest.raises(WebSocketDisconnect):
                        ws.receive_json()
    logs = capsys.readouterr().out
    assert '"outcome": "enabled"' in logs
    assert '"error_code": "voice_process_session_limit"' in logs


@pytest.mark.parametrize("seconds", [0.05, 300.0])
def test_deadline_cancels_runner(
    seconds: float, monkeypatch: pytest.MonkeyPatch, capsys: pytest.CaptureFixture[str]
) -> None:
    configure_logging(json_output=True)
    real_timeout = asyncio.timeout

    def short_session_timeout(delay: float | None) -> asyncio.Timeout:
        return real_timeout(0.05 if delay == 300 else delay)

    monkeypatch.setattr(asyncio, "timeout", short_session_timeout)
    runner = EchoVoice()
    with (
        client_for(runner, seconds=seconds) as client,
        client.websocket_connect("/v1/voice", headers={"origin": "http://localhost:3000"}) as ws,
    ):
        ws.send_json({"type": "start"})
        assert ws.receive_json()["type"] == "ready"
        event = ws.receive_json()
        assert event["type"] == "error"
        assert ("5-minute time limit" if seconds == 300 else "0.05-second time limit") in event[
            "text"
        ]
    assert runner.closed
    events = [json.loads(line) for line in capsys.readouterr().out.splitlines()]
    ended = next(event for event in events if event["event"] == "voice_session_ended")
    assert ended["outcome"] == "time_limit"
    assert ended["duration_ms"] >= 50
    assert ended["session_limit_seconds"] == seconds
    assert ended["phase"] == "active"


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
        ws.send_json({"type": "text", "text": "  Tell me about Forrest Gump  "})
        ws.send_bytes(b"\x00\x01" * 2400)
        assert ws.receive_bytes() == b"\x00\x01" * 2400
        assert runner.texts == ["Tell me about Forrest Gump"]
        assert [command.context.movie_id for command in runner.contexts if command.context] == [
            6,
            7,
        ]
        ws.send_json({"type": "end"})


@pytest.mark.parametrize("ready", [False, True])
def test_provider_timeout_is_not_reported_as_session_expiry(
    ready: bool, capsys: pytest.CaptureFixture[str]
) -> None:
    configure_logging(json_output=True)

    class TimedOutVoice(EchoVoice):
        async def run(self, transport: VoiceTransport, delegation: SecretStr | None = None) -> None:
            if ready:
                await transport.send(VoiceEvent(type="ready"))
            raise TimeoutError("synthetic-private-provider-message")

    with (
        client_for(TimedOutVoice()) as client,
        client.websocket_connect("/v1/voice", headers={"origin": "http://localhost:3000"}) as ws,
    ):
        ws.send_json({"type": "start"})
        if ready:
            assert ws.receive_json()["type"] == "ready"
        event = ws.receive_json()
        assert event["text"] == "Voice connection timed out. Please reconnect."
    logs = capsys.readouterr().out
    assert "synthetic-private" not in logs
    events = [json.loads(line) for line in logs.splitlines()]
    ended = next(event for event in events if event["event"] == "voice_session_ended")
    assert ended["outcome"] == "connection_timeout"
    assert ended["phase"] == ("active" if ready else "provider_connect")
    assert ended["duration_ms"] < 10_000


@pytest.mark.parametrize("delivery_error", [None, TimeoutError, WebSocketDisconnect])
def test_idle_expiry_is_reported_and_logged_separately(
    delivery_error: type[Exception] | None,
    monkeypatch: pytest.MonkeyPatch,
    capsys: pytest.CaptureFixture[str],
) -> None:
    from imdb_agent.concierge.voice import VoiceIdleTimeoutError
    from imdb_agent.web.voice import BrowserVoiceTransport

    original_send = BrowserVoiceTransport.send

    async def send(transport: BrowserVoiceTransport, event: VoiceEvent | bytes) -> None:
        if isinstance(event, VoiceEvent) and event.type == "standby" and delivery_error:
            raise delivery_error()
        await original_send(transport, event)

    monkeypatch.setattr(BrowserVoiceTransport, "send", send)
    configure_logging(json_output=True)

    class IdleVoice(EchoVoice):
        async def run(self, transport: VoiceTransport, delegation: SecretStr | None = None) -> None:
            await transport.send(VoiceEvent(type="ready"))
            raise VoiceIdleTimeoutError

    with (
        client_for(IdleVoice()) as client,
        client.websocket_connect("/v1/voice", headers={"origin": "http://localhost:3000"}) as ws,
    ):
        ws.send_json({"type": "start"})
        assert ws.receive_json()["type"] == "ready"
        if delivery_error is None:
            assert ws.receive_json()["type"] == "standby"
        with pytest.raises(WebSocketDisconnect):
            ws.receive_json()
    events = [json.loads(line) for line in capsys.readouterr().out.splitlines()]
    ended = next(event for event in events if event["event"] == "voice_session_ended")
    assert ended["outcome"] == "idle_timeout"


def test_session_logs_correlate_and_count_activity_without_payload(
    capsys: pytest.CaptureFixture[str],
) -> None:
    import structlog

    configure_logging(json_output=True)

    class LoggingVoice(EchoVoice):
        async def run(self, transport: VoiceTransport, delegation: SecretStr | None = None) -> None:
            structlog.get_logger().info("synthetic_runner_event")
            await super().run(transport, delegation)

    runner = LoggingVoice()
    with client_for(runner) as client:
        for _ in range(2):
            with client.websocket_connect(
                "/v1/voice", headers={"origin": "http://localhost:3000"}
            ) as ws:
                ws.send_json({"type": "start"})
                assert ws.receive_json()["type"] == "ready"
                ws.send_json({"type": "text", "text": "synthetic-private-user-message"})
                ws.send_bytes(b"\x00\x01" * 2400)
                assert ws.receive_bytes() == b"\x00\x01" * 2400
                ws.send_json({"type": "end"})
                with pytest.raises(WebSocketDisconnect):
                    ws.receive_json()
    logs = capsys.readouterr().out
    assert "synthetic-private-user-message" not in logs
    events = [json.loads(line) for line in logs.splitlines()]
    starts = [event for event in events if event["event"] == "voice_session_started"]
    assert len(starts) == 2
    assert starts[0]["request_id"] != starts[1]["request_id"]
    for start in starts:
        matching = [event for event in events if event.get("request_id") == start["request_id"]]
        assert {event["event"] for event in matching} == {
            "voice_session_started",
            "synthetic_runner_event",
            "voice_session_ready",
            "voice_session_ended",
        }
        ended = next(event for event in matching if event["event"] == "voice_session_ended")
        assert ended["outcome"] == "user_end"
        assert ended["typed_messages"] == 1
        assert ended["control_messages"] == 2
        assert ended["audio_input_bytes"] == ended["audio_output_bytes"] == 4800
        assert ended["turns"] == 0

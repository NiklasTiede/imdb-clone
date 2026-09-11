"""Bounded browser WebSocket adapter; no provider frames or credentials cross it."""

from __future__ import annotations

import asyncio
from contextlib import suppress
from time import monotonic
from typing import TYPE_CHECKING, Literal
from uuid import uuid4

import structlog
from fastapi import APIRouter, WebSocket, WebSocketDisconnect
from pydantic import BaseModel, ConfigDict, SecretStr, ValidationError
from starlette.websockets import WebSocketState

from imdb_agent.concierge.personal import DelegationRejectedError, DelegationVerifier
from imdb_agent.concierge.voice import (
    PCM_BYTES_PER_SECOND,
    VoiceCommand,
    VoiceDisconnectedError,
    VoiceEvent,
    VoiceIdleTimeoutError,
    VoiceSessionLimitError,
)

if TYPE_CHECKING:
    from imdb_agent.concierge.voice import VoiceRunner


class VoiceStart(BaseModel):
    model_config = ConfigDict(extra="forbid", strict=True)
    type: Literal["start"]
    delegation: SecretStr | None = None


class BrowserVoiceTransport:
    def __init__(self, socket: WebSocket, *, seconds: float) -> None:
        self._socket = socket
        self._input_bytes = 0
        self._output_bytes = 0
        self._max_bytes = int(seconds * PCM_BYTES_PER_SECOND)
        self._commands = 0
        self._typed_messages = 0
        self._turns = 0
        self._started_at = monotonic()
        self.ready = False
        self.end_requested = False
        self._messages: asyncio.Queue[bytes | VoiceCommand] = asyncio.Queue(maxsize=8)

    async def receive(self) -> bytes | VoiceCommand:
        return await self._messages.get()

    def statistics(self) -> dict[str, int]:
        return {
            "audio_input_bytes": self._input_bytes,
            "audio_output_bytes": self._output_bytes,
            "control_messages": self._commands,
            "typed_messages": self._typed_messages,
            "turns": self._turns,
        }

    async def read_browser(self) -> None:
        """Watch disconnect/end even while the provider handshake is still pending."""
        while True:
            message = await self._read_message()
            if isinstance(message, VoiceCommand) and message.type == "end":
                self.end_requested = True
                return
            try:
                self._messages.put_nowait(message)
            except asyncio.QueueFull:
                raise ValueError("voice_input_backpressure") from None

    async def _read_message(self) -> bytes | VoiceCommand:
        message = await self._socket.receive()
        if message["type"] == "websocket.disconnect":
            raise VoiceDisconnectedError
        data = message.get("bytes")
        if data is not None:
            self._input_bytes += len(data)
            if (
                not data
                or len(data) % 2
                or len(data) > 9_600
                or self._input_bytes > self._max_bytes
            ):
                raise ValueError("invalid_audio")
            return data
        text = message.get("text") or ""
        self._commands += 1
        if len(text) > 1500 or self._commands > 300:
            raise ValueError("invalid_control")
        command = VoiceCommand.model_validate_json(text)
        if command.type == "text":
            self._typed_messages += 1
        return command

    async def send(self, event: VoiceEvent | bytes) -> None:
        async with asyncio.timeout(5):
            if isinstance(event, bytes):
                self._output_bytes += len(event)
                if self._output_bytes > self._max_bytes:
                    raise ValueError("output_audio_limit")
                await self._socket.send_bytes(event)
            else:
                await self._socket.send_text(event.model_dump_json(exclude_none=True))
                self._turns = max(self._turns, event.turn)
                if event.type == "ready" and not self.ready:
                    self.ready = True
                    structlog.get_logger().info(
                        "voice_session_ready",
                        duration_ms=round((monotonic() - self._started_at) * 1000),
                    )

    async def send_error(self, message: str) -> None:
        # Reporting a failure must not hide its original reason if the socket is gone.
        with suppress(Exception):
            await self.send(VoiceEvent(type="error", text=message))


def create_voice_router(
    runner: VoiceRunner | None,
    *,
    verifier: DelegationVerifier | None = None,
    allowed_origins: tuple[str, ...],
    session_seconds: float,
    max_sessions: int,
) -> APIRouter:
    router = APIRouter()
    active = 0
    started = 0
    logger = structlog.get_logger()
    logger.info(
        "voice_availability",
        outcome="enabled" if runner is not None else "disabled",
        session_limit_seconds=session_seconds,
    )

    async def voice(socket: WebSocket) -> None:
        nonlocal active, started
        if socket.headers.get("origin") not in allowed_origins:
            logger.warning("voice_connection_rejected", error_code="voice_origin_rejected")
            await socket.close(code=1008)
            return
        await socket.accept()
        if runner is None or active >= 2 or started >= max_sessions:
            if runner is None:
                error_code = "voice_disabled"
                message = (
                    "Voice is disabled on the server. "
                    "Start the local service with make run-agent-voice."
                )
            elif active >= 2:
                error_code = "voice_concurrency_limit"
                message = "All voice sessions are busy. End another session, then try again."
            else:
                error_code = "voice_process_session_limit"
                message = (
                    "The local voice service reached its session limit. Restart it to continue."
                )
            logger.warning("voice_connection_rejected", error_code=error_code)
            await socket.send_text(
                VoiceEvent(type="error", text=message).model_dump_json(exclude_none=True)
            )
            await socket.close(code=1013)
            return
        active += 1
        started += 1
        transport = BrowserVoiceTransport(socket, seconds=session_seconds)
        started_at = monotonic()
        deadline = asyncio.timeout(session_seconds)
        phase = "start"
        outcome = "runner_completed"
        error_type: str | None = None
        tokens = structlog.contextvars.bind_contextvars(request_id=uuid4().hex)
        logger.info("voice_session_started", session_limit_seconds=session_seconds)
        try:
            async with deadline:
                async with asyncio.timeout(5):
                    initial = await socket.receive_text()
                    if len(initial) > 1200:
                        raise ValueError("invalid_start")
                    start = VoiceStart.model_validate_json(initial)
                    if start.delegation is not None:
                        phase = "delegation"
                        if verifier is None:
                            raise DelegationRejectedError
                        await verifier.verify(start.delegation)
                phase = "provider_connect"
                tasks = [
                    asyncio.create_task(transport.read_browser()),
                    asyncio.create_task(runner.run(transport, start.delegation)),
                ]
                try:
                    done, _ = await asyncio.wait(tasks, return_when=asyncio.FIRST_COMPLETED)
                    for task in done:
                        task.result()
                    if transport.end_requested:
                        outcome = "user_end"
                finally:
                    for task in tasks:
                        task.cancel()
                    await asyncio.gather(*tasks, return_exceptions=True)
        except VoiceDisconnectedError, WebSocketDisconnect:
            outcome = "client_disconnect"
        except DelegationRejectedError:
            outcome = "delegation_rejected"
            await transport.send_error("Sign in again to use your watchlist, then restart voice.")
        except VoiceSessionLimitError:
            outcome = "usage_limit"
            await transport.send_error(
                "This voice session reached its usage limit. Start a new session to continue."
            )
        except VoiceIdleTimeoutError:
            outcome = "idle_timeout"
            # The browser may leave while the final standby notification is in flight.
            with suppress(Exception):
                await transport.send(VoiceEvent(type="standby"))
        except TimeoutError:
            if deadline.expired():
                outcome = "time_limit"
                limit = (
                    f"{session_seconds / 60:g}-minute"
                    if session_seconds % 60 == 0
                    else f"{session_seconds:g}-second"
                )
                message = (
                    f"Voice session reached its {limit} time limit. "
                    "Start a new session to continue."
                )
            elif phase == "start":
                outcome = "start_timeout"
                message = "Voice start timed out. Please reconnect."
            elif phase == "delegation":
                outcome = "delegation_timeout"
                message = "Voice sign-in verification timed out. Please reconnect."
            else:
                outcome = "connection_timeout"
                message = "Voice connection timed out. Please reconnect."
            await transport.send_error(message)
        except (ValidationError, ValueError) as error:
            outcome = "invalid_message"
            error_type = type(error).__name__
            await transport.send_error("Invalid voice message. Please reconnect.")
        except asyncio.CancelledError:
            outcome = "server_shutdown"
            raise
        except Exception as error:
            # Provider messages can contain transcripts or credentials: record only the class.
            outcome = "provider_error"
            error_type = type(error).__name__
            await transport.send_error("Voice connection failed. Please reconnect.")
        finally:
            active -= 1
            logger.info(
                "voice_session_ended",
                outcome=outcome,
                error_code=None
                if outcome in {"user_end", "client_disconnect", "runner_completed"}
                else f"voice_{outcome}",
                error_type=error_type,
                phase="active" if transport.ready else phase,
                duration_ms=round((monotonic() - started_at) * 1000),
                session_limit_seconds=session_seconds,
                **transport.statistics(),
            )
            structlog.contextvars.reset_contextvars(**tokens)
            if socket.client_state is WebSocketState.CONNECTED:
                with suppress(Exception):
                    async with asyncio.timeout(5):
                        await socket.close()

    router.add_api_websocket_route("/v1/voice", voice)
    return router

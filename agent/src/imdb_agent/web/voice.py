"""Bounded browser WebSocket adapter; no provider frames or credentials cross it."""

from __future__ import annotations

import asyncio
from typing import TYPE_CHECKING, Literal

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
        self._messages: asyncio.Queue[bytes | VoiceCommand] = asyncio.Queue(maxsize=8)

    async def receive(self) -> bytes | VoiceCommand:
        return await self._messages.get()

    async def read_browser(self) -> None:
        """Watch disconnect/end even while the provider handshake is still pending."""
        while True:
            message = await self._read_message()
            if isinstance(message, VoiceCommand) and message.type == "end":
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
        return VoiceCommand.model_validate_json(text)

    async def send(self, event: VoiceEvent | bytes) -> None:
        async with asyncio.timeout(5):
            if isinstance(event, bytes):
                self._output_bytes += len(event)
                if self._output_bytes > self._max_bytes:
                    raise ValueError("output_audio_limit")
                await self._socket.send_bytes(event)
            else:
                await self._socket.send_text(event.model_dump_json(exclude_none=True))


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

    async def voice(socket: WebSocket) -> None:
        nonlocal active, started
        if socket.headers.get("origin") not in allowed_origins:
            await socket.close(code=1008)
            return
        await socket.accept()
        if runner is None or active >= 2 or started >= max_sessions:
            await socket.send_text(
                VoiceEvent(
                    type="error", text="Voice is unavailable. Please use text or try again later."
                ).model_dump_json(exclude_none=True)
            )
            await socket.close(code=1013)
            return
        active += 1
        started += 1
        transport = BrowserVoiceTransport(socket, seconds=session_seconds)
        try:
            async with asyncio.timeout(session_seconds):
                async with asyncio.timeout(5):
                    initial = await socket.receive_text()
                    if len(initial) > 1200:
                        raise ValueError("invalid_start")
                    start = VoiceStart.model_validate_json(initial)
                    if start.delegation is not None:
                        if verifier is None:
                            raise DelegationRejectedError
                        await verifier.verify(start.delegation)
                tasks = [
                    asyncio.create_task(transport.read_browser()),
                    asyncio.create_task(runner.run(transport, start.delegation)),
                ]
                try:
                    done, _ = await asyncio.wait(tasks, return_when=asyncio.FIRST_COMPLETED)
                    for task in done:
                        task.result()
                finally:
                    for task in tasks:
                        task.cancel()
                    await asyncio.gather(*tasks, return_exceptions=True)
        except VoiceDisconnectedError, WebSocketDisconnect:
            pass
        except DelegationRejectedError:
            await transport.send(
                VoiceEvent(
                    type="error", text="Sign in again to use your watchlist, then restart voice."
                )
            )
        except VoiceSessionLimitError:
            structlog.get_logger().info("voice_session_ended", error_code="voice_usage_limit")
            await transport.send(
                VoiceEvent(
                    type="error",
                    text=(
                        "This voice session reached its usage limit. "
                        "Start a new session to continue."
                    ),
                )
            )
        except TimeoutError:
            await transport.send(
                VoiceEvent(type="error", text="Voice session expired. Start a new session.")
            )
        except ValidationError, ValueError:
            await transport.send(
                VoiceEvent(type="error", text="Invalid voice message. Please reconnect.")
            )
        except Exception as error:
            # Never send or log provider exceptions: they can contain transcripts or credentials.
            structlog.get_logger().warning(
                "voice_session_failed",
                error_type=type(error).__name__,
            )
            if socket.client_state is WebSocketState.CONNECTED:
                await transport.send(
                    VoiceEvent(
                        type="error", text="Voice connection failed. Please reconnect or use text."
                    )
                )
        finally:
            active -= 1
            if socket.client_state is WebSocketState.CONNECTED:
                await socket.close()

    router.add_api_websocket_route("/v1/voice", voice)
    return router

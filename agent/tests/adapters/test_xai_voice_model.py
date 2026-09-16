import base64
import json
from unittest.mock import AsyncMock, MagicMock, patch

import pytest
from pydantic_ai.exceptions import UserError
from pydantic_ai.messages import ModelRequest
from pydantic_ai.models import ModelRequestParameters
from pydantic_ai.providers.xai import XaiProvider
from pydantic_ai.realtime import RealtimeError
from pydantic_ai.realtime.codec import AudioDelta, CancelResponse
from pydantic_ai.realtime.xai import XaiRealtimeModelSettings
from pydantic_ai.tools import ToolDefinition

from popcorn_society_agent.adapters.xai_voice_model import ConciergeXaiVoiceModel


@pytest.mark.asyncio
async def test_hosted_greeting_preserves_audio_across_both_handshake_updates() -> None:
    def audio(value: bytes) -> str:
        return json.dumps(
            {
                "type": "response.output_audio.delta",
                "event_id": "audio",
                "response_id": "welcome",
                "item_id": "greeting",
                "output_index": 0,
                "content_index": 0,
                "delta": base64.b64encode(value).decode(),
            }
        )

    socket = AsyncMock()
    socket.recv.side_effect = [
        json.dumps({"type": "session.created", "session": {}}),
        json.dumps(
            {"type": "response.created", "event_id": "response", "response": {"id": "welcome"}}
        ),
        audio(b"\x01\x00"),
        json.dumps({"type": "session.updated"}),
        audio(b"\x02\x00"),
        json.dumps({"type": "session.updated"}),
    ]
    socket.__aiter__.return_value = [audio(b"\x03\x00")]
    opening = MagicMock()
    opening.__aenter__ = AsyncMock(return_value=socket)
    opening.__aexit__ = AsyncMock(return_value=False)
    model = ConciergeXaiVoiceModel(
        "grok-voice-think-fast-2.0",
        provider=XaiProvider(api_key="test-only"),
        agent_id="agent_test",
    )
    with patch("websockets.connect", return_value=opening):
        async with model.connect(
            messages=[], model_settings=None, model_request_parameters=ModelRequestParameters()
        ) as live:
            chunks = [event.data async for event in live if isinstance(event, AudioDelta)]
            assert chunks == [b"\x01\x00", b"\x02\x00", b"\x03\x00"]
            await live.send(CancelResponse())
            assert json.loads(socket.send.call_args.args[0])["type"] == "response.cancel"


@pytest.mark.asyncio
@pytest.mark.parametrize("frames", [1025, 1])
async def test_hosted_startup_backlog_is_bounded_and_closes_socket(frames: int) -> None:
    socket = AsyncMock()
    socket.recv.side_effect = [
        json.dumps({"type": "session.created", "session": {}}),
        *[json.dumps({"type": "unknown", "data": "x" * (1_024_001 if frames == 1 else 0)})]
        * frames,
    ]
    opening = MagicMock()
    opening.__aenter__ = AsyncMock(return_value=socket)
    opening.__aexit__ = AsyncMock(return_value=False)
    model = ConciergeXaiVoiceModel(
        "grok-voice-think-fast-2.0",
        provider=XaiProvider(api_key="test-only"),
        agent_id="agent_test",
    )
    with (
        patch("websockets.connect", return_value=opening),
        pytest.raises(RealtimeError, match="backlog"),
    ):
        async with model.connect(
            messages=[], model_settings=None, model_request_parameters=ModelRequestParameters()
        ):
            pytest.fail("Oversized startup must not connect")
    opening.__aexit__.assert_awaited_once()


@pytest.mark.asyncio
@pytest.mark.parametrize("agent_id", [None, "agent_test-profile"])
@pytest.mark.parametrize("voice", [None, "zenith"])
async def test_handshake_preserves_profile_audio_and_application_tools(
    agent_id: str | None,
    voice: str | None,
) -> None:
    socket = AsyncMock()
    socket.recv.side_effect = [
        json.dumps(
            {
                "type": "session.created",
                "event_id": "created-1",
                "session": {"model": "grok-voice-think-fast-2.0", "voice": "custom-profile-voice"},
            }
        ),
        *([json.dumps({"type": "session.updated", "session": {"tools": []}})] if agent_id else []),
        json.dumps({"type": "session.updated"}),
    ]

    async def receive_before_update(_payload: str) -> None:
        assert socket.recv.await_count == (2 if agent_id else 1)

    socket.send.side_effect = receive_before_update
    connection = MagicMock()
    connection.__aenter__ = AsyncMock(return_value=socket)
    connection.__aexit__ = AsyncMock(return_value=False)
    settings = XaiRealtimeModelSettings(
        xai_turn_detection={"type": "server_vad"},
        parallel_tool_calls=False,
    )
    if voice is not None:
        settings["xai_voice"] = voice
    model = ConciergeXaiVoiceModel(
        "grok-voice-think-fast-2.0",
        provider=XaiProvider(api_key="test-only"),
        agent_id=agent_id,
        settings=settings,
    )
    with patch("websockets.connect", return_value=connection) as connect:
        async with model.connect(
            messages=[ModelRequest(parts=[], instructions="Movie Concierge application policy")],
            model_settings=None,
            model_request_parameters=ModelRequestParameters(
                function_tools=[ToolDefinition(name="search_movies", parameters_json_schema={})],
            ),
        ) as live:
            expected_query = (
                "agent_id=agent_test-profile" if agent_id else "model=grok-voice-think-fast-2.0"
            )
            assert connect.call_args.args[0] == f"wss://api.x.ai/v1/realtime?{expected_query}"
            assert live.model_name == "grok-voice-think-fast-2.0"
            assert live.interrupts_response_on_speech is True
            assert live.input_transcription_enabled is True
            update = json.loads(socket.send.call_args_list[0].args[0])
            assert update["type"] == "session.update"
            session = update["session"]
            assert session["audio"]["output"] == {
                "format": {"type": "audio/pcm", "rate": 24000},
                "speed": 1.15,
            }
            assert session["audio"]["input"]["format"] == {"type": "audio/pcm", "rate": 24000}
            assert session["instructions"] == "Movie Concierge application policy"
            if voice is not None:
                assert session["voice"] == voice
            else:
                assert "voice" not in session
            if agent_id:
                assert "model" not in session
                assert "reasoning" not in session
            assert session["turn_detection"]["type"] == "server_vad"
            assert "silence_duration_ms" not in session["turn_detection"]
            assert session["parallel_tool_calls"] is False
            assert session["tools"][0]["name"] == "search_movies"
    connection.__aexit__.assert_awaited_once()


@pytest.mark.asyncio
async def test_hosted_profile_does_not_silently_restart_conversation_on_reconnect() -> None:
    model = ConciergeXaiVoiceModel(
        "grok-voice-think-fast-2.0",
        provider=XaiProvider(api_key="test-only"),
        agent_id="agent_test",
        settings=XaiRealtimeModelSettings(reconnect={"max_attempts": 1}),
    )
    with patch("websockets.connect") as connect:
        with pytest.raises(UserError, match="new session"):
            async with model.connect(
                messages=[],
                model_settings=None,
                model_request_parameters=ModelRequestParameters(),
            ):
                pass
        connect.assert_not_called()

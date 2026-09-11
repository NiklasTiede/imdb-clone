import json
from unittest.mock import AsyncMock, MagicMock, patch

import pytest
from pydantic_ai.models import ModelRequestParameters
from pydantic_ai.providers.xai import XaiProvider
from pydantic_ai.realtime.xai import XaiRealtimeModelSettings
from pydantic_ai.tools import ToolDefinition

from imdb_agent.adapters.xai_voice_model import ConciergeXaiVoiceModel


@pytest.mark.asyncio
async def test_handshake_sends_speed_without_losing_audio_or_tool_configuration() -> None:
    socket = AsyncMock()
    socket.recv.side_effect = [
        json.dumps(
            {
                "type": "session.created",
                "event_id": "created-1",
                "session": {"model": "grok-voice-think-fast-2.0"},
            }
        ),
        json.dumps({"type": "session.updated"}),
    ]
    connection = MagicMock()
    connection.__aenter__ = AsyncMock(return_value=socket)
    connection.__aexit__ = AsyncMock(return_value=False)
    model = ConciergeXaiVoiceModel(
        "grok-voice-think-fast-2.0",
        provider=XaiProvider(api_key="test-only"),
        settings=XaiRealtimeModelSettings(
            xai_voice="eve",
            xai_turn_detection={"type": "server_vad", "silence_duration_ms": 650},
            parallel_tool_calls=False,
        ),
    )
    with patch("websockets.connect", return_value=connection):
        async with model.connect(
            messages=[],
            model_settings=None,
            model_request_parameters=ModelRequestParameters(
                function_tools=[ToolDefinition(name="search_movies", parameters_json_schema={})],
            ),
        ):
            update = json.loads(socket.send.call_args_list[0].args[0])
            assert update["type"] == "session.update"
            session = update["session"]
            assert session["audio"]["output"] == {
                "format": {"type": "audio/pcm", "rate": 24000},
                "speed": 1.15,
            }
            assert session["audio"]["input"]["format"] == {"type": "audio/pcm", "rate": 24000}
            assert session["voice"] == "eve"
            assert session["turn_detection"]["silence_duration_ms"] == 650
            assert session["parallel_tool_calls"] is False
            assert session["tools"][0]["name"] == "search_movies"

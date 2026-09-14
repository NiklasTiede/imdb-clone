"""xAI hosted profiles and output speed missing from Pydantic AI 2.42's surface."""

from contextlib import asynccontextmanager
from typing import TYPE_CHECKING, Any, cast
from urllib.parse import urlencode

from pydantic import BaseModel, ConfigDict
from pydantic_ai._instrumentation import get_instructions
from pydantic_ai.exceptions import UserError
from pydantic_ai.realtime._openai_protocol import (
    config_interrupts_response_on_speech,
    connect_openai_protocol,
    expect_event,
)
from pydantic_ai.realtime.xai import XaiRealtimeConnection, XaiRealtimeModel

if TYPE_CHECKING:
    from collections.abc import AsyncGenerator, Awaitable, Callable, Sequence

    from pydantic_ai.messages import ModelMessage
    from pydantic_ai.models import ModelRequestParameters
    from pydantic_ai.providers.xai import XaiProvider
    from pydantic_ai.realtime import RealtimeModelSettings
    from pydantic_ai.realtime.xai import XaiRealtimeModelSettings
    from pydantic_ai.tools import ToolDefinition
    from websockets.asyncio.client import ClientConnection


class _HostedSession(BaseModel):
    model_config = ConfigDict(extra="ignore", strict=True)
    model: str | None = None


class _HostedSessionCreated(BaseModel):
    model_config = ConfigDict(extra="ignore", strict=True)
    session: _HostedSession


class ConciergeXaiVoiceModel(XaiRealtimeModel):
    """Keep compatibility code here until the SDK exposes agent_id and output speed.

    Reuse its handshake lifecycle, codec and cancellation handling. Hosted profiles
    select the model/voice; application instructions, tools and audio remain local.
    """

    def __init__(
        self,
        model: str,
        *,
        provider: XaiProvider,
        settings: XaiRealtimeModelSettings | None = None,
        agent_id: str | None = None,
    ) -> None:
        super().__init__(model, provider=provider, settings=settings)
        self._agent_id = agent_id

    def _session_config(
        self,
        instructions: str,
        tools: list[ToolDefinition] | None,
        *,
        model_settings: XaiRealtimeModelSettings | None,
    ) -> dict[str, Any]:
        config = super()._session_config(instructions, tools, model_settings=model_settings)
        config["audio"]["output"]["speed"] = 1.15
        if self._agent_id is not None:
            config.pop("voice", None)
            # Advertise only tools handled by our local authorization/grounding flow.
            config.setdefault("tools", [])
        return config

    @asynccontextmanager
    async def connect(
        self,
        *,
        messages: Sequence[ModelMessage],
        model_settings: RealtimeModelSettings | None,
        model_request_parameters: ModelRequestParameters,
    ) -> AsyncGenerator[XaiRealtimeConnection]:
        if self._agent_id is None:
            async with super().connect(
                messages=messages,
                model_settings=model_settings,
                model_request_parameters=model_request_parameters,
            ) as connection:
                yield connection
            return

        settings = cast(
            "XaiRealtimeModelSettings", self._merge_model_settings(model_settings) or {}
        )
        if settings.get("reconnect") is not None:
            raise UserError("Hosted xAI profiles require starting a new session after disconnect.")
        config = self._session_config(
            get_instructions(messages, model_request_parameters) or "",
            model_request_parameters.function_tools,
            model_settings=settings,
        )
        url = "wss://api.x.ai/v1/realtime?" + urlencode({"agent_id": self._agent_id})

        async def headers() -> dict[str, str]:
            return {"Authorization": f"Bearer {self._api_key}"}

        def server_model(created: dict[str, Any]) -> str | None:
            return _HostedSessionCreated.model_validate(created).session.model

        async def profile_loaded(ws: ClientConnection, _created: dict[str, Any]) -> None:
            # Hosted agents apply their saved config asynchronously after session.created.
            # Wait for that update before sending ours, or it can overwrite our tools/VAD.
            await expect_event(
                ws, "session.updated", timeout=settings.get("handshake_timeout", 10.0)
            )

        def build_connection(
            ws: ClientConnection,
            _dial: Callable[[], Awaitable[ClientConnection]],
            model_name: str | None,
            model_name_getter: Callable[[], str | None],
        ) -> XaiRealtimeConnection:
            return XaiRealtimeConnection(
                ws,
                model_name=model_name,
                model_name_getter=model_name_getter,
                input_transcription_enabled=settings.get("input_transcription_model", "auto")
                is not None,
                interrupts_response_on_speech=config_interrupts_response_on_speech(config),
            )

        async with connect_openai_protocol(
            model_name=self.model,
            messages=messages,
            profile=self.profile,
            provider_name=self.system,
            session_config=config,
            handshake_timeout=settings.get("handshake_timeout", 10.0),
            dial_headers=headers,
            dial_url=lambda: url,
            session_model=server_model,
            build_connection=build_connection,
            after_session_created=profile_loaded,
        ) as connection:
            yield connection

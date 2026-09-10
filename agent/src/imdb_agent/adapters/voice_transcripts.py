"""Preserve input item IDs absent from Pydantic AI's public SpeechPart events.

The codec still exposes IDs. Correlate finalized transcript parts without changing
provider payloads or using SDK private state, so delayed old commands fail closed.
"""

from __future__ import annotations

from collections import deque
from contextlib import asynccontextmanager
from typing import TYPE_CHECKING

from pydantic_ai.realtime import RealtimeModel
from pydantic_ai.realtime.codec import InputTranscript, RealtimeConnection

if TYPE_CHECKING:
    from collections.abc import AsyncGenerator, AsyncIterator, Callable, Sequence

    from pydantic_ai.messages import ModelMessage
    from pydantic_ai.models import ModelRequestParameters
    from pydantic_ai.realtime import RealtimeModelSettings
    from pydantic_ai.realtime.codec import RealtimeCodecEvent, RealtimeInput
    from pydantic_ai.realtime.profiles import RealtimeModelProfile


class CorrelatedVoiceModel(RealtimeModel):
    def __init__(self, delegate: RealtimeModel) -> None:
        self.delegate = delegate
        self.settings = delegate.settings
        self.final_items: deque[str | None] = deque()

    @property
    def model_name(self) -> str:
        return self.delegate.model_name

    @property
    def system(self) -> str:
        return self.delegate.system

    @property
    def profile(self) -> RealtimeModelProfile:
        return self.delegate.profile

    @asynccontextmanager
    async def connect(
        self,
        *,
        messages: Sequence[ModelMessage],
        model_settings: RealtimeModelSettings | None,
        model_request_parameters: ModelRequestParameters,
    ) -> AsyncGenerator[RealtimeConnection]:
        async with self.delegate.connect(
            messages=messages,
            model_settings=model_settings,
            model_request_parameters=model_request_parameters,
        ) as connection:
            yield CorrelatedConnection(connection, self.final_items)


class CorrelatedConnection(RealtimeConnection):
    def __init__(self, delegate: RealtimeConnection, final_items: deque[str | None]) -> None:
        self.delegate = delegate
        self.final_items = final_items

    async def send(self, content: RealtimeInput) -> None:
        await self.delegate.send(content)

    async def __aiter__(self) -> AsyncIterator[RealtimeCodecEvent]:
        seen: set[str] = set()
        async for event in self.delegate:
            if (
                isinstance(event, InputTranscript)
                and event.is_final
                and (event.item_id is None or event.item_id not in seen)
            ):
                self.final_items.append(event.item_id)
                if event.item_id:
                    seen.add(event.item_id)
            yield event

    @property
    def model_name(self) -> str | None:
        return self.delegate.model_name

    @property
    def input_transcription_enabled(self) -> bool:
        return self.delegate.input_transcription_enabled

    @property
    def reconnect_restores_in_flight_state(self) -> bool:
        return self.delegate.reconnect_restores_in_flight_state

    def set_message_history(self, message_history: Callable[[], Sequence[ModelMessage]]) -> None:
        self.delegate.set_message_history(message_history)

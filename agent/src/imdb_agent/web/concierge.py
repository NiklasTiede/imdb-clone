from __future__ import annotations

import re
from typing import TYPE_CHECKING, Annotated, Final

from fastapi import APIRouter, Header
from fastapi.sse import EventSourceResponse, ServerSentEvent
from pydantic import BaseModel, ConfigDict, Field
from pydantic.alias_generators import to_camel

if TYPE_CHECKING:
    from collections.abc import AsyncIterator

    from imdb_agent.concierge.service import ConciergeService

CLIENT_ID_PATTERN: Final = re.compile(r"^[A-Za-z0-9][A-Za-z0-9._:-]{15,127}$")
CONVERSATION_ID_PATTERN: Final = r"^[a-f0-9]{32}$"


class WebModel(BaseModel):
    model_config = ConfigDict(
        alias_generator=to_camel,
        extra="forbid",
        frozen=True,
        populate_by_name=True,
        serialize_by_alias=True,
        strict=True,
    )


class CreateConversationResponse(WebModel):
    conversation_id: str


class MessageRequest(WebModel):
    message: str = Field(min_length=1, max_length=600)


def create_concierge_router(service: ConciergeService) -> APIRouter:
    router = APIRouter(prefix="/v1", tags=["concierge"])

    async def create_conversation(
        client_id: Annotated[str, Header(alias="X-Concierge-Client-ID")],
    ) -> CreateConversationResponse:
        validated_client_id = _validate_client_id(client_id)
        conversation_id = await service.create_conversation(validated_client_id)
        return CreateConversationResponse(conversation_id=conversation_id)

    async def send_message(
        conversation_id: Annotated[
            str, Field(pattern=CONVERSATION_ID_PATTERN, min_length=32, max_length=32)
        ],
        request: MessageRequest,
        client_id: Annotated[str, Header(alias="X-Concierge-Client-ID")],
    ) -> AsyncIterator[ServerSentEvent]:
        validated_client_id = _validate_client_id(client_id)
        async for event in service.stream_turn(
            client_id=validated_client_id,
            conversation_id=conversation_id,
            message=request.message.strip(),
        ):
            yield ServerSentEvent(
                data=event,
                event=event.type,
                id=str(event.sequence),
            )

    router.add_api_route(
        "/conversations",
        create_conversation,
        methods=["POST"],
        response_model=CreateConversationResponse,
        status_code=201,
    )
    router.add_api_route(
        "/conversations/{conversation_id}/messages",
        send_message,
        methods=["POST"],
        response_class=EventSourceResponse,
        responses={
            200: {
                "description": "Typed Movie Concierge server-sent event stream.",
                "content": {"text/event-stream": {}},
                "headers": {
                    "Cache-Control": {"schema": {"type": "string"}},
                    "X-Accel-Buffering": {"schema": {"type": "string"}},
                },
            }
        },
    )
    return router


def _validate_client_id(client_id: str) -> str:
    if CLIENT_ID_PATTERN.fullmatch(client_id) is None:
        from fastapi import HTTPException

        raise HTTPException(status_code=422, detail="Invalid concierge client identifier.")
    return client_id

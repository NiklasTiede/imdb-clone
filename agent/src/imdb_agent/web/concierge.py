from __future__ import annotations

import hashlib
import re
from typing import TYPE_CHECKING, Annotated, Final

from fastapi import APIRouter, Depends, Header, HTTPException
from fastapi.sse import EventSourceResponse, ServerSentEvent
from pydantic import BaseModel, ConfigDict, Field, SecretStr
from pydantic.alias_generators import to_camel

from imdb_agent.concierge.personal import DelegationRejectedError, DelegationVerifier

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


def create_concierge_router(
    service: ConciergeService, verifier: DelegationVerifier | None = None
) -> APIRouter:
    router = APIRouter(prefix="/v1", tags=["concierge"])

    async def owner(
        client_id: Annotated[str, Header(alias="X-Concierge-Client-ID")],
        delegation: Annotated[str | None, Header(alias="X-Concierge-Delegation")] = None,
    ) -> str:
        validated = _validate_client_id(client_id)
        if delegation is None:
            return validated
        if verifier is None or len(delegation) > 1024:
            raise HTTPException(status_code=401, detail="Sign in again to use your watchlist.")
        try:
            binding = await verifier.verify(SecretStr(delegation))
        except DelegationRejectedError:
            raise HTTPException(
                status_code=401, detail="Sign in again to use your watchlist."
            ) from None
        return hashlib.sha256(f"{validated}:{binding}".encode()).hexdigest()

    async def create_conversation(
        client_id: Annotated[str, Header(alias="X-Concierge-Client-ID")],
        delegation: Annotated[str | None, Header(alias="X-Concierge-Delegation")] = None,
    ) -> CreateConversationResponse:
        validated_client_id = await owner(client_id, delegation)
        conversation_id = await service.create_conversation(validated_client_id)
        return CreateConversationResponse(conversation_id=conversation_id)

    owner_dependency = Depends(owner)

    async def send_message(
        conversation_id: Annotated[
            str, Field(pattern=CONVERSATION_ID_PATTERN, min_length=32, max_length=32)
        ],
        request: MessageRequest,
        validated_client_id: str = owner_dependency,
        delegation: Annotated[str | None, Header(alias="X-Concierge-Delegation")] = None,
    ) -> AsyncIterator[ServerSentEvent]:
        async for event in service.stream_turn(
            client_id=validated_client_id,
            conversation_id=conversation_id,
            message=request.message.strip(),
            delegation=SecretStr(delegation) if delegation else None,
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

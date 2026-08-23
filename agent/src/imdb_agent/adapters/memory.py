from __future__ import annotations

import asyncio
from dataclasses import dataclass, field
from decimal import Decimal
from uuid import uuid4

from imdb_agent.concierge.ports import (
    BudgetExhaustedError,
    BudgetReservation,
    ConversationBusyError,
    ConversationMessage,
    ConversationNotFoundError,
)


@dataclass(slots=True)
class _Conversation:
    client_id: str
    messages: list[ConversationMessage] = field(default_factory=lambda: list[ConversationMessage]())
    active: bool = False
    access_order: int = 0


class InMemoryConversationStore:
    """Process-local bounded sessions; a restart intentionally clears every conversation."""

    def __init__(
        self,
        *,
        max_conversations: int = 500,
        max_messages: int = 16,
        max_content_chars: int = 12_000,
    ) -> None:
        self._max_conversations = max_conversations
        self._max_messages = max_messages
        self._max_content_chars = max_content_chars
        self._conversations: dict[str, _Conversation] = {}
        self._lock = asyncio.Lock()
        self._access_order = 0

    async def create(self, client_id: str) -> str:
        async with self._lock:
            self._make_space()
            conversation_id = uuid4().hex
            self._conversations[conversation_id] = _Conversation(
                client_id=client_id,
                access_order=self._next_access_order(),
            )
            return conversation_id

    async def begin_turn(
        self, client_id: str, conversation_id: str, message: str
    ) -> tuple[ConversationMessage, ...]:
        async with self._lock:
            conversation = self._owned(client_id, conversation_id)
            if conversation.active:
                raise ConversationBusyError
            conversation.active = True
            conversation.access_order = self._next_access_order()
            history = tuple(conversation.messages)
            conversation.messages.append(ConversationMessage(role="user", content=message))
            self._trim(conversation)
            return history

    async def complete_turn(
        self,
        client_id: str,
        conversation_id: str,
        response: ConversationMessage,
    ) -> None:
        async with self._lock:
            conversation = self._owned(client_id, conversation_id)
            conversation.messages.append(response)
            conversation.active = False
            conversation.access_order = self._next_access_order()
            self._trim(conversation)

    async def fail_turn(self, client_id: str, conversation_id: str) -> None:
        async with self._lock:
            try:
                conversation = self._owned(client_id, conversation_id)
            except ConversationNotFoundError:
                return
            if conversation.active and conversation.messages:
                conversation.messages.pop()
            conversation.active = False
            conversation.access_order = self._next_access_order()

    async def snapshot(
        self, client_id: str, conversation_id: str
    ) -> tuple[ConversationMessage, ...]:
        async with self._lock:
            return tuple(self._owned(client_id, conversation_id).messages)

    def _owned(self, client_id: str, conversation_id: str) -> _Conversation:
        conversation = self._conversations.get(conversation_id)
        if conversation is None or conversation.client_id != client_id:
            raise ConversationNotFoundError
        return conversation

    def _trim(self, conversation: _Conversation) -> None:
        while len(conversation.messages) > self._max_messages:
            conversation.messages.pop(0)
        while (
            len(conversation.messages) > 1
            and sum(len(message.content) for message in conversation.messages)
            > self._max_content_chars
        ):
            conversation.messages.pop(0)

    def _make_space(self) -> None:
        if len(self._conversations) < self._max_conversations:
            return
        inactive = (
            (conversation_id, conversation)
            for conversation_id, conversation in self._conversations.items()
            if not conversation.active
        )
        oldest_id, _oldest = min(inactive, key=lambda item: item[1].access_order)
        del self._conversations[oldest_id]

    def _next_access_order(self) -> int:
        self._access_order += 1
        return self._access_order


class InMemoryCostLedger:
    """Pessimistically reserves each run so concurrent requests cannot exceed the cap."""

    def __init__(self, *, project_limit_usd: Decimal, per_run_limit_usd: Decimal) -> None:
        self._project_limit_usd = project_limit_usd
        self._per_run_limit_usd = per_run_limit_usd
        self._committed_usd = Decimal(0)
        self._reserved_usd = Decimal(0)
        self._lock = asyncio.Lock()

    async def reserve(self) -> BudgetReservation:
        async with self._lock:
            next_total = self._committed_usd + self._reserved_usd + self._per_run_limit_usd
            if next_total > self._project_limit_usd:
                raise BudgetExhaustedError
            self._reserved_usd += self._per_run_limit_usd
            return BudgetReservation(amount_usd=self._per_run_limit_usd)

    async def settle(
        self,
        reservation: BudgetReservation,
        actual_cost_usd: Decimal | None,
        *,
        succeeded: bool,
    ) -> Decimal:
        async with self._lock:
            self._reserved_usd = max(Decimal(0), self._reserved_usd - reservation.amount_usd)
            charged = (
                actual_cost_usd
                if succeeded and actual_cost_usd is not None
                else reservation.amount_usd
            )
            self._committed_usd = min(self._project_limit_usd, self._committed_usd + charged)
            return self._committed_usd

    @property
    def committed_usd(self) -> Decimal:
        return self._committed_usd

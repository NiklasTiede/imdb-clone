from __future__ import annotations

from dataclasses import dataclass
from typing import TYPE_CHECKING, Protocol

if TYPE_CHECKING:
    from collections.abc import AsyncIterator
    from decimal import Decimal

    from imdb_agent.concierge.events import GroundedMovie, RunnerEvent, UsageSummary
    from imdb_agent.concierge.tools import ToolName


@dataclass(frozen=True, slots=True)
class ConversationMessage:
    role: str
    content: str
    movies: tuple[GroundedMovie, ...] = ()


@dataclass(frozen=True, slots=True)
class RunRequest:
    conversation_id: str
    message: str
    history: tuple[ConversationMessage, ...]


class ConciergeRunner(Protocol):
    def stream(self, request: RunRequest) -> AsyncIterator[RunnerEvent]: ...


class ConversationNotFoundError(LookupError):
    """Conversation is absent or belongs to another client."""


class ConversationBusyError(RuntimeError):
    """A conversation already has an active turn."""


class ConversationStore(Protocol):
    async def create(self, client_id: str) -> str: ...

    async def begin_turn(
        self, client_id: str, conversation_id: str, message: str
    ) -> tuple[ConversationMessage, ...]: ...

    async def complete_turn(
        self,
        client_id: str,
        conversation_id: str,
        response: ConversationMessage,
    ) -> None: ...

    async def fail_turn(self, client_id: str, conversation_id: str) -> None: ...


@dataclass(frozen=True, slots=True)
class BudgetReservation:
    amount_usd: Decimal


class BudgetExhaustedError(RuntimeError):
    """The configured process inference budget cannot fund another run."""


class CostLedger(Protocol):
    async def reserve(self) -> BudgetReservation: ...

    async def settle(
        self,
        reservation: BudgetReservation,
        actual_cost_usd: Decimal | None,
        *,
        succeeded: bool,
    ) -> Decimal: ...


class RunObserver(Protocol):
    def started(self) -> None: ...

    def first_event(self, duration_seconds: float) -> None: ...

    def budget_committed(self, amount_usd: Decimal) -> None: ...

    def tool_called(self, tool_name: ToolName) -> None: ...

    def ui_action(self, *, action: str, outcome: str) -> None: ...

    def finished(
        self,
        *,
        outcome: str,
        duration_seconds: float,
        usage: UsageSummary | None,
    ) -> None: ...

    def disconnected(self) -> None: ...

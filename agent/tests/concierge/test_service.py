from __future__ import annotations

from decimal import Decimal
from typing import TYPE_CHECKING

import pytest

from imdb_agent.adapters.fakes import FakeConciergeRunner
from imdb_agent.adapters.memory import InMemoryConversationStore, InMemoryCostLedger
from imdb_agent.concierge.events import (
    CompletionEvent,
    CompletionOutcome,
    ErrorEvent,
    MovieCardEvent,
    UsageSummary,
)
from imdb_agent.concierge.service import ConciergeService

if TYPE_CHECKING:
    from imdb_agent.concierge.events import ConciergeEvent

pytestmark = pytest.mark.asyncio


class RecordingObserver:
    def __init__(self) -> None:
        self.outcomes: list[str] = []
        self.disconnects = 0

    def started(self) -> None:
        return None

    def tool_called(self, tool_name: str) -> None:
        return None

    def finished(
        self,
        *,
        outcome: str,
        duration_seconds: float,
        usage: UsageSummary | None,
    ) -> None:
        self.outcomes.append(outcome)

    def disconnected(self) -> None:
        self.disconnects += 1


async def collect_events(
    service: ConciergeService,
    *,
    client_id: str,
    conversation_id: str,
    message: str,
) -> list[ConciergeEvent]:
    return [
        event
        async for event in service.stream_turn(
            client_id=client_id,
            conversation_id=conversation_id,
            message=message,
        )
    ]


def service_fixture() -> tuple[
    ConciergeService, InMemoryConversationStore, InMemoryCostLedger, RecordingObserver
]:
    store = InMemoryConversationStore()
    ledger = InMemoryCostLedger(project_limit_usd=Decimal("20"), per_run_limit_usd=Decimal("0.25"))
    observer = RecordingObserver()
    return (
        ConciergeService(
            runner=FakeConciergeRunner(),
            conversations=store,
            cost_ledger=ledger,
            observer=observer,
        ),
        store,
        ledger,
        observer,
    )


async def test_streams_typed_grounded_events_and_persists_bounded_history() -> None:
    service, store, ledger, observer = service_fixture()
    conversation_id = await service.create_conversation("browser-client-0001")

    events = await collect_events(
        service,
        client_id="browser-client-0001",
        conversation_id=conversation_id,
        message="Find thoughtful science fiction under two hours.",
    )

    assert [event.sequence for event in events] == list(range(1, len(events) + 1))
    assert any(
        isinstance(event, MovieCardEvent) and event.movie.primary_title == "Arrival"
        for event in events
    )
    assert events[-1] == CompletionEvent(
        sequence=len(events),
        conversation_id=conversation_id,
        outcome=CompletionOutcome.SUCCESS,
    )
    history = await store.snapshot("browser-client-0001", conversation_id)
    assert [message.role for message in history] == ["user", "assistant"]
    assert history[-1].movies[0].movie_id == 42
    assert ledger.committed_usd == 0
    assert observer.outcomes == ["success"]


async def test_conversations_are_isolated_by_browser_client() -> None:
    service, _store, _ledger, _observer = service_fixture()
    conversation_id = await service.create_conversation("browser-client-0001")

    events = await collect_events(
        service,
        client_id="browser-client-0002",
        conversation_id=conversation_id,
        message="Show me the previous result.",
    )

    assert isinstance(events[0], ErrorEvent)
    assert events[0].code == "conversation_not_found"
    assert not any(isinstance(event, MovieCardEvent) for event in events)
    assert isinstance(events[-1], CompletionEvent)
    assert events[-1].outcome == "error"


async def test_project_budget_rejects_request_before_runner_is_called() -> None:
    store = InMemoryConversationStore()
    service = ConciergeService(
        runner=FakeConciergeRunner(estimated_cost_usd=Decimal("0.10")),
        conversations=store,
        cost_ledger=InMemoryCostLedger(
            project_limit_usd=Decimal("0.10"),
            per_run_limit_usd=Decimal("0.10"),
        ),
        observer=RecordingObserver(),
    )
    conversation_id = await service.create_conversation("browser-client-0001")
    first = await collect_events(
        service,
        client_id="browser-client-0001",
        conversation_id=conversation_id,
        message="Find Arrival.",
    )
    second = await collect_events(
        service,
        client_id="browser-client-0001",
        conversation_id=conversation_id,
        message="Find another.",
    )

    assert isinstance(first[-1], CompletionEvent)
    assert first[-1].outcome == "success"
    assert isinstance(second[0], ErrorEvent)
    assert second[0].code == "project_budget_exhausted"

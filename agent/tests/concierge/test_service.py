from __future__ import annotations

import asyncio
from decimal import Decimal
from typing import TYPE_CHECKING

import pytest

from imdb_agent.adapters.fakes import FakeConciergeRunner, fake_arrival
from imdb_agent.adapters.memory import InMemoryConversationStore, InMemoryCostLedger
from imdb_agent.concierge.events import (
    CompletionEvent,
    CompletionOutcome,
    ErrorEvent,
    MovieCardEvent,
    TextEvent,
    UiActionEvent,
    UsageEvent,
    UsageSummary,
)
from imdb_agent.concierge.ports import ConversationNotFoundError, RunRequest
from imdb_agent.concierge.service import ConciergeRunError, ConciergeService
from imdb_agent.concierge.tools import ToolName

if TYPE_CHECKING:
    from collections.abc import AsyncIterator

    from imdb_agent.concierge.events import ConciergeEvent, RunnerEvent

pytestmark = pytest.mark.asyncio


class RecordingObserver:
    def __init__(self) -> None:
        self.outcomes: list[str] = []
        self.disconnects = 0
        self.first_event_durations: list[float] = []
        self.committed_budget: list[Decimal] = []
        self.tools: list[ToolName] = []
        self.ui_actions: list[tuple[str, str]] = []

    def started(self) -> None:
        return None

    def first_event(self, duration_seconds: float) -> None:
        self.first_event_durations.append(duration_seconds)

    def budget_committed(self, amount_usd: Decimal) -> None:
        self.committed_budget.append(amount_usd)

    def tool_called(self, tool_name: ToolName) -> None:
        self.tools.append(tool_name)

    def ui_action(self, *, action: str, outcome: str) -> None:
        self.ui_actions.append((action, outcome))

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
    assert observer.committed_budget == [Decimal(0)]
    assert observer.outcomes == ["success"]
    assert observer.tools == [ToolName.SEARCH_MOVIES]


class NeverCalledRunner:
    async def stream(self, request: RunRequest) -> AsyncIterator[RunnerEvent]:
        raise AssertionError(f"runner must not handle capability help: {request.message}")
        yield TextEvent(delta="unreachable")  # pragma: no cover


async def test_capability_help_is_local_persisted_and_free_of_model_usage() -> None:
    store = InMemoryConversationStore()
    ledger = InMemoryCostLedger(
        project_limit_usd=Decimal(0),
        per_run_limit_usd=Decimal("0.25"),
    )
    observer = RecordingObserver()
    service = ConciergeService(
        runner=NeverCalledRunner(),
        conversations=store,
        cost_ledger=ledger,
        observer=observer,
        max_concurrent_runs=0,
    )
    conversation_id = await service.create_conversation("browser-client-0001")

    events = await collect_events(
        service,
        client_id="browser-client-0001",
        conversation_id=conversation_id,
        message="What kind of actions can I do with you?",
    )

    response = next(event.delta for event in events if isinstance(event, TextEvent))
    assert "Open one movie page" in response
    assert not any(isinstance(event, UsageEvent) for event in events)
    assert events[-1].type == "completion"
    assert events[-1].outcome == "success"
    history = await store.snapshot("browser-client-0001", conversation_id)
    assert history[-1].content == response
    assert ledger.committed_usd == 0
    assert observer.committed_budget == []


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


async def test_explicit_open_request_emits_grounded_action_after_current_run_card() -> None:
    service, _store, _ledger, observer = service_fixture()
    conversation_id = await service.create_conversation("browser-client-0001")

    events = await collect_events(
        service,
        client_id="browser-client-0001",
        conversation_id=conversation_id,
        message="Open Arrival.",
    )

    card_index = next(
        index for index, event in enumerate(events) if isinstance(event, MovieCardEvent)
    )
    action_index = next(
        index for index, event in enumerate(events) if isinstance(event, UiActionEvent)
    )
    action_event = events[action_index]
    assert isinstance(action_event, UiActionEvent)
    assert action_event.action.movie_id == 42
    assert card_index < action_index < len(events) - 1
    assert observer.ui_actions == [("open_movie", "emitted")]


class TwoMovieRunner:
    async def stream(self, request: RunRequest) -> AsyncIterator[RunnerEvent]:
        yield MovieCardEvent(movie=fake_arrival())
        yield MovieCardEvent(
            movie=fake_arrival().model_copy(
                update={"movie_id": 43, "primary_title": "Contact", "start_year": 1997}
            )
        )
        yield TextEvent(delta="I found two grounded possibilities.")


async def test_ambiguous_open_request_emits_no_action_and_records_rejection() -> None:
    store = InMemoryConversationStore()
    observer = RecordingObserver()
    service = ConciergeService(
        runner=TwoMovieRunner(),
        conversations=store,
        cost_ledger=InMemoryCostLedger(
            project_limit_usd=Decimal("20"), per_run_limit_usd=Decimal("0.25")
        ),
        observer=observer,
    )
    conversation_id = await service.create_conversation("browser-client-0001")

    events = await collect_events(
        service,
        client_id="browser-client-0001",
        conversation_id=conversation_id,
        message="Open one of those.",
    )

    assert not any(isinstance(event, UiActionEvent) for event in events)
    assert observer.ui_actions == [("open_movie", "rejected")]


class StaleContextRunner:
    def __init__(self) -> None:
        self._run = 0

    async def stream(self, request: RunRequest) -> AsyncIterator[RunnerEvent]:
        self._run += 1
        if self._run == 1:
            yield MovieCardEvent(movie=fake_arrival())
            yield TextEvent(delta="Arrival is grounded for this turn.")
        else:
            assert request.history[-1].movies[0].movie_id == 42
            yield TextEvent(delta="I could not re-ground that movie in this turn.")


async def test_stale_card_cannot_navigate_without_current_grounding() -> None:
    store = InMemoryConversationStore()
    observer = RecordingObserver()
    service = ConciergeService(
        runner=StaleContextRunner(),
        conversations=store,
        cost_ledger=InMemoryCostLedger(
            project_limit_usd=Decimal("20"), per_run_limit_usd=Decimal("0.25")
        ),
        observer=observer,
    )
    conversation_id = await service.create_conversation("browser-client-0001")
    await collect_events(
        service,
        client_id="browser-client-0001",
        conversation_id=conversation_id,
        message="Find Arrival.",
    )

    events = await collect_events(
        service,
        client_id="browser-client-0001",
        conversation_id=conversation_id,
        message="Open it.",
    )

    assert not any(isinstance(event, UiActionEvent) for event in events)
    assert observer.ui_actions == [("open_movie", "rejected")]


class FailingOpenRunner:
    async def stream(self, request: RunRequest) -> AsyncIterator[RunnerEvent]:
        raise ConciergeRunError(
            "tool_unavailable",
            "The movie catalog is temporarily unavailable.",
            retryable=True,
        )
        yield UsageEvent  # pragma: no cover - keeps this an async generator


async def test_tool_failure_cannot_emit_action_and_records_rejection() -> None:
    store = InMemoryConversationStore()
    observer = RecordingObserver()
    service = ConciergeService(
        runner=FailingOpenRunner(),
        conversations=store,
        cost_ledger=InMemoryCostLedger(
            project_limit_usd=Decimal("20"), per_run_limit_usd=Decimal("0.25")
        ),
        observer=observer,
    )
    conversation_id = await service.create_conversation("browser-client-0001")

    events = await collect_events(
        service,
        client_id="browser-client-0001",
        conversation_id=conversation_id,
        message="Open Arrival.",
    )

    assert not any(isinstance(event, UiActionEvent) for event in events)
    assert any(
        isinstance(event, ErrorEvent) and event.code == "tool_unavailable" for event in events
    )
    assert observer.ui_actions == [("open_movie", "rejected")]


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


class BlockingRunner:
    def __init__(self) -> None:
        self.started = asyncio.Event()
        self.release = asyncio.Event()

    async def stream(self, request: RunRequest) -> AsyncIterator[RunnerEvent]:
        self.started.set()
        await self.release.wait()
        yield TextEvent(delta="Capacity released.")


async def test_global_capacity_rejects_without_queueing_or_calling_second_run() -> None:
    store = InMemoryConversationStore()
    runner = BlockingRunner()
    observer = RecordingObserver()
    service = ConciergeService(
        runner=runner,
        conversations=store,
        cost_ledger=InMemoryCostLedger(
            project_limit_usd=Decimal("20"),
            per_run_limit_usd=Decimal("0.25"),
        ),
        observer=observer,
        max_concurrent_runs=1,
    )
    first_conversation = await service.create_conversation("browser-client-0001")
    second_conversation = await service.create_conversation("browser-client-0002")
    first_task = asyncio.create_task(
        collect_events(
            service,
            client_id="browser-client-0001",
            conversation_id=first_conversation,
            message="Find Arrival.",
        )
    )
    await runner.started.wait()

    second = await collect_events(
        service,
        client_id="browser-client-0002",
        conversation_id=second_conversation,
        message="Find another.",
    )
    runner.release.set()
    await first_task

    assert isinstance(second[0], ErrorEvent)
    assert second[0].code == "concierge_busy"
    assert isinstance(second[-1], CompletionEvent)
    assert second[-1].outcome == "error"
    assert "capacity_exhausted" in observer.outcomes


async def test_conversation_store_evicts_oldest_inactive_session_at_bound() -> None:
    store = InMemoryConversationStore(max_conversations=2)
    first = await store.create("browser-client-0001")
    second = await store.create("browser-client-0002")

    third = await store.create("browser-client-0003")

    with pytest.raises(ConversationNotFoundError):
        await store.snapshot("browser-client-0001", first)
    assert await store.snapshot("browser-client-0002", second) == ()
    assert await store.snapshot("browser-client-0003", third) == ()

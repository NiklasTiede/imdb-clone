from __future__ import annotations

import asyncio
from time import perf_counter
from typing import TYPE_CHECKING

from imdb_agent.concierge.events import (
    CompletionEvent,
    CompletionOutcome,
    ConciergeEvent,
    ErrorEvent,
    GroundedMovie,
    RunStatus,
    StatusEvent,
    TextEvent,
    ToolCallEvent,
    UiActionEvent,
)
from imdb_agent.concierge.policy import (
    TOOL_STATUSES,
    UiActionDecisionOutcome,
    capability_response,
    decide_open_movie_action,
    requests_open_movie,
)
from imdb_agent.concierge.ports import (
    BudgetExhaustedError,
    ConciergeRunner,
    ConversationBusyError,
    ConversationMessage,
    ConversationNotFoundError,
    ConversationStore,
    CostLedger,
    RunObserver,
    RunRequest,
)

if TYPE_CHECKING:
    from collections.abc import AsyncIterator


class ConciergeRunError(RuntimeError):
    def __init__(self, code: str, safe_message: str, *, retryable: bool) -> None:
        super().__init__(safe_message)
        self.code = code
        self.safe_message = safe_message
        self.retryable = retryable


class _RunCapacityExhaustedError(RuntimeError):
    """The process already has the configured number of active model runs."""


class _RunCapacity:
    def __init__(self, maximum: int) -> None:
        self._maximum = maximum
        self._active = 0
        self._lock = asyncio.Lock()

    async def try_acquire(self) -> bool:
        async with self._lock:
            if self._active >= self._maximum:
                return False
            self._active += 1
            return True

    async def release(self) -> None:
        async with self._lock:
            self._active = max(0, self._active - 1)


class ConciergeService:
    def __init__(
        self,
        *,
        runner: ConciergeRunner,
        conversations: ConversationStore,
        cost_ledger: CostLedger,
        observer: RunObserver,
        max_concurrent_runs: int = 2,
    ) -> None:
        self._runner = runner
        self._conversations = conversations
        self._cost_ledger = cost_ledger
        self._observer = observer
        self._capacity = _RunCapacity(max_concurrent_runs)

    async def create_conversation(self, client_id: str) -> str:
        return await self._conversations.create(client_id)

    async def stream_turn(
        self, *, client_id: str, conversation_id: str, message: str
    ) -> AsyncIterator[ConciergeEvent]:
        started_at = perf_counter()
        sequence = 0
        completion_outcome = CompletionOutcome.ERROR
        metric_outcome = "internal_error"
        usage = None
        reservation = None
        turn_started = False
        capacity_acquired = False
        first_event_observed = False
        ui_action_observed = False
        text_parts: list[str] = []
        movies_by_id: dict[int, GroundedMovie] = {}
        self._observer.started()
        local_response = capability_response(message)

        def next_event(event: ConciergeEvent) -> ConciergeEvent:
            nonlocal sequence
            sequence += 1
            return event.model_copy(update={"sequence": sequence})

        try:
            if local_response is None:
                capacity_acquired = await self._capacity.try_acquire()
                if not capacity_acquired:
                    raise _RunCapacityExhaustedError
            history = await self._conversations.begin_turn(client_id, conversation_id, message)
            turn_started = True
            if local_response is not None:
                yield next_event(StatusEvent(status=RunStatus.THINKING))
                self._observer.first_event(perf_counter() - started_at)
                first_event_observed = True
                text_parts.append(local_response)
                yield next_event(TextEvent(delta=local_response))
            else:
                reservation = await self._cost_ledger.reserve()
                yield next_event(StatusEvent(status=RunStatus.THINKING))
                request = RunRequest(
                    conversation_id=conversation_id,
                    message=message,
                    history=history,
                )
                async for event in self._runner.stream(request):
                    if not first_event_observed:
                        self._observer.first_event(perf_counter() - started_at)
                        first_event_observed = True
                    if isinstance(event, ToolCallEvent):
                        self._observer.tool_called(event.tool)
                        yield next_event(StatusEvent(status=TOOL_STATUSES[event.tool]))
                        continue
                    if isinstance(event, TextEvent):
                        text_parts.append(event.delta)
                    elif event.type == "movie-card":
                        movies_by_id[event.movie.movie_id] = event.movie
                    else:
                        usage = event.usage
                    yield next_event(event)

            response_text = "".join(text_parts).strip()
            if not response_text:
                response_text = "I found grounded catalog results for you."
            await self._conversations.complete_turn(
                client_id,
                conversation_id,
                ConversationMessage(
                    role="assistant",
                    content=response_text,
                    movies=tuple(movies_by_id.values()),
                ),
            )
            turn_started = False
            completion_outcome = CompletionOutcome.SUCCESS
            metric_outcome = "success"
            if reservation is not None:
                self._observer.budget_committed(
                    await self._cost_ledger.settle(
                        reservation,
                        usage.estimated_cost_usd if usage is not None else None,
                        succeeded=True,
                    )
                )
                reservation = None
            action_decision = decide_open_movie_action(
                message,
                tuple(movies_by_id.values()),
            )
            if action_decision.outcome is not UiActionDecisionOutcome.NOT_REQUESTED:
                self._observer.ui_action(
                    action="open_movie",
                    outcome=action_decision.outcome,
                )
                ui_action_observed = True
            if action_decision.action is not None:
                yield next_event(UiActionEvent(action=action_decision.action))
        except ConversationNotFoundError:
            metric_outcome = "conversation_not_found"
            yield next_event(
                ErrorEvent(
                    code="conversation_not_found",
                    message="This conversation is unavailable. Start a new conversation.",
                    retryable=False,
                )
            )
        except ConversationBusyError:
            metric_outcome = "conversation_busy"
            yield next_event(
                ErrorEvent(
                    code="conversation_busy",
                    message="This conversation is already handling a message.",
                    retryable=True,
                )
            )
        except BudgetExhaustedError:
            metric_outcome = "budget_exhausted"
            yield next_event(
                ErrorEvent(
                    code="project_budget_exhausted",
                    message="The model budget is exhausted. No model request was sent.",
                    retryable=False,
                )
            )
        except _RunCapacityExhaustedError:
            metric_outcome = "capacity_exhausted"
            yield next_event(
                ErrorEvent(
                    code="concierge_busy",
                    message="The Movie Concierge is at capacity. Please try again shortly.",
                    retryable=True,
                )
            )
        except ConciergeRunError as error:
            metric_outcome = error.code
            yield next_event(
                ErrorEvent(
                    code=error.code,
                    message=error.safe_message,
                    retryable=error.retryable,
                )
            )
        except asyncio.CancelledError:
            completion_outcome = CompletionOutcome.CANCELLED
            metric_outcome = "cancelled"
            self._observer.disconnected()
            raise
        except Exception:
            metric_outcome = "internal_error"
            yield next_event(
                ErrorEvent(
                    code="concierge_unavailable",
                    message="The Movie Concierge is temporarily unavailable.",
                    retryable=True,
                )
            )
        finally:
            if not ui_action_observed and requests_open_movie(message):
                self._observer.ui_action(action="open_movie", outcome="rejected")
            if turn_started:
                await self._conversations.fail_turn(client_id, conversation_id)
            if reservation is not None:
                self._observer.budget_committed(
                    await self._cost_ledger.settle(reservation, None, succeeded=False)
                )
            if capacity_acquired:
                await self._capacity.release()
            self._observer.finished(
                outcome=metric_outcome,
                duration_seconds=perf_counter() - started_at,
                usage=usage,
            )

        yield next_event(
            CompletionEvent(conversation_id=conversation_id, outcome=completion_outcome)
        )

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
    UsageEvent,
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

_STATUS_TO_TOOL = {
    RunStatus.SEARCHING: "search_movies",
    RunStatus.FETCHING_DETAILS: "get_movie_details",
    RunStatus.FINDING_SIMILAR: "get_similar_movies",
    RunStatus.CHOOSING_TONIGHT: "get_tonight_picks",
}

if TYPE_CHECKING:
    from collections.abc import AsyncIterator


class ConciergeRunError(RuntimeError):
    def __init__(self, code: str, safe_message: str, *, retryable: bool) -> None:
        super().__init__(safe_message)
        self.code = code
        self.safe_message = safe_message
        self.retryable = retryable


class ConciergeService:
    def __init__(
        self,
        *,
        runner: ConciergeRunner,
        conversations: ConversationStore,
        cost_ledger: CostLedger,
        observer: RunObserver,
    ) -> None:
        self._runner = runner
        self._conversations = conversations
        self._cost_ledger = cost_ledger
        self._observer = observer

    async def create_conversation(self, client_id: str) -> str:
        return await self._conversations.create(client_id)

    async def stream_turn(
        self, *, client_id: str, conversation_id: str, message: str
    ) -> AsyncIterator[ConciergeEvent]:
        started_at = perf_counter()
        sequence = 0
        outcome = CompletionOutcome.ERROR
        usage = None
        reservation = None
        turn_started = False
        text_parts: list[str] = []
        movies_by_id: dict[int, GroundedMovie] = {}
        self._observer.started()

        def next_event(event: ConciergeEvent) -> ConciergeEvent:
            nonlocal sequence
            sequence += 1
            return event.model_copy(update={"sequence": sequence})

        try:
            history = await self._conversations.begin_turn(client_id, conversation_id, message)
            turn_started = True
            reservation = await self._cost_ledger.reserve()
            yield next_event(StatusEvent(status=RunStatus.THINKING))

            request = RunRequest(
                conversation_id=conversation_id,
                message=message,
                history=history,
            )
            async for event in self._runner.stream(request):
                if isinstance(event, TextEvent):
                    text_parts.append(event.delta)
                elif event.type == "movie-card":
                    movies_by_id[event.movie.movie_id] = event.movie
                elif isinstance(event, UsageEvent):
                    usage = event.usage
                else:
                    self._observer.tool_called(_STATUS_TO_TOOL[event.status])
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
            outcome = CompletionOutcome.SUCCESS
            await self._cost_ledger.settle(
                reservation,
                usage.estimated_cost_usd if usage is not None else None,
                succeeded=True,
            )
            reservation = None
        except ConversationNotFoundError:
            yield next_event(
                ErrorEvent(
                    code="conversation_not_found",
                    message="This conversation is unavailable. Start a new conversation.",
                    retryable=False,
                )
            )
        except ConversationBusyError:
            yield next_event(
                ErrorEvent(
                    code="conversation_busy",
                    message="This conversation is already handling a message.",
                    retryable=True,
                )
            )
        except BudgetExhaustedError:
            yield next_event(
                ErrorEvent(
                    code="project_budget_exhausted",
                    message="The local model budget is exhausted. No model request was sent.",
                    retryable=False,
                )
            )
        except ConciergeRunError as error:
            yield next_event(
                ErrorEvent(
                    code=error.code,
                    message=error.safe_message,
                    retryable=error.retryable,
                )
            )
        except asyncio.CancelledError:
            outcome = CompletionOutcome.CANCELLED
            self._observer.disconnected()
            raise
        except Exception:
            yield next_event(
                ErrorEvent(
                    code="concierge_unavailable",
                    message="The Movie Concierge is temporarily unavailable.",
                    retryable=True,
                )
            )
        finally:
            if turn_started:
                await self._conversations.fail_turn(client_id, conversation_id)
            if reservation is not None:
                await self._cost_ledger.settle(reservation, None, succeeded=False)
            self._observer.finished(
                outcome=outcome.value,
                duration_seconds=perf_counter() - started_at,
                usage=usage,
            )

        yield next_event(CompletionEvent(conversation_id=conversation_id, outcome=outcome))

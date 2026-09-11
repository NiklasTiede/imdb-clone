from __future__ import annotations

import asyncio
import json
from contextlib import asynccontextmanager
from typing import TYPE_CHECKING

import pytest
from pydantic_ai import Agent
from pydantic_ai.exceptions import ToolFailed
from pydantic_ai.messages import (
    BinaryAudio,
    ModelMessage,
    RealtimeInputSpeechEndEvent,
    RealtimeInputSpeechStartEvent,
)
from pydantic_ai.realtime import RealtimeModel, RealtimeModelSettings
from pydantic_ai.realtime.codec import (
    AudioDelta,
    InputTranscript,
    OutputTranscript,
    RealtimeCodecEvent,
    RealtimeConnection,
    RealtimeInput,
    ResponseDone,
    ToolCall,
    ToolResult,
)
from pydantic_ai.realtime.profiles import RealtimeModelProfile
from structlog.testing import capture_logs

from imdb_agent.adapters.realtime_voice import relay_voice
from imdb_agent.concierge.voice import VoiceCommand, VoiceEvent

if TYPE_CHECKING:
    from collections.abc import AsyncGenerator, AsyncIterator, Sequence

    from pydantic_ai.models import ModelRequestParameters


class CatalogConnection(RealtimeConnection):
    def __init__(self, *, late_transcript: bool = False, old_command: bool = False) -> None:
        self.events: asyncio.Queue[RealtimeCodecEvent] = asyncio.Queue()
        self.closed = False
        self.late_transcript = late_transcript
        self.old_command = old_command
        self.tool_called = False

    async def send(self, content: RealtimeInput) -> None:
        if isinstance(content, BinaryAudio):
            await self.events.put(RealtimeInputSpeechStartEvent(item_id="user-1"))
            if self.old_command:
                await self.events.put(RealtimeInputSpeechStartEvent(item_id="user-2"))
                await self.transcript()
                await self.events.put(
                    InputTranscript("Just tell me the runtime", is_final=True, item_id="user-2")
                )
            elif not self.late_transcript:
                await self.transcript()
            await self.events.put(
                ToolCall(
                    "lookup", tool_name="search_movies", args=json.dumps({"query": "Forrest Gump"})
                )
            )
            await self.events.put(ResponseDone())
        elif isinstance(content, ToolResult):
            self.tool_called = True
            await self.events.put(OutputTranscript("Forrest Gump runs for 142 minutes."))
            await self.events.put(AudioDelta(b"\x00\x01" * 2400))
            await self.events.put(ResponseDone())
            if self.late_transcript:
                await self.transcript()

    async def transcript(self) -> None:
        await self.events.put(
            InputTranscript("Find Forrest Gump and open it", is_final=True, item_id="user-1")
        )

    async def __aiter__(self) -> AsyncIterator[RealtimeCodecEvent]:
        while True:
            yield await self.events.get()


class CatalogModel(RealtimeModel):
    def __init__(self, *, late_transcript: bool = False, old_command: bool = False) -> None:
        self.connection = CatalogConnection(
            late_transcript=late_transcript, old_command=old_command
        )

    @property
    def model_name(self) -> str:
        return "test-voice"

    @property
    def system(self) -> str:
        return "test"

    @property
    def profile(self) -> RealtimeModelProfile:
        return RealtimeModelProfile(
            supports_interruption=True,
            supports_manual_turn_control=True,
            audio_input_sample_rate=24000,
            audio_output_sample_rate=24000,
        )

    @asynccontextmanager
    async def connect(
        self,
        *,
        messages: Sequence[ModelMessage],
        model_settings: RealtimeModelSettings | None,
        model_request_parameters: ModelRequestParameters,
    ) -> AsyncGenerator[RealtimeConnection]:
        try:
            yield self.connection
        finally:
            self.connection.closed = True


class Browser:
    def __init__(self, *, end_on_completion: bool = False) -> None:
        self.end_on_completion = end_on_completion
        self.input: asyncio.Queue[VoiceCommand | bytes] = asyncio.Queue()
        self.events: list[VoiceEvent | bytes] = []

    async def receive(self) -> VoiceCommand | bytes:
        return await self.input.get()

    async def send(self, event: VoiceEvent | bytes) -> None:
        self.events.append(event)
        if isinstance(event, VoiceEvent) and event.type == "ready":
            await self.input.put(b"\x00" * 4800)
        if (
            isinstance(event, VoiceEvent)
            and event.type == "status"
            and event.status == "listening"
            and any(isinstance(item, bytes) for item in self.events)
            and (
                self.end_on_completion
                or any(
                    isinstance(item, VoiceEvent) and item.type == "ui-action"
                    for item in self.events
                )
            )
        ):
            await self.input.put(VoiceCommand(type="end"))
        if (
            isinstance(event, VoiceEvent)
            and event.type == "ui-action"
            and any(
                isinstance(item, VoiceEvent) and item.type == "reply-complete"
                for item in self.events
            )
            and any(isinstance(item, bytes) for item in self.events)
        ):
            await self.input.put(VoiceCommand(type="end"))


@pytest.mark.asyncio
@pytest.mark.parametrize("late_transcript", [False, True])
@pytest.mark.parametrize(
    ("message", "expected"),
    [
        ("Find Forrest Gump", "show_search_results"),
        ("Open my settings", "open_page"),
        ("please open my ratings list", "open_page"),
        ("Show me my rated movies", "open_page"),
        ("Don't open my settings", None),
    ],
)
async def test_navigation_uses_final_current_speech(
    message: str,
    expected: str | None,
    late_transcript: bool,
) -> None:
    class NavigationConnection(CatalogConnection):
        async def send(self, content: RealtimeInput) -> None:
            if isinstance(content, BinaryAudio) and expected == "open_page":
                await self.events.put(RealtimeInputSpeechStartEvent(item_id="user-1"))
                if not self.late_transcript:
                    await self.transcript()
                await self.events.put(OutputTranscript("Opening your page."))
                await self.events.put(AudioDelta(b"\x00\x01" * 2400))
                await self.events.put(ResponseDone())
                if self.late_transcript:
                    await self.transcript()
            else:
                await super().send(content)

        async def transcript(self) -> None:
            await self.events.put(InputTranscript(message, is_final=True, item_id="user-1"))

    model = CatalogModel()
    model.connection = NavigationConnection(late_transcript=late_transcript)

    # A late transcript comes after the provider completes its reply; end only once processed.
    class NavigationBrowser(Browser):
        async def send(self, event: VoiceEvent | bytes) -> None:
            self.events.append(event)
            if isinstance(event, VoiceEvent) and event.type == "ready":
                await self.input.put(b"\x00" * 4800)
            if (
                isinstance(event, VoiceEvent)
                and event.type == "transcript"
                and event.speaker == "user"
                and late_transcript
            ):
                await self.input.put(VoiceCommand(type="end"))
            if (
                isinstance(event, VoiceEvent)
                and event.type == "reply-complete"
                and not late_transcript
            ):
                await self.input.put(VoiceCommand(type="end"))

    browser = NavigationBrowser()
    agent: Agent[None, str] = Agent()

    def search_movies(query: str) -> dict[str, object]:
        return {"schemaVersion": "1.0", "movies": [], "totalMatches": 0, "moreAvailable": False}

    agent.tool_plain(search_movies)
    async with asyncio.timeout(3):
        await relay_voice(agent, model, browser, authenticated=True)
    actions = [
        event.action
        for event in browser.events
        if isinstance(event, VoiceEvent) and event.type == "ui-action"
    ]
    assert [action.type for action in actions if action] == ([expected] if expected else [])
    if expected == "open_page":
        assert not model.connection.tool_called
        assert actions[0] is not None and actions[0].type == "open_page"
        assert actions[0].destination == ("settings" if "settings" in message else "ratings")
    if expected == "show_search_results":
        assert actions[0] is not None and actions[0].type == "show_search_results"
        assert actions[0].query == "Forrest Gump"


@pytest.mark.asyncio
async def test_first_audio_reaches_browser_before_provider_finishes_reply() -> None:
    delivered = asyncio.Event()

    class StreamingConnection(CatalogConnection):
        async def __aiter__(self) -> AsyncIterator[RealtimeCodecEvent]:
            async for event in super().__aiter__():
                yield event
                if isinstance(event, RealtimeInputSpeechStartEvent):
                    yield RealtimeInputSpeechEndEvent(item_id=event.item_id)
                if isinstance(event, AudioDelta):
                    # Provider cannot finish until the browser receives the very first chunk.
                    await delivered.wait()
                    yield AudioDelta(b"\x01\x02" * 2400)

    class StreamingBrowser(Browser):
        async def send(self, event: VoiceEvent | bytes) -> None:
            await super().send(event)
            if isinstance(event, bytes):
                delivered.set()

    model = CatalogModel()
    model.connection = StreamingConnection()
    browser = StreamingBrowser(end_on_completion=True)
    agent: Agent[None, str] = Agent()

    def search_movies(query: str) -> dict[str, object]:
        return {"schemaVersion": "1.0", "movies": [], "totalMatches": 0, "moreAvailable": False}

    agent.tool_plain(search_movies)
    with capture_logs() as logs:
        async with asyncio.timeout(3):
            await relay_voice(agent, model, browser)
    assert [event for event in browser.events if isinstance(event, bytes)] == [
        b"\x00\x01" * 2400,
        b"\x01\x02" * 2400,
    ]
    audio_logs = [event for event in logs if event["event"] == "voice_first_audio"]
    assert len(audio_logs) == 1
    assert audio_logs[0]["outcome"] == "first_turn"
    assert any(event["event"] == "voice_tool_completed" for event in logs)


@pytest.mark.asyncio
@pytest.mark.parametrize(
    ("late_transcript", "old_command"), [(False, False), (True, False), (False, True)]
)
async def test_realtime_tool_audio_navigation_and_cleanup(
    late_transcript: bool, old_command: bool
) -> None:
    model = CatalogModel(late_transcript=late_transcript, old_command=old_command)
    browser = Browser(end_on_completion=old_command)
    agent: Agent[None, str] = Agent()

    def search_movies(query: str) -> dict[str, object]:
        assert query == "Forrest Gump"
        return {
            "schemaVersion": "1.0",
            "movies": [
                {
                    "movieId": 42,
                    "primaryTitle": "Forrest Gump",
                    "type": "MOVIE",
                    "runtimeMinutes": 142,
                }
            ],
            "totalMatches": 1,
            "moreAvailable": False,
        }

    agent.tool_plain(search_movies)
    async with asyncio.timeout(3):
        await relay_voice(agent, model, browser)
    assert model.connection.closed
    assert model.connection.tool_called
    assert any(isinstance(event, bytes) and event for event in browser.events)
    audio_index = max(i for i, event in enumerate(browser.events) if isinstance(event, bytes))
    assert any(
        isinstance(event, VoiceEvent) and event.type == "reply-complete"
        for event in browser.events[audio_index + 1 :]
    )
    actions = [
        event
        for event in browser.events
        if isinstance(event, VoiceEvent) and event.type == "ui-action"
    ]
    if old_command:
        assert not actions
        return
    assert len(actions) == 1
    if not late_transcript:
        assert browser.events.index(actions[0]) < audio_index
    assert (
        actions[0].action is not None
        and actions[0].action.type == "open_movie"
        and actions[0].action.movie_id == 42
    )
    assert browser.events.index(actions[0]) > next(
        i
        for i, event in enumerate(browser.events)
        if isinstance(event, VoiceEvent) and event.type == "movie-card"
    )


class SequentialConnection(CatalogConnection):
    turn = 0

    async def send(self, content: RealtimeInput) -> None:
        if isinstance(content, BinaryAudio):
            self.turn += 1
            title = f"Fixture movie {self.turn}"
            await self.events.put(RealtimeInputSpeechStartEvent(item_id=f"user-{self.turn}"))
            await self.events.put(
                InputTranscript(f"Open {title}", is_final=True, item_id=f"user-{self.turn}")
            )
            await self.events.put(
                ToolCall(
                    f"lookup-{self.turn}",
                    tool_name="search_movies",
                    args=json.dumps({"query": title}),
                )
            )
            await self.events.put(ResponseDone())
        elif isinstance(content, ToolResult):
            await self.events.put(OutputTranscript(f"Here's fixture movie {self.turn}."))
            await self.events.put(AudioDelta(b"\x00\x01" * 2400))
            await self.events.put(ResponseDone())


class SequentialBrowser(Browser):
    def __init__(self, turns: int) -> None:
        super().__init__()
        self.remaining = turns

    async def send(self, event: VoiceEvent | bytes) -> None:
        self.events.append(event)
        if isinstance(event, VoiceEvent):
            if event.type == "ready":
                await self.input.put(b"\x00" * 2400)
            elif event.type == "reply-complete":
                self.remaining -= 1
                await self.input.put(b"\x00" * 2400 if self.remaining else VoiceCommand(type="end"))


@pytest.mark.asyncio
@pytest.mark.parametrize("turns", [10, 17])
async def test_multi_movie_session_and_explicit_usage_limit(turns: int) -> None:
    from imdb_agent.concierge.voice import VoiceSessionLimitError

    model = CatalogModel()
    model.connection = SequentialConnection()
    browser = SequentialBrowser(turns)
    agent: Agent[None, str] = Agent()

    def search_movies(query: str) -> dict[str, object]:
        return {
            "schemaVersion": "1.0",
            "movies": [
                {"movieId": int(query.rsplit(" ", 1)[1]), "primaryTitle": query, "type": "MOVIE"}
            ],
            "totalMatches": 1,
            "moreAvailable": False,
        }

    agent.tool_plain(search_movies)

    async with asyncio.timeout(3):
        if turns == 17:
            with pytest.raises(VoiceSessionLimitError):
                await relay_voice(agent, model, browser)
        else:
            await relay_voice(agent, model, browser)
    actions = [
        event.action.movie_id
        for event in browser.events
        if isinstance(event, VoiceEvent)
        and event.type == "ui-action"
        and event.action
        and event.action.type == "open_movie"
    ]
    assert actions == list(range(1, min(turns, 16) + 1))
    assert model.connection.closed


@pytest.mark.asyncio
@pytest.mark.parametrize("late_transcript", [False, True, None])
@pytest.mark.parametrize("authorized", [False, True])
async def test_model_interpreted_rating_uses_grounded_id_and_committed_receipt(
    late_transcript: bool | None,
    authorized: bool,
) -> None:
    from typing import Any, cast

    from pydantic import SecretStr
    from pydantic_ai import RunContext

    from imdb_agent.adapters.personal_tools import PersonalToolGate
    from imdb_agent.concierge.events import GroundedMovie
    from imdb_agent.concierge.personal import PersonalTurn

    class RejectedConnection(CatalogConnection):
        async def send(self, content: RealtimeInput) -> None:
            if isinstance(content, BinaryAudio):
                await self.events.put(RealtimeInputSpeechStartEvent(item_id="user-1"))
                if late_transcript is False:
                    await self.transcript()
                await self.events.put(
                    ToolCall(
                        "write",
                        tool_name="set_my_movie_rating",
                        args=json.dumps({"movieId": 6 if authorized else 999, "score": 7.0}),
                    )
                )
                await self.events.put(ResponseDone())
                if late_transcript:
                    await self.transcript()
            else:
                await super().send(content)

        async def transcript(self) -> None:
            await self.events.put(
                InputTranscript(
                    "For me, Amelie deserves a seven.",
                    is_final=True,
                    item_id="user-1",
                )
            )

    model = CatalogModel()
    model.connection = RejectedConnection()
    state = PersonalTurn(
        movies=(
            GroundedMovie(
                movie_id=6,
                primary_title="Amélie",
                movie_type="MOVIE",
            ),
        )
    )
    browser = Browser(end_on_completion=True)
    agent: Agent[None, str] = Agent(retries=0)
    gate = PersonalToolGate(SecretStr("synthetic-session"), state)

    async def backend(
        name: str,
        args: dict[str, Any],
        *,
        metadata: dict[str, Any] | None = None,
    ) -> dict[str, Any]:
        assert authorized, "Rejected command must never reach Java"
        assert metadata is not None
        return {
            "contractVersion": "1.0",
            "operationId": metadata["operationId"],
            "movieId": args["movieId"],
            "score": args["score"],
            "previousScore": None,
            "kind": "rating_set",
            "changed": True,
        }

    async def set_my_movie_rating(movieId: int, score: float) -> object:
        return await gate.call(
            cast("RunContext[Any]", None),
            backend,
            "set_my_movie_rating",
            {"movieId": movieId, "score": score},
        )

    agent.tool_plain(set_my_movie_rating)
    async with asyncio.timeout(3):
        await relay_voice(agent, model, browser, personal=state, authenticated=True)
    assert model.connection.tool_called
    assert any(isinstance(item, bytes) for item in browser.events)
    actions = [
        item.action
        for item in browser.events
        if isinstance(item, VoiceEvent) and item.type == "ui-action"
    ]
    if authorized:
        assert state.receipt is not None
        assert len(actions) == 1
        assert actions[0] is not None and actions[0].type == "open_ratings"
        assert actions[0].score == 7.0
    else:
        assert not actions
        assert state.receipt is None


@pytest.mark.asyncio
@pytest.mark.parametrize("late_transcript", [False, True])
@pytest.mark.parametrize("followup", ["search_movies", "get_similar_movies", "failed"])
async def test_voice_search_waits_for_final_discovery_result(
    followup: str, late_transcript: bool
) -> None:
    class DiscoveryConnection(CatalogConnection):
        step = 0

        async def transcript(self) -> None:
            await self.events.put(
                InputTranscript(
                    "Find movies similar to Forrest Gump"
                    if followup == "get_similar_movies"
                    else "Find Forrest Gump",
                    is_final=True,
                    item_id="user-1",
                )
            )

        async def send(self, content: RealtimeInput) -> None:
            if isinstance(content, ToolResult):
                self.step += 1
                if self.step == 1:
                    assert not any(
                        isinstance(e, VoiceEvent) and e.type == "ui-action" for e in browser.events
                    )
                    name = (
                        "get_similar_movies"
                        if followup == "get_similar_movies"
                        else "search_movies"
                    )
                    await self.events.put(
                        ToolCall(
                            "refinement",
                            tool_name=name,
                            args=json.dumps(
                                {"movieId": 42}
                                if name == "get_similar_movies"
                                else {"query": "Forrest Gump 1994"}
                            ),
                        )
                    )
                    await self.events.put(ResponseDone())
                    return
            await super().send(content)

    model = CatalogModel()
    model.connection = DiscoveryConnection(late_transcript=late_transcript)
    agent: Agent[None, str] = Agent()

    def search_movies(query: str) -> dict[str, object]:
        if query.endswith("1994") and followup == "failed":
            raise ToolFailed("Catalog unavailable")
        return {"schemaVersion": "1.0", "movies": [], "totalMatches": 0, "moreAvailable": False}

    def get_similar_movies(movieId: int) -> dict[str, object]:
        return {"schemaVersion": "1.0", "movies": [], "strategy": "SIMILAR"}

    agent.tool_plain(search_movies)
    agent.tool_plain(get_similar_movies)

    # When a late final transcript produces no action, end after that final transcript.
    class DiscoveryBrowser(Browser):
        async def send(self, event: VoiceEvent | bytes) -> None:
            await super().send(event)
            if (
                isinstance(event, VoiceEvent)
                and event.type == "transcript"
                and event.speaker == "user"
                and late_transcript
            ):
                await self.input.put(VoiceCommand(type="end"))

    browser = DiscoveryBrowser(end_on_completion=not late_transcript)
    async with asyncio.timeout(3):
        await relay_voice(agent, model, browser)
    actions = [
        event.action
        for event in browser.events
        if isinstance(event, VoiceEvent) and event.type == "ui-action"
    ]
    if followup == "search_movies":
        assert (
            len(actions) == 1
            and actions[0] is not None
            and actions[0].type == "show_search_results"
        )
        assert actions[0].query == "Forrest Gump 1994"
    else:
        assert not actions


@pytest.mark.asyncio
@pytest.mark.parametrize("late_transcript", [False, True])
@pytest.mark.parametrize(
    "tool, action_type",
    [
        ("navigate_app", "open_page"),
        ("open_movie_page", "open_movie"),
        ("open_movie_trailer", "open_movie_trailer"),
    ],
)
async def test_semantic_navigation_tools_reach_browser(
    late_transcript: bool, tool: str, action_type: str
) -> None:
    from imdb_agent.concierge.events import GroundedMovie
    from imdb_agent.concierge.personal import PersonalTurn

    movie_page = tool != "navigate_app"

    class SemanticConnection(CatalogConnection):
        async def send(self, content: RealtimeInput) -> None:
            if isinstance(content, BinaryAudio):
                await self.events.put(RealtimeInputSpeechStartEvent(item_id="user-1"))
                if not late_transcript:
                    await self.transcript()
                await self.events.put(
                    ToolCall(
                        "navigate",
                        tool_name=tool,
                        args='{"movie_id":6}' if movie_page else '{"destination":"ratings"}',
                    )
                )
                await self.events.put(ResponseDone())
                if late_transcript:
                    await self.transcript()
            elif isinstance(content, ToolResult):
                await self.events.put(OutputTranscript("Here you go."))
                await self.events.put(AudioDelta(b"\x00\x01" * 2400))
                await self.events.put(ResponseDone())

        async def transcript(self) -> None:
            await self.events.put(
                InputTranscript(
                    (
                        "Let me watch its trailer"
                        if tool == "open_movie_trailer"
                        else "Let's have a look at that one"
                    )
                    if movie_page
                    else "My rated movies, please",
                    is_final=True,
                    item_id="user-1",
                )
            )

    personal = PersonalTurn(
        movies=(GroundedMovie(movie_id=6, primary_title="Forrest Gump", movie_type="MOVIE"),)
    )
    model = CatalogModel()
    model.connection = SemanticConnection()
    browser = Browser()
    agent: Agent[None, str] = Agent()
    async with asyncio.timeout(3):
        await relay_voice(agent, model, browser, personal=personal, authenticated=True)
    actions = [
        event.action
        for event in browser.events
        if isinstance(event, VoiceEvent) and event.type == "ui-action"
    ]
    assert len(actions) == 1
    assert actions[0] is not None
    assert actions[0].type == action_type
    if movie_page:
        index = next(
            i
            for i, event in enumerate(browser.events)
            if isinstance(event, VoiceEvent) and event.type == "ui-action"
        )
        previous = browser.events[index - 1]
        assert isinstance(previous, VoiceEvent) and previous.type == "movie-card"


@pytest.mark.asyncio
async def test_voice_context_tool_reads_the_latest_browser_snapshot_without_navigation() -> None:
    from imdb_agent.concierge.page_context import PageContext

    class ContextConnection(CatalogConnection):
        output: str | None = None

        async def send(self, content: RealtimeInput) -> None:
            if isinstance(content, BinaryAudio):
                await self.events.put(RealtimeInputSpeechStartEvent(item_id="page-question"))
                await self.events.put(
                    InputTranscript(
                        "What can I do on this page?", is_final=True, item_id="page-question"
                    )
                )
                await self.events.put(
                    ToolCall("page-context", tool_name="get_page_context", args="{}")
                )
                await self.events.put(ResponseDone())
            elif isinstance(content, ToolResult):
                self.output = content.output
                await self.events.put(
                    OutputTranscript("You can read the synopsis and play a trailer.")
                )
                await self.events.put(AudioDelta(b"\x00\x01" * 2400))
                await self.events.put(ResponseDone())

    browser = Browser(end_on_completion=True)
    from imdb_agent.concierge.streaming import StreamingRegion

    region = StreamingRegion()
    for movie_id in (6, 7):
        browser.input.put_nowait(
            VoiceCommand(
                type="context",
                context=PageContext(page="movie", movie_id=movie_id, streaming_country="DE"),
            )
        )
    model = CatalogModel()
    connection = ContextConnection()
    model.connection = connection
    agent: Agent[None, str] = Agent()
    async with asyncio.timeout(3):
        await relay_voice(agent, model, browser, streaming_region=region)
    assert region.country == "DE"
    assert connection.output is not None
    assert json.loads(connection.output)["context"]["movieId"] == 7
    assert not any(
        isinstance(event, VoiceEvent) and event.type == "ui-action" for event in browser.events
    )


@pytest.mark.asyncio
async def test_voice_external_facts_keep_movie_context_and_do_not_navigate() -> None:
    from imdb_agent.concierge.events import GroundedMovie
    from imdb_agent.concierge.personal import PersonalTurn

    class EnrichmentConnection(CatalogConnection):
        async def send(self, content: RealtimeInput) -> None:
            if isinstance(content, BinaryAudio):
                await self.events.put(RealtimeInputSpeechStartEvent(item_id="extra"))
                await self.events.put(
                    InputTranscript("Who directed this movie?", is_final=True, item_id="extra")
                )
                await self.events.put(
                    ToolCall("extra", tool_name="get_movie_enrichment", args='{"movieId":6}')
                )
                await self.events.put(ResponseDone())
            elif isinstance(content, ToolResult):
                await self.events.put(OutputTranscript("Extra TMDB information is unavailable."))
                await self.events.put(AudioDelta(b"\x00\x01" * 2400))
                await self.events.put(ResponseDone())

    browser = Browser(end_on_completion=True)
    model = CatalogModel()
    model.connection = EnrichmentConnection()
    agent: Agent[None, str] = Agent()

    def get_movie_enrichment(movieId: int) -> dict[str, object]:
        assert movieId == 6
        return {
            "contractVersion": "1.0",
            "movieId": 6,
            "outcome": "UNAVAILABLE",
            "source": "TMDB",
            "sourceUrl": None,
            "fetchedAt": None,
            "facts": None,
        }

    agent.tool_plain(get_movie_enrichment)
    movie = GroundedMovie(movie_id=6, primary_title="Forrest Gump", movie_type="MOVIE")
    personal = PersonalTurn(movies=(movie,))
    async with asyncio.timeout(3):
        await relay_voice(agent, model, browser, personal=personal)
    assert personal.movies == (movie,)
    assert not any(
        isinstance(event, VoiceEvent) and event.type in {"movie-card", "ui-action"}
        for event in browser.events
    )
    assert any(isinstance(event, bytes) for event in browser.events)


@pytest.mark.asyncio
async def test_voice_watch_providers_keep_movie_context_and_do_not_navigate() -> None:
    from imdb_agent.concierge.events import GroundedMovie
    from imdb_agent.concierge.personal import PersonalTurn

    class EnrichmentConnection(CatalogConnection):
        async def send(self, content: RealtimeInput) -> None:
            if isinstance(content, BinaryAudio):
                await self.events.put(RealtimeInputSpeechStartEvent(item_id="extra"))
                await self.events.put(
                    InputTranscript(
                        "Where can I stream this movie?", is_final=True, item_id="extra"
                    )
                )
                await self.events.put(
                    ToolCall("extra", tool_name="get_movie_watch_providers", args='{"movieId":6}')
                )
                await self.events.put(ResponseDone())
            elif isinstance(content, ToolResult):
                await self.events.put(OutputTranscript("Extra TMDB information is unavailable."))
                await self.events.put(AudioDelta(b"\x00\x01" * 2400))
                await self.events.put(ResponseDone())

    browser = Browser(end_on_completion=True)
    model = CatalogModel()
    model.connection = EnrichmentConnection()
    agent: Agent[None, str] = Agent()

    def get_movie_watch_providers(movieId: int) -> dict[str, object]:
        assert movieId == 6
        return {
            "contractVersion": "1.0",
            "movieId": 6,
            "outcome": "UNAVAILABLE",
            "source": "JUSTWATCH_VIA_TMDB",
            "country": "CH",
            "sourceUrl": None,
            "fetchedAt": None,
            "offers": None,
        }

    agent.tool_plain(get_movie_watch_providers)
    movie = GroundedMovie(movie_id=6, primary_title="Forrest Gump", movie_type="MOVIE")
    personal = PersonalTurn(movies=(movie,))
    async with asyncio.timeout(3):
        await relay_voice(agent, model, browser, personal=personal)
    assert personal.movies == (movie,)
    assert not any(
        isinstance(event, VoiceEvent) and event.type in {"movie-card", "ui-action"}
        for event in browser.events
    )
    assert any(isinstance(event, bytes) for event in browser.events)


@pytest.mark.asyncio
async def test_rejected_write_loop_stops_and_next_spoken_removal_still_works() -> None:
    from typing import Any, cast

    from pydantic import SecretStr
    from pydantic_ai import RunContext
    from pydantic_ai.realtime.codec import CancelResponse

    from imdb_agent.adapters.personal_tools import PersonalToolGate
    from imdb_agent.concierge.events import GroundedMovie
    from imdb_agent.concierge.personal import PersonalTurn

    class LoopConnection(CatalogConnection):
        turn = 0
        attempts = 0
        stopped = False

        async def request_removal(self) -> None:
            self.attempts += 1
            await self.events.put(
                ToolCall(
                    f"remove-{self.attempts}",
                    tool_name="remove_movie_from_my_watchlist",
                    args=json.dumps({"movieId": 999 if self.turn == 1 else 9}),
                )
            )
            await self.events.put(ResponseDone())

        async def send(self, content: RealtimeInput) -> None:
            if isinstance(content, BinaryAudio):
                self.turn += 1
                self.stopped = False
                await self.events.put(RealtimeInputSpeechStartEvent(item_id=f"user-{self.turn}"))
                await self.events.put(
                    InputTranscript(
                        "Remove that movie from my watchlist"
                        if self.turn == 1
                        else "And now remove the movie Good Will Hunting from my watchlist.",
                        is_final=True,
                        item_id=f"user-{self.turn}",
                    )
                )
                await self.request_removal()
            elif isinstance(content, CancelResponse):
                self.stopped = True
                await self.events.put(ResponseDone(interrupted=True))
            elif isinstance(content, ToolResult):
                if self.turn == 1 and not self.stopped:
                    await self.request_removal()
                elif self.turn == 2:
                    await self.events.put(OutputTranscript("Removed from your watchlist."))
                    await self.events.put(AudioDelta(b"\x00\x01" * 2400))
                    await self.events.put(ResponseDone())

    class RecoveryBrowser(Browser):
        async def send(self, event: VoiceEvent | bytes) -> None:
            await super().send(event)
            if isinstance(event, VoiceEvent) and event.type == "status" and event.text:
                await self.input.put(b"\x00" * 4800)

    state = PersonalTurn(
        movies=(
            GroundedMovie(
                movie_id=9,
                primary_title="Good Will Hunting",
                movie_type="MOVIE",
            ),
        )
    )
    gate = PersonalToolGate(SecretStr("synthetic-session"), state)
    writes: list[int] = []

    async def backend(
        name: str, args: dict[str, Any], *, metadata: dict[str, Any] | None = None
    ) -> dict[str, Any]:
        assert metadata is not None
        writes.append(args["movieId"])
        return {
            "contractVersion": "1.0",
            "operationId": metadata["operationId"],
            "movieId": 9,
            "kind": "watchlist_remove",
            "changed": True,
        }

    agent: Agent[None, str] = Agent(retries=0)

    async def remove_movie_from_my_watchlist(movieId: int) -> object:
        return await gate.call(
            cast("RunContext[Any]", None),
            backend,
            "remove_movie_from_my_watchlist",
            {"movieId": movieId},
        )

    model = CatalogModel()
    agent.tool_plain(remove_movie_from_my_watchlist)
    connection = LoopConnection()
    model.connection = connection
    browser = RecoveryBrowser()
    async with asyncio.timeout(3):
        await relay_voice(agent, model, browser, personal=state, authenticated=True)
    assert writes == [9]
    assert connection.attempts <= 4
    notices = [
        e for e in browser.events if isinstance(e, VoiceEvent) and e.type == "status" and e.text
    ]
    assert len(notices) == 1
    assert not any(isinstance(e, VoiceEvent) and e.type == "error" for e in browser.events)
    actions = [
        e.action for e in browser.events if isinstance(e, VoiceEvent) and e.type == "ui-action"
    ]
    assert len(actions) == 1
    assert actions[0] is not None and actions[0].type == "open_watchlist"
    assert actions[0].removed is True


@pytest.mark.asyncio
@pytest.mark.parametrize("spoken_first", [True, False])
async def test_typed_followup_keeps_voice_grounding_and_answers_aloud(spoken_first: bool) -> None:
    class TextConnection(CatalogConnection):
        def __init__(self) -> None:
            super().__init__()
            self.texts: list[str] = []

        async def transcript(self) -> None:
            await self.events.put(
                InputTranscript("Tell me about Forrest Gump", is_final=True, item_id="user-1")
            )

        async def send(self, content: RealtimeInput) -> None:
            if isinstance(content, str):
                self.texts.append(content)
                if content == "Tell me about Forrest Gump":
                    await self.events.put(
                        ToolCall(
                            "lookup", tool_name="search_movies", args='{"query":"Forrest Gump"}'
                        )
                    )
                else:
                    # A delayed microphone transcript may not replace the typed follow-up.
                    await self.events.put(
                        InputTranscript("Do not open anything", is_final=True, item_id="old-audio")
                    )
                    await self.events.put(OutputTranscript("Here's Forrest Gump."))
                    await self.events.put(AudioDelta(b"\x00\x01" * 2400))
                await self.events.put(ResponseDone())
            else:
                await super().send(content)

    class TextBrowser(Browser):
        async def send(self, event: VoiceEvent | bytes) -> None:
            self.events.append(event)
            if isinstance(event, VoiceEvent):
                if event.type == "ready":
                    await self.input.put(
                        b"\x00" * 4800
                        if spoken_first
                        else VoiceCommand(type="text", text="Tell me about Forrest Gump")
                    )
                if event.type == "reply-complete":
                    await self.input.put(
                        VoiceCommand(type="text", text="Open it")
                        if event.turn == 1
                        else VoiceCommand(type="end")
                    )

    model = CatalogModel()
    connection = TextConnection()
    model.connection = connection
    browser = TextBrowser()
    agent: Agent[None, str] = Agent()

    def search_movies(query: str) -> dict[str, object]:
        return {
            "schemaVersion": "1.0",
            "movies": [{"movieId": 42, "primaryTitle": "Forrest Gump", "type": "MOVIE"}],
            "totalMatches": 1,
            "moreAvailable": False,
        }

    agent.tool_plain(search_movies)
    async with asyncio.timeout(3):
        await relay_voice(agent, model, browser)
    assert connection.texts == (
        ["Open it"] if spoken_first else ["Tell me about Forrest Gump", "Open it"]
    )
    transcripts = [
        (event.turn, event.text)
        for event in browser.events
        if isinstance(event, VoiceEvent) and event.type == "transcript" and event.speaker == "user"
    ]
    assert transcripts == [(1, "Tell me about Forrest Gump"), (2, "Open it")]
    actions = [
        event
        for event in browser.events
        if isinstance(event, VoiceEvent) and event.type == "ui-action"
    ]
    assert len(actions) == 1 and actions[0].turn == 2
    assert actions[0].action is not None and actions[0].action.type == "open_movie"
    assert actions[0].action.movie_id == 42
    assert sum(isinstance(event, bytes) for event in browser.events) == 2
    assert connection.closed


@pytest.mark.asyncio
async def test_typed_input_interrupts_a_spoken_reply_without_unmuting() -> None:
    from pydantic_ai.realtime.codec import CancelResponse, ClearAudio

    class InterruptConnection(CatalogConnection):
        def __init__(self) -> None:
            super().__init__()
            self.cancelled = 0
            self.cleared = 0
            self.requests: list[str] = []

        async def send(self, content: RealtimeInput) -> None:
            if isinstance(content, CancelResponse):
                self.cancelled += 1
                await self.events.put(ResponseDone(interrupted=True))
            elif isinstance(content, ClearAudio):
                self.cleared += 1
            elif isinstance(content, str):
                self.requests.append(content)
                await self.events.put(OutputTranscript("Here is an answer."))
                await self.events.put(AudioDelta(b"\x00\x01" * 2400))
                if len(self.requests) == 2:
                    await self.events.put(ResponseDone())

    class InterruptBrowser(Browser):
        second_sent = False

        async def send(self, event: VoiceEvent | bytes) -> None:
            self.events.append(event)
            if isinstance(event, VoiceEvent) and event.type == "ready":
                await self.input.put(VoiceCommand(type="mute"))
                await self.input.put(VoiceCommand(type="text", text="Tell me about Forrest Gump"))
            elif isinstance(event, bytes) and not self.second_sent:
                self.second_sent = True
                await self.input.put(VoiceCommand(type="text", text="What about its director?"))
            elif isinstance(event, VoiceEvent) and event.type == "reply-complete":
                await self.input.put(VoiceCommand(type="end"))

    model = CatalogModel()
    connection = InterruptConnection()
    model.connection = connection
    browser = InterruptBrowser()
    agent: Agent[None, str] = Agent()
    async with asyncio.timeout(3):
        await relay_voice(agent, model, browser)
    assert connection.requests == ["Tell me about Forrest Gump", "What about its director?"]
    assert connection.cancelled == 1
    assert connection.cleared == 3
    completed = [
        event.turn
        for event in browser.events
        if isinstance(event, VoiceEvent) and event.type == "reply-complete"
    ]
    assert completed == [2]
    statuses = [
        event.status
        for event in browser.events
        if isinstance(event, VoiceEvent) and event.type == "status"
    ]
    assert statuses[-1] == "muted"


@pytest.mark.asyncio
async def test_idle_deadline_raises_a_distinct_session_reason(
    monkeypatch: pytest.MonkeyPatch,
) -> None:
    from imdb_agent.concierge.voice import VoiceIdleTimeoutError

    real_timeout = asyncio.timeout

    def short_idle_timeout(delay: float | None) -> asyncio.Timeout:
        return real_timeout(0.01 if delay == 45 else delay)

    monkeypatch.setattr(asyncio, "timeout", short_idle_timeout)

    class SilentBrowser(Browser):
        async def send(self, event: VoiceEvent | bytes) -> None:
            self.events.append(event)

    model = CatalogModel()
    browser = SilentBrowser()
    agent: Agent[None, str] = Agent()
    async with real_timeout(3):
        with pytest.raises(VoiceIdleTimeoutError):
            await relay_voice(agent, model, browser)
    assert model.connection.closed


@pytest.mark.asyncio
async def test_spoken_preamble_survives_tool_call_and_final_answer() -> None:
    class PreambleConnection(CatalogConnection):
        async def transcript(self) -> None:
            await super().transcript()
            await self.events.put(OutputTranscript("Let me check the movie."))

    model = CatalogModel()
    model.connection = PreambleConnection()
    browser = Browser(end_on_completion=True)
    agent: Agent[None, str] = Agent()

    def search_movies(query: str) -> dict[str, object]:
        return {
            "schemaVersion": "1.0",
            "movies": [{"movieId": 42, "primaryTitle": "Forrest Gump", "type": "MOVIE"}],
            "totalMatches": 1,
            "moreAvailable": False,
        }

    agent.tool_plain(search_movies)
    async with asyncio.timeout(3):
        await relay_voice(agent, model, browser)
    transcripts = [
        event.text
        for event in browser.events
        if isinstance(event, VoiceEvent)
        and event.type == "transcript"
        and event.speaker == "assistant"
        and event.final
    ]
    assert transcripts[-1] == ("Let me check the movie.\n\nForrest Gump runs for 142 minutes.")
    activities = [
        event.activity.model_dump(mode="json")
        for event in browser.events
        if isinstance(event, VoiceEvent) and event.activity is not None
    ]
    assert activities == [
        {"callId": "lookup", "tool": "search_movies", "status": "started"},
        {"callId": "lookup", "tool": "search_movies", "status": "completed"},
    ]

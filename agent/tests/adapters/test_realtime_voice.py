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
async def test_rejected_personal_command_is_a_failed_result_not_a_disconnected_session() -> None:
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
                await self.events.put(
                    InputTranscript("I like Forrest Gump", is_final=True, item_id="user-1")
                )
                await self.events.put(
                    ToolCall(
                        "write",
                        tool_name="set_my_movie_rating",
                        args=json.dumps({"movieId": 6, "score": 8.5}),
                    )
                )
                await self.events.put(ResponseDone())
            else:
                await super().send(content)

    model = CatalogModel()
    model.connection = RejectedConnection()
    state = PersonalTurn(
        movies=(
            GroundedMovie(
                movie_id=6,
                primary_title="Forrest Gump",
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
        pytest.fail("Rejected command must never reach Java")

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
    assert not any(
        isinstance(item, VoiceEvent) and item.type == "ui-action" for item in browser.events
    )
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

from __future__ import annotations

import asyncio
import base64
import json
from time import monotonic
from typing import TYPE_CHECKING, Literal, cast

import pytest
from pydantic import SecretStr

from imdb_agent.adapters.live_voice import LiveEvent, LiveSession, LiveTask
from imdb_agent.adapters.logging import configure_logging
from imdb_agent.concierge.events import (
    GroundedMovie,
    MovieCardEvent,
    OpenMovieAction,
    OpenMovieTrailerAction,
    OpenPageAction,
    TextEvent,
    ToolActivity,
    ToolActivityEvent,
    UiActionEvent,
)
from imdb_agent.concierge.personal import DelegationRejectedError
from imdb_agent.concierge.service import ConciergeRunError
from imdb_agent.concierge.tools import ToolName
from imdb_agent.concierge.voice import VoiceCommand, VoiceEvent

if TYPE_CHECKING:
    from collections.abc import AsyncIterator, Mapping

    from websockets.asyncio.client import ClientConnection

    from imdb_agent.concierge.events import RunnerEvent
    from imdb_agent.concierge.ports import RunRequest


class Browser:
    def __init__(self) -> None:
        self.input: asyncio.Queue[bytes | VoiceCommand] = asyncio.Queue()
        self.events: list[VoiceEvent | bytes] = []
        self.received = asyncio.Event()

    async def receive(self) -> bytes | VoiceCommand:
        return await self.input.get()

    async def send(self, event: VoiceEvent | bytes) -> None:
        self.events.append(event)
        self.received.set()


class Socket:
    def __init__(self, browser: Browser) -> None:
        self.input: asyncio.Queue[str] = asyncio.Queue()
        self.sent: list[dict[str, object]] = []
        self.browser = browser
        self.audio_received = asyncio.Event()

    def emit(self, value: Mapping[str, object]) -> None:
        self.input.put_nowait(json.dumps(value))

    async def recv(self) -> str:
        return await self.input.get()

    async def close(self) -> None:
        pass

    def __aiter__(self) -> AsyncIterator[str]:
        return self.events()

    async def events(self) -> AsyncIterator[str]:
        while True:
            yield await self.recv()

    async def send(self, value: str) -> None:
        event: dict[str, object] = json.loads(value)
        self.sent.append(event)
        if event["type"] == "session.start":
            self.emit({"type": "session.started"})
        elif event["type"] == "session.input_audio.append":
            self.audio_received.set()
        elif event["type"] == "session.commentary.append":
            self.browser.input.put_nowait(VoiceCommand(type="end"))
        elif event["type"] == "session.close":
            self.emit({"type": "session.closed", "usage": {"seconds": 2.5}})


class Verifier:
    def __init__(self, accepted: bool = True) -> None:
        self.accepted = accepted

    async def verify(self, token: SecretStr) -> str:
        if not self.accepted:
            raise DelegationRejectedError
        return "verified-user"


class Backend:
    def __init__(self, socket: Socket | None = None) -> None:
        self.requests: list[RunRequest] = []
        self.socket = socket

    async def stream(self, request: RunRequest) -> AsyncIterator[RunnerEvent]:
        self.requests.append(request)
        if self.socket:
            await self.socket.browser.input.put(b"\x00" * 960)
            await self.socket.audio_received.wait()
        yield UiActionEvent(action=OpenPageAction(destination="home"))
        yield TextEvent(delta="The homepage is open.")


def session_for(accepted: bool = True) -> tuple[LiveSession, Socket, Browser, Backend]:
    browser = Browser()
    socket = Socket(browser)
    backend = Backend(socket)
    session = LiveSession(
        cast("ClientConnection", socket), browser, backend, Verifier(accepted), None
    )
    return session, socket, browser, backend


@pytest.mark.asyncio
async def test_delegation_keeps_audio_flowing_and_closes_with_usage() -> None:
    session, socket, browser, backend = session_for()

    async def conversation() -> None:
        await browser.received.wait()
        for event in [
            {
                "type": "session.input_transcript.delta",
                "delta": "Open the ",
                "start_ms": 0,
                "end_ms": 300,
            },
            {
                "type": "session.input_transcript.delta",
                "delta": "homepage.",
                "start_ms": 300,
                "end_ms": 600,
            },
            {
                "type": "session.output_audio.delta",
                "delta": base64.b64encode(b"\x00" * 960).decode(),
            },
            {
                "type": "session.delegation.created",
                "delegation": {"id": "opaque-request", "target": "client"},
            },
            {
                "type": "session.delegation.created",
                "delegation": {"id": "opaque-request", "target": "client"},
            },
        ]:
            socket.emit(event)

    async with asyncio.timeout(3):
        await asyncio.gather(session.run(), conversation())
    assert len(backend.requests) == 1
    assert backend.requests[0].message == "Open the homepage."
    assert backend.requests[0].delegation is None
    assert socket.audio_received.is_set()
    assert b"\x00" * 960 in browser.events
    assert any(
        isinstance(event, VoiceEvent) and event.type == "ui-action" for event in browser.events
    )
    assert not any(
        isinstance(event, VoiceEvent) and event.speaker == "assistant" for event in browser.events
    )
    result = next(event for event in socket.sent if event["type"] == "session.commentary.append")
    assert result["delegation_id"] == "opaque-request"
    assert result["content"] == "The homepage is open."
    assert session.closed.is_set() and session.voice_seconds == 2.5


@pytest.mark.asyncio
async def test_small_talk_and_overlapping_captions_do_not_trigger_tools_or_cancel_audio() -> None:
    session, _, browser, backend = session_for()
    for role, delta, start, end in [
        ("user", "Hello ", 100, 300),
        ("assistant", "Hi!", 200, 400),
        ("user", "there", 300, 500),
    ]:
        await session.caption(
            cast("Literal['user', 'assistant']", role),
            LiveEvent(type="transcript", delta=delta, start_ms=start, end_ms=end),
        )
    assert backend.requests == [] and session.requests.empty()
    captions = [event for event in browser.events if isinstance(event, VoiceEvent)]
    assert captions[0].turn == captions[2].turn
    assert captions[2].text == "Hello there"
    assert all(event.type == "transcript" and not event.final for event in captions)
    assert session.fragments[1] == ("assistant", "Hi!", 200, 400)


@pytest.mark.asyncio
async def test_queued_anonymous_task_stays_anonymous_after_login() -> None:
    session, socket, _, backend = session_for()
    backend.socket = None
    await session.caption("user", LiveEvent(type="transcript", delta="Open the homepage."))
    task = LiveTask("earlier", session.delegation, session.context)
    session.delegation = SecretStr("synthetic-verified-login")
    session.last_fragment = monotonic() - 1
    await session.delegate(task)
    await session.delegate(LiveTask("duplicate-intent", None, None))
    assert len(backend.requests) == 1
    assert backend.requests[0].delegation is None
    assert "synthetic-verified-login" not in json.dumps(socket.sent)


@pytest.mark.asyncio
@pytest.mark.parametrize("accepted", [False, True])
async def test_login_verification_does_not_replace_voice_connection(accepted: bool) -> None:
    session, socket, browser, _ = session_for(accepted)
    worker = asyncio.create_task(session.authenticate())
    try:
        session.auth_requests.put_nowait(SecretStr("synthetic-verified-login"))
        async with asyncio.timeout(1):
            await browser.received.wait()
        event = browser.events[-1]
        assert isinstance(event, VoiceEvent)
        assert event.type == ("authenticated" if accepted else "authentication-failed")
        assert (session.delegation is not None) is accepted
        assert all(item["type"] != "session.start" for item in socket.sent)
        assert "synthetic-verified-login" not in json.dumps(socket.sent)
    finally:
        worker.cancel()
        await asyncio.gather(worker, return_exceptions=True)


@pytest.mark.asyncio
async def test_unicode_context_is_bounded_without_losing_characters() -> None:
    session, socket, _, _ = session_for()
    text = "A film about Zürich 🎬. " * 80
    await session.append("thinking", text)
    assert "".join(str(event["content"]) for event in socket.sent) == text
    assert all(len(str(event["content"]).encode()) <= 480 for event in socket.sent)


@pytest.mark.asyncio
async def test_queued_task_does_not_execute_the_next_utterance() -> None:
    session, _, _, backend = session_for()
    backend.socket = None
    await session.caption("user", LiveEvent(type="transcript", delta="Open home", end_ms=100))
    task = LiveTask("earlier", None, None, session.rows["user"])
    await session.caption(
        "user", LiveEvent(type="transcript", delta="Open settings", start_ms=3000, end_ms=4000)
    )
    session.last_fragment = monotonic() - 1
    await session.delegate(task)
    assert backend.requests[0].message == "Open home"


@pytest.mark.asyncio
async def test_separate_delegations_in_one_caption_row_have_distinct_action_groups() -> None:
    session, _, browser, backend = session_for()
    backend.socket = None

    class MovieBackend(Backend):
        async def stream(self, request: RunRequest) -> AsyncIterator[RunnerEvent]:
            self.requests.append(request)
            yield MovieCardEvent(
                movie=GroundedMovie(movie_id=42, primary_title="Forrest Gump", movie_type="MOVIE")
            )
            yield UiActionEvent(
                action=(
                    OpenMovieAction(movie_id=42)
                    if len(self.requests) == 1
                    else OpenMovieTrailerAction(movie_id=42)
                )
            )
            yield TextEvent(delta="Opened.")

    backend = MovieBackend()
    session.backend = backend
    await session.caption(
        "user", LiveEvent(type="transcript", delta="Open Forrest Gump.", end_ms=500)
    )
    first_row = session.rows["user"][0]
    session.last_fragment = monotonic() - 1
    await session.delegate(LiveTask("first", None, None, session.rows["user"]))
    await session.caption(
        "user",
        LiveEvent(type="transcript", delta=" Now play its trailer.", start_ms=700, end_ms=1100),
    )
    assert session.rows["user"][0] == first_row
    session.last_fragment = monotonic() - 1
    second_task = LiveTask("second", None, None, session.rows["user"])
    await session.delegate(second_task)
    await session.delegate(second_task)
    actions = [
        event
        for event in browser.events
        if isinstance(event, VoiceEvent) and event.type == "ui-action"
    ]
    assert len(backend.requests) == len(actions) == 2
    assert actions[0].turn != actions[1].turn
    cards = [
        event
        for event in browser.events
        if isinstance(event, VoiceEvent) and event.type == "movie-card"
    ]
    assert [card.turn for card in cards] == [action.turn for action in actions]


@pytest.mark.asyncio
async def test_text_only_delegation_logs_context_sizes_without_claiming_an_action(
    capsys: pytest.CaptureFixture[str],
) -> None:
    class TextBackend(Backend):
        async def stream(self, request: RunRequest) -> AsyncIterator[RunnerEvent]:
            yield TextEvent(delta="Which movie?")

    configure_logging(json_output=True)
    session, _, _, _ = session_for()
    session.backend = TextBackend()
    await session.caption("user", LiveEvent(type="transcript", delta="private user request"))
    session.last_fragment = monotonic() - 1
    await session.delegate(LiveTask("private-delegation", None, None, session.rows["user"]))
    raw = capsys.readouterr().out
    assert "private" not in raw
    events = [json.loads(line) for line in raw.splitlines()]
    context = next(event for event in events if event["event"] == "live_delegation_context")
    assert context["transcript_chars"] == len("private user request")
    assert context["transcript_age_ms"] >= 1000
    assert context["transcript_row_changed"] is False
    assert context["history_chars"] == 0
    result = next(event for event in events if event["event"] == "live_delegation_finished")
    assert result["outcome"] == "text_only"
    assert result["tool_calls"] == result["ui_actions"] == 0


@pytest.mark.asyncio
async def test_failed_backend_is_logged_with_tool_progress_without_payload(
    capsys: pytest.CaptureFixture[str],
) -> None:
    class FailedBackend(Backend):
        async def stream(self, request: RunRequest) -> AsyncIterator[RunnerEvent]:
            yield ToolActivityEvent(
                activity=ToolActivity(
                    call_id="private-call-id", tool=ToolName.SEARCH_MOVIES, status="started"
                )
            )
            raise ConciergeRunError("tool_unavailable", "private provider details", retryable=True)

    configure_logging(json_output=True)
    session, _, _, _ = session_for()
    session.backend = FailedBackend()
    await session.caption("user", LiveEvent(type="transcript", delta="private user request"))
    session.last_fragment = monotonic() - 1
    await session.delegate(LiveTask("private-delegation", None, None))
    raw = capsys.readouterr().out
    assert "private" not in raw
    events = [json.loads(line) for line in raw.splitlines()]
    tool = next(event for event in events if event["event"] == "live_tool_activity")
    assert tool["tool"] == "search_movies" and tool["outcome"] == "started"
    result = next(event for event in events if event["event"] == "live_delegation_finished")
    assert result["outcome"] == "backend_failed"
    assert result["error_code"] == "tool_unavailable"
    assert result["tool_calls"] == 1 and result["ui_actions"] == 0


@pytest.mark.asyncio
@pytest.mark.parametrize("closing_phase", ["usage", "websocket"])
async def test_provider_finalization_drains_browser_audio_after_session_limit(
    closing_phase: str,
) -> None:
    from imdb_agent.concierge.voice import VoiceSessionLimitError

    session, socket, browser, _ = session_for()
    browser.input = asyncio.Queue(maxsize=20)

    class LimitedBackend(Backend):
        async def stream(self, request: RunRequest) -> AsyncIterator[RunnerEvent]:
            raise VoiceSessionLimitError
            yield TextEvent(delta="unreachable")

    session.backend = LimitedBackend()
    closing = asyncio.Event()
    release = asyncio.Event()
    original_send = socket.send

    async def delayed_close(value: str) -> None:
        if closing_phase == "usage" and json.loads(value)["type"] == "session.close":
            closing.set()
            await release.wait()
        await original_send(value)

    socket.send = delayed_close

    async def delayed_websocket_close() -> None:
        if closing_phase == "websocket":
            assert session.closed.is_set()
            closing.set()
            await release.wait()

    socket.close = delayed_websocket_close

    async def produce() -> None:
        await browser.received.wait()
        socket.emit({"type": "session.input_transcript.delta", "delta": "Open home"})
        socket.emit(
            {
                "type": "session.delegation.created",
                "delegation": {
                    "id": "over-limit",
                    "target": "client",
                },
            }
        )
        await closing.wait()
        try:
            for _ in range(30):
                browser.input.put_nowait(b"\x00" * 960)
                await asyncio.sleep(0.005)
        finally:
            release.set()

    async with asyncio.timeout(2):
        producer = asyncio.create_task(produce())
        try:
            with pytest.raises(VoiceSessionLimitError):
                await session.run()
            await producer
        finally:
            producer.cancel()
            await asyncio.gather(producer, return_exceptions=True)
    assert session.closed.is_set()
    assert not any(e["type"] == "session.input_audio.append" for e in socket.sent)


def test_delegation_context_is_bounded_and_preserves_latest_grounded_movie() -> None:
    from imdb_agent.concierge.ports import ConversationMessage

    session, _, _, _ = session_for()
    movie = GroundedMovie(movie_id=42, primary_title="Forrest Gump", movie_type="MOVIE")
    session.history = [
        ConversationMessage(role="assistant", content="Verified result", movies=(movie,))
    ]
    for i in range(80):
        session.fragments.extend(
            [
                ("user", f"Earlier request {i}. " * 10, i * 2000, i * 2000 + 300),
                ("assistant", f"Earlier answer {i}. " * 10, i * 2000 + 400, i * 2000 + 1000),
            ]
        )
    session.fragments.extend(
        [("user", "Play its ", 200000, 200100), ("user", "trailer.", 200100, 200200)]
    )
    history = session.delegation_history("Play its trailer.")
    assert len(history) <= 9
    assert sum(len(m.content) for m in history) <= 5000
    assert all("Earlier request 0." not in m.content for m in history)
    assert all(m.content != "Play its trailer." for m in history)
    assert history
    assert history[-1].movies == (movie,)


@pytest.mark.asyncio
async def test_failed_catalog_delegation_allows_next_request_to_recover() -> None:
    class RecoveringBackend(Backend):
        async def stream(self, request: RunRequest) -> AsyncIterator[RunnerEvent]:
            self.requests.append(request)
            if len(self.requests) == 1:
                raise ConciergeRunError("tool_unavailable", "Catalog unavailable", retryable=True)
            yield TextEvent(delta="Recovered")
            yield UiActionEvent(action=OpenPageAction(destination="home"))

    session, _, browser, _ = session_for()
    backend = RecoveringBackend()
    session.backend = backend
    for i in range(2):
        await session.caption(
            "user",
            LiveEvent(
                type="transcript",
                delta="Open home",
                start_ms=i * 3000,
                end_ms=i * 3000 + 100,
            ),
        )
        session.last_fragment = monotonic() - 1
        await session.delegate(LiveTask(str(i), None, None, session.rows["user"]))
    assert len(backend.requests) == 2
    assert sum(isinstance(e, VoiceEvent) and e.type == "ui-action" for e in browser.events) == 1

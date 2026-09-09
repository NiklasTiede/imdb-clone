"""xAI audio and Java MCP adapter for one bounded, ephemeral browser session."""

from __future__ import annotations

import asyncio
from typing import TYPE_CHECKING

from pydantic_ai import Agent, FunctionToolCallEvent, FunctionToolResultEvent, UsageLimits
from pydantic_ai.exceptions import UsageLimitExceeded
from pydantic_ai.messages import (
    ModelResponse,
    PartDeltaEvent,
    PartEndEvent,
    SpeechPart,
    SpeechPartDelta,
    ToolReturnPart,
)
from pydantic_ai.providers.xai import XaiProvider
from pydantic_ai.realtime import (
    RealtimeInputSpeechEndEvent,
    RealtimeInputSpeechStartEvent,
    RealtimeTurnCompleteEvent,
)
from pydantic_ai.realtime.xai import XaiRealtimeModel, XaiRealtimeModelSettings

from imdb_agent.adapters.catalog_contract import parse_grounded_movies
from imdb_agent.adapters.personal_tools import PersonalToolGate, base_toolset, personal_policy
from imdb_agent.adapters.voice_timing import VoiceTiming
from imdb_agent.adapters.voice_transcripts import CorrelatedVoiceModel
from imdb_agent.concierge.events import OpenLoginAction, OpenWatchlistAction
from imdb_agent.concierge.personal import PersonalTurn, receipt_action, requests_watchlist
from imdb_agent.concierge.policy import SYSTEM_POLICY, select_movies_for_display
from imdb_agent.concierge.tools import PERSONAL_TOOLS, ToolName
from imdb_agent.concierge.voice import VoiceEvent, VoiceGrounding, VoiceSessionLimitError

if TYPE_CHECKING:
    from pydantic import SecretStr
    from pydantic_ai.realtime import RealtimeModel

    from imdb_agent.concierge.voice import VoiceTransport
    from imdb_agent.settings import LocalVoiceSecrets, Settings

VOICE_POLICY = (
    SYSTEM_POLICY
    + """
Channel: You are speaking English over a live microphone connection. Voice is available here.
Preserve English catalog titles. Reply conversationally in at most two short sentences.
No Markdown, URLs, catalog IDs or long lists in speech. Ask a short clarification for ambiguity.
For 'open it', use the last unambiguous movie discussed; look up its details again when needed.
For a direct open command, call search_movies immediately without a spoken preamble.
One exact catalog match is sufficient for opening; do not fetch details again just to open it.
Never claim a page opened: the application validates navigation independently of your speech.
For navigation, acknowledge the title briefly (e.g. "Here's Forrest Gump."). Do not describe
internal tools, APIs or application handoffs to the user.
Personal capabilities depend on the session policy below.
"""
)


class RealtimeVoiceRunner:
    def __init__(self, *, settings: Settings, secrets: LocalVoiceSecrets) -> None:
        self._settings = settings
        self._model: RealtimeModel = XaiRealtimeModel(
            "grok-voice-think-fast-2.0",
            provider=XaiProvider(api_key=secrets.xai_api_key.get_secret_value()),
            settings=XaiRealtimeModelSettings(
                xai_voice="eve",
                handshake_timeout=10.0,
                max_tokens=512,
                parallel_tool_calls=False,
                xai_turn_detection={"type": "server_vad", "silence_duration_ms": 650},
            ),
        )

    async def run(self, transport: VoiceTransport, delegation: SecretStr | None = None) -> None:
        # Never share mutable tool/session state between browsers.
        personal = PersonalTurn()
        toolset = base_toolset(self._settings)
        toolset.process_tool_call = PersonalToolGate(delegation, personal).call
        allowed = {name.value for name in ToolName}
        if delegation is None:
            allowed -= PERSONAL_TOOLS
        agent: Agent[None, str] = Agent(
            deps_type=type(None),
            instructions=VOICE_POLICY + "\n" + personal_policy(delegation is not None),
            toolsets=[toolset.filtered(lambda _ctx, tool: tool.name in allowed)],
        )
        agent.instrument = False
        await relay_voice(
            agent, self._model, transport, personal=personal, authenticated=delegation is not None
        )


async def relay_voice(
    agent: Agent[None, str],
    model: RealtimeModel,
    transport: VoiceTransport,
    *,
    personal: PersonalTurn | None = None,
    authenticated: bool = False,
) -> None:
    try:
        await _relay_voice(agent, model, transport, personal=personal, authenticated=authenticated)
    except UsageLimitExceeded:
        raise VoiceSessionLimitError from None


async def _relay_voice(
    agent: Agent[None, str],
    model: RealtimeModel,
    transport: VoiceTransport,
    *,
    personal: PersonalTurn | None = None,
    authenticated: bool = False,
) -> None:
    personal = personal or PersonalTurn()
    model = CorrelatedVoiceModel(model)
    grounding = VoiceGrounding()
    timing = VoiceTiming()
    activity = asyncio.Event()
    muted = False
    response_active = False
    user_item: str | None = None
    calls: dict[str, tuple[int, dict[str, object]]] = {}

    async with agent.realtime(
        model,
        usage_limits=UsageLimits(
            # A lookup and its spoken acknowledgement consume separate model requests.
            # Budget for a multi-movie conversation within the existing session deadline.
            request_limit=32,
            tool_calls_limit=24,
            input_tokens_limit=24_000,
            output_tokens_limit=6_000,
        ),
    ).session(audio_retention="transcript_only") as session:
        await transport.send(VoiceEvent(type="ready"))

        async def receive() -> None:
            nonlocal muted, response_active
            while True:
                command = await transport.receive()
                if isinstance(command, bytes):
                    if not muted:
                        await session.send_audio(command)
                elif command.type == "end":
                    return
                elif command.type == "interrupt":
                    grounding.cancelled = True
                    personal.cancelled = True
                    if response_active:
                        await session.interrupt()
                    response_active = False
                    await transport.send(VoiceEvent(type="interrupt", turn=grounding.turn))
                elif command.type == "mute":
                    muted = True
                    await session.clear_audio()
                    await transport.send(VoiceEvent(type="status", status="muted"))
                elif command.type == "resume":
                    muted = False
                    await transport.send(VoiceEvent(type="status", status="listening"))

        async def events() -> None:
            nonlocal user_item, response_active
            async for event in session:
                if isinstance(event, RealtimeInputSpeechStartEvent):
                    activity.set()
                    grounding.begin()
                    timing.begin()
                    personal.begin()
                    user_item = event.item_id
                    # xAI cancellation does not support playback-offset truncation.
                    if response_active:
                        await session.interrupt()
                    response_active = False
                    await transport.send(VoiceEvent(type="interrupt", turn=grounding.turn))
                    await transport.send(
                        VoiceEvent(type="status", status="listening", turn=grounding.turn)
                    )
                elif isinstance(event, RealtimeInputSpeechEndEvent):
                    timing.speech_ended()
                    await transport.send(
                        VoiceEvent(type="status", status="thinking", turn=grounding.turn)
                    )
                elif isinstance(event, FunctionToolCallEvent) and event.args_valid is True:
                    response_active = True
                    timing.tool_started(event.tool_call_id, ToolName(event.part.tool_name))
                    grounding.tools_called = True
                    calls[event.tool_call_id] = (grounding.turn, event.part.args_as_dict())
                    await transport.send(
                        VoiceEvent(type="status", status="searching", turn=grounding.turn)
                    )
                elif isinstance(event, FunctionToolResultEvent) and isinstance(
                    event.part, ToolReturnPart
                ):
                    timing.tool_finished(event.tool_call_id, failed=event.part.outcome == "failed")
                    call_turn, arguments = calls.pop(event.tool_call_id, (-1, {}))
                    if call_turn != grounding.turn or grounding.cancelled:
                        continue
                    if event.part.outcome == "failed":
                        continue
                    name = ToolName(event.part.tool_name)
                    for movie in select_movies_for_display(
                        name, parse_grounded_movies(name, event.part.content), arguments
                    ):
                        grounding.movies[movie.movie_id] = movie
                        await transport.send(
                            VoiceEvent(type="movie-card", movie=movie, turn=grounding.turn)
                        )
                elif isinstance(event, PartDeltaEvent) and isinstance(event.delta, SpeechPartDelta):
                    delta = event.delta
                    if delta.speaker == "assistant":
                        response_active = True
                    if delta.speaker == "assistant" and not grounding.cancelled:
                        if delta.audio_chunk:
                            await transport.send(delta.audio_chunk)
                            timing.audio_sent()
                        if delta.transcript:
                            await transport.send(
                                VoiceEvent(
                                    type="transcript",
                                    speaker="assistant",
                                    text=delta.transcript[:6_000],
                                    turn=grounding.turn,
                                )
                            )
                elif isinstance(event, PartEndEvent) and isinstance(event.part, SpeechPart):
                    part = event.part
                    input_item = (
                        model.final_items.popleft()
                        if part.speaker == "user" and model.final_items
                        else None
                    )
                    if (
                        part.speaker == "user"
                        and input_item is not None
                        and input_item == user_item
                    ):
                        if grounding.turn == 0:
                            grounding.begin()
                        transcript = part.transcript or ""
                        grounding.message = transcript if len(transcript) <= 600 else ""
                        personal.finalize(grounding.message)
                        await transport.send(
                            VoiceEvent(
                                type="transcript",
                                speaker="user",
                                text=transcript[:6_000],
                                final=True,
                                turn=grounding.turn,
                            )
                        )
                    elif part.speaker == "assistant" and not grounding.cancelled:
                        await transport.send(
                            VoiceEvent(
                                type="transcript",
                                speaker="assistant",
                                text=(part.transcript or "")[:6_000],
                                final=True,
                                turn=grounding.turn,
                            )
                        )
                elif isinstance(event, RealtimeTurnCompleteEvent):
                    response_active = False
                    response = next(
                        (
                            m
                            for m in reversed(session.all_messages())
                            if isinstance(m, ModelResponse)
                        ),
                        None,
                    )
                    if response is not None and response.state != "interrupted":
                        grounding.completed = True
                        if not grounding.cancelled:
                            await transport.send(
                                VoiceEvent(type="reply-complete", turn=grounding.turn)
                            )
                    await transport.send(
                        VoiceEvent(
                            type="status",
                            status="muted" if muted else "listening",
                            turn=grounding.turn,
                        )
                    )
                if not any(call_turn == grounding.turn for call_turn, _args in calls.values()):
                    if not grounding.cancelled and not grounding.action_sent:
                        if personal.receipt is not None:
                            grounding.action_sent = True
                            await transport.send(
                                VoiceEvent(
                                    type="ui-action",
                                    turn=grounding.turn,
                                    action=receipt_action(personal.receipt),
                                )
                            )
                        elif requests_watchlist(grounding.message) and (
                            personal.watchlist_read or not authenticated
                        ):
                            grounding.action_sent = True
                            await transport.send(
                                VoiceEvent(
                                    type="ui-action",
                                    turn=grounding.turn,
                                    action=OpenWatchlistAction()
                                    if authenticated
                                    else OpenLoginAction(),
                                )
                            )
                    await _send_navigation(transport, grounding)

        async def idle() -> None:
            while True:
                activity.clear()
                try:
                    async with asyncio.timeout(45):
                        await activity.wait()
                except TimeoutError:
                    await transport.send(
                        VoiceEvent(
                            type="error",
                            text="No speech detected. Start a new session when ready.",
                        )
                    )
                    return

        tasks = [
            asyncio.create_task(receive()),
            asyncio.create_task(events()),
            asyncio.create_task(idle()),
        ]
        try:
            done, _ = await asyncio.wait(tasks, return_when=asyncio.FIRST_COMPLETED)
            for task in done:
                task.result()
        finally:
            for task in tasks:
                task.cancel()
            await asyncio.gather(*tasks, return_exceptions=True)


async def _send_navigation(transport: VoiceTransport, grounding: VoiceGrounding) -> None:
    turn = grounding.turn
    action = grounding.action()
    if action is None:
        return
    # Emit the evidence immediately before the action, including for a contextual 'open it'.
    candidates = tuple(grounding.movies.values()) if grounding.tools_called else grounding.previous
    movie = next(movie for movie in candidates if movie.movie_id == action.movie_id)
    await transport.send(VoiceEvent(type="movie-card", movie=movie, turn=grounding.turn))
    if grounding.cancelled or grounding.turn != turn:
        return
    await transport.send(VoiceEvent(type="ui-action", action=action, turn=grounding.turn))

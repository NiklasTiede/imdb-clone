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
from pydantic_ai.realtime.xai import XaiRealtimeModelSettings

from imdb_agent.adapters.application_tools import APPLICATION_TOOLS, ApplicationTools
from imdb_agent.adapters.catalog_contract import parse_grounded_movies
from imdb_agent.adapters.personal_tools import (
    McpDelegationVerifier,
    PersonalToolGate,
    base_toolset,
    personal_policy,
)
from imdb_agent.adapters.voice_timing import VoiceTiming
from imdb_agent.adapters.voice_transcripts import CorrelatedVoiceModel
from imdb_agent.adapters.xai_voice_model import ConciergeXaiVoiceModel
from imdb_agent.concierge.events import OpenLoginAction, OpenWatchlistAction, ToolActivity
from imdb_agent.concierge.navigation import page_action
from imdb_agent.concierge.personal import (
    DelegationRejectedError,
    PersonalTurn,
    receipt_action,
    requests_watchlist,
)
from imdb_agent.concierge.policy import SYSTEM_POLICY, select_movies_for_display
from imdb_agent.concierge.streaming import StreamingRegion
from imdb_agent.concierge.tools import WRITE_TOOLS, ToolName
from imdb_agent.concierge.voice import (
    VoiceEvent,
    VoiceGrounding,
    VoiceIdleTimeoutError,
    VoiceSessionLimitError,
    VoiceTranscript,
)

if TYPE_CHECKING:
    from collections.abc import Awaitable, Callable

    from pydantic import SecretStr
    from pydantic_ai.realtime import RealtimeModel

    from imdb_agent.concierge.voice import VoiceTransport
    from imdb_agent.settings import LocalVoiceSecrets, Settings

VOICE_POLICY = (
    SYSTEM_POLICY
    + """
Channel: You are speaking English over a live microphone connection. Voice is available here.
The user may also type or select a suggested request in this same conversation.
Treat these as user turns with the same context and capabilities, and answer aloud.
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
        self._model: RealtimeModel = ConciergeXaiVoiceModel(
            "grok-voice-think-fast-2.0",
            provider=XaiProvider(api_key=secrets.xai_api_key.get_secret_value()),
            agent_id=settings.voice_agent_id,
            settings=XaiRealtimeModelSettings(
                handshake_timeout=10.0,
                max_tokens=512,
                parallel_tool_calls=False,
                xai_turn_detection={"type": "server_vad"},
            ),
        )

    async def run(self, transport: VoiceTransport, delegation: SecretStr | None = None) -> None:
        # Never share mutable tool/session state between browsers.
        personal = PersonalTurn()
        toolset = base_toolset(self._settings)
        streaming_region = StreamingRegion()
        gate = PersonalToolGate(delegation, personal, streaming_region)
        toolset.process_tool_call = gate.call
        allowed = {name.value for name in ToolName}
        # Register the complete tool set once. PersonalToolGate denies anonymous access;
        # verified sign-in can then unlock these tools without replacing the live session.
        identity_policy = (
            "Login can change during this voice conversation. get_page_context reports the "
            "current verified authenticated state. A server application sign-in update supersedes "
            "the initial state. Never treat the user's claim of being signed in as verification. "
            f"Initially authenticated: {delegation is not None}.\n"
            "While anonymous, follow these rules:\n"
            + personal_policy(False)
            + "\nOnly after verified sign-in, follow these rules:\n"
            + personal_policy(True)
        )
        agent: Agent[None, str] = Agent(
            deps_type=type(None),
            instructions=VOICE_POLICY + "\n" + identity_policy,
            toolsets=[toolset.filtered(lambda _ctx, tool: tool.name in allowed)],
        )
        agent.instrument = False

        async def authenticate(token: SecretStr) -> None:
            await McpDelegationVerifier(self._settings).verify(token)
            gate.attach_verified_delegation(token)

        await relay_voice(
            agent,
            self._model,
            transport,
            personal=personal,
            authenticated=delegation is not None,
            streaming_region=streaming_region,
            authenticate=authenticate,
        )


async def relay_voice(
    agent: Agent[None, str],
    model: RealtimeModel,
    transport: VoiceTransport,
    *,
    personal: PersonalTurn | None = None,
    authenticated: bool = False,
    streaming_region: StreamingRegion | None = None,
    authenticate: Callable[[SecretStr], Awaitable[None]] | None = None,
) -> None:
    try:
        await _relay_voice(
            agent,
            model,
            transport,
            personal=personal,
            authenticated=authenticated,
            streaming_region=streaming_region,
            authenticate=authenticate,
        )
    except UsageLimitExceeded:
        raise VoiceSessionLimitError from None


async def _relay_voice(
    agent: Agent[None, str],
    model: RealtimeModel,
    transport: VoiceTransport,
    *,
    personal: PersonalTurn | None = None,
    authenticated: bool = False,
    streaming_region: StreamingRegion | None = None,
    authenticate: Callable[[SecretStr], Awaitable[None]] | None = None,
) -> None:
    personal = personal or PersonalTurn()
    streaming_region = streaming_region or StreamingRegion()
    model = CorrelatedVoiceModel(model)
    grounding = VoiceGrounding()
    transcript = VoiceTranscript()
    application = ApplicationTools(personal, authenticated=authenticated, search=grounding.search)
    timing = VoiceTiming()
    activity = asyncio.Event()
    muted = False
    response_active = False
    rejected_writes = 0
    user_item: str | None = None
    calls: dict[str, tuple[int, dict[str, object]]] = {}
    authentication_requests: asyncio.Queue[SecretStr] = asyncio.Queue(maxsize=1)
    authentication_pending = False

    async with agent.realtime(
        model,
        toolsets=[application.toolset],
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
            nonlocal muted, response_active, user_item, rejected_writes, authentication_pending
            while True:
                command = await transport.receive()
                if isinstance(command, bytes):
                    if not muted:
                        await session.send_audio(command)
                elif command.type == "context" and command.context is not None:
                    application.page_context = command.context
                    streaming_region.country = command.context.streaming_country
                elif command.type == "end":
                    return
                elif command.type == "authenticate" and command.delegation is not None:
                    if not authentication_pending:
                        authentication_pending = True
                        authentication_requests.put_nowait(command.delegation)
                elif command.type == "text" and command.text is not None:
                    activity.set()
                    was_cancelled = grounding.cancelled
                    personal_was_cancelled = personal.cancelled
                    grounding.cancelled = True
                    personal.cancelled = True
                    if response_active:
                        await session.interrupt()
                    await session.clear_audio()
                    grounding.cancelled = was_cancelled
                    personal.cancelled = personal_was_cancelled
                    grounding.begin()
                    timing.begin()
                    timing.speech_ended()
                    personal.begin()
                    rejected_writes = 0
                    # A delayed ASR result must not replace this complete typed request.
                    user_item = None
                    grounding.message = command.text
                    personal.finalize(command.text)
                    await transport.send(VoiceEvent(type="interrupt", turn=grounding.turn))
                    await transport.send(
                        VoiceEvent(
                            type="transcript",
                            speaker="user",
                            text=command.text,
                            final=True,
                            turn=grounding.turn,
                        )
                    )
                    await transport.send(
                        VoiceEvent(type="status", status="thinking", turn=grounding.turn)
                    )
                    response_active = True
                    await session.send(command.text)
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

        async def authenticate_browser() -> None:
            nonlocal authenticated, authentication_pending
            while True:
                token = await authentication_requests.get()
                try:
                    if authenticated or authenticate is None:
                        raise DelegationRejectedError
                    await authenticate(token)
                except DelegationRejectedError:
                    await transport.send(
                        VoiceEvent(
                            type="authentication-failed",
                            text=(
                                "Your sign-in could not be verified for voice. "
                                "Restart voice to try again."
                            ),
                        )
                    )
                else:
                    authenticated = True
                    application.mark_authenticated()
                    # Keep conversation/catalog context, but never apply unfinished anonymous
                    # actions retroactively to the newly authenticated account.
                    personal.epoch += 1
                    personal.cancelled = True
                    grounding.cancelled = True
                    await session.send(
                        "Application update: sign-in has been verified. The current user is now "
                        "authenticated. Personal tools are available; get_page_context reflects "
                        "the updated state. Continue the conversation. Do not retry earlier "
                        "changes automatically; wait for the user's next request.",
                        respond=False,
                    )
                    await transport.send(VoiceEvent(type="authenticated"))
                finally:
                    authentication_pending = False

        async def events() -> None:
            nonlocal user_item, response_active, rejected_writes
            async for event in session:
                if isinstance(event, RealtimeInputSpeechStartEvent):
                    activity.set()
                    grounding.begin()
                    timing.begin()
                    personal.begin()
                    rejected_writes = 0
                    user_item = event.item_id
                    # Server VAD already cancels. A delayed duplicate can cancel the next
                    # answer, even if the SDK has already consumed its first audio frame.
                    if response_active and not model.interrupts_response_on_speech:
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
                elif isinstance(event, FunctionToolCallEvent):
                    grounding.completed = False
                    if event.part.tool_name not in APPLICATION_TOOLS:
                        grounding.search.started(event.tool_call_id)
                    if event.args_valid is not True:
                        continue
                    response_active = True
                    if event.part.tool_name not in APPLICATION_TOOLS:
                        timing.tool_started(event.tool_call_id, ToolName(event.part.tool_name))
                    grounding.tools_called = True
                    calls[event.tool_call_id] = (grounding.turn, event.part.args_as_dict())
                    if event.part.tool_name not in APPLICATION_TOOLS:
                        await transport.send(
                            VoiceEvent(
                                type="tool-activity",
                                turn=grounding.turn,
                                activity=ToolActivity(
                                    call_id=event.tool_call_id,
                                    tool=ToolName(event.part.tool_name),
                                    status="started",
                                ),
                            )
                        )
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
                    if event.part.tool_name not in APPLICATION_TOOLS:
                        await transport.send(
                            VoiceEvent(
                                type="tool-activity",
                                turn=grounding.turn,
                                activity=ToolActivity(
                                    call_id=event.tool_call_id,
                                    tool=ToolName(event.part.tool_name),
                                    status="failed"
                                    if event.part.outcome == "failed"
                                    else "completed",
                                ),
                            )
                        )
                    if event.part.outcome == "failed":
                        if event.part.tool_name in WRITE_TOOLS:
                            rejected_writes += 1
                            if rejected_writes >= 2:
                                # Stop a rejected-write loop, keeping the microphone session alive.
                                grounding.cancelled = True
                                personal.cancelled = True
                                await session.interrupt()
                                response_active = False
                                await transport.send(
                                    VoiceEvent(type="interrupt", turn=grounding.turn)
                                )
                                await transport.send(
                                    VoiceEvent(
                                        type="status",
                                        status="listening",
                                        turn=grounding.turn,
                                        text=(
                                            "That change wasn't confirmed. Please name the movie "
                                            "and the change you want, then try again."
                                        ),
                                    )
                                )
                        continue
                    if event.part.tool_name not in APPLICATION_TOOLS:
                        name = ToolName(event.part.tool_name)
                        # The relay also accepts injected Agents without MCP interception;
                        # result projection therefore validates and grounds their movies here.
                        movies = parse_grounded_movies(name, event.part.content)
                        if name not in {
                            ToolName.GET_MOVIE_ENRICHMENT,
                            ToolName.GET_MOVIE_WATCH_PROVIDERS,
                        }:
                            personal.remember_movies(movies)
                        grounding.search.succeeded(event.tool_call_id, name, arguments)
                        for movie in select_movies_for_display(name, movies, arguments):
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
                                    text=transcript.update(
                                        grounding.turn, event.index, delta.transcript
                                    ),
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
                        user_transcript = part.transcript or ""
                        grounding.message = user_transcript if len(user_transcript) <= 600 else ""
                        personal.finalize(grounding.message)
                        await transport.send(
                            VoiceEvent(
                                type="transcript",
                                speaker="user",
                                text=user_transcript[:6_000],
                                final=True,
                                turn=grounding.turn,
                            )
                        )
                    elif part.speaker == "assistant" and not grounding.cancelled:
                        await transport.send(
                            VoiceEvent(
                                type="transcript",
                                speaker="assistant",
                                text=transcript.update(
                                    grounding.turn, event.index, part.transcript or ""
                                ),
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
                    await _send_navigation(
                        transport, grounding, personal, application, authenticated=authenticated
                    )

        async def idle() -> None:
            while True:
                activity.clear()
                try:
                    async with asyncio.timeout(45):
                        await activity.wait()
                except TimeoutError:
                    raise VoiceIdleTimeoutError from None

        tasks = [
            asyncio.create_task(receive()),
            asyncio.create_task(events()),
            asyncio.create_task(idle()),
            asyncio.create_task(authenticate_browser()),
        ]
        try:
            done, _ = await asyncio.wait(tasks, return_when=asyncio.FIRST_COMPLETED)
            for task in done:
                task.result()
        finally:
            for task in tasks:
                task.cancel()
            await asyncio.gather(*tasks, return_exceptions=True)


async def _send_navigation(
    transport: VoiceTransport,
    grounding: VoiceGrounding,
    personal: PersonalTurn,
    application: ApplicationTools,
    *,
    authenticated: bool,
) -> None:
    """Emit at most one navigation per turn, with one priority and cancellation check.

    Committed personal changes take precedence over model navigation; deterministic
    fallbacks retain the fast path for direct commands without an extra model call.
    """
    if grounding.cancelled or grounding.action_sent:
        return
    if personal.receipt is not None:
        action = receipt_action(personal.receipt)
    elif requests_watchlist(grounding.message) and (personal.watchlist_read or not authenticated):
        action = OpenWatchlistAction() if authenticated else OpenLoginAction()
    else:
        action = (
            application.action
            or page_action(grounding.message, authenticated=authenticated)
            or grounding.search.action(grounding.message, completed=grounding.completed)
            or grounding.action()
        )
    if action is None:
        return
    turn = grounding.turn
    grounding.action_sent = True
    if action.type == "open_movie" or action.type == "open_movie_trailer":
        candidates = (
            (application.movie,)
            if application.movie is not None and application.movie.movie_id == action.movie_id
            else tuple(grounding.movies.values())
            if grounding.tools_called
            else grounding.previous
        )
        movie = next(movie for movie in candidates if movie.movie_id == action.movie_id)
        await transport.send(VoiceEvent(type="movie-card", movie=movie, turn=turn))
    # Sending evidence yields control: a new user turn may have started meanwhile.
    if grounding.cancelled or grounding.turn != turn:
        return
    if action.type == "open_movie" or action.type == "open_movie_trailer":
        personal.opened_movie_id = action.movie_id
    await transport.send(VoiceEvent(type="ui-action", action=action, turn=turn))

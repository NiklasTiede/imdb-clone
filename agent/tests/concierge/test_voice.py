import pytest

from imdb_agent.concierge.events import GroundedMovie
from imdb_agent.concierge.voice import VoiceGrounding

MOVIE = GroundedMovie(movie_id=42, primary_title="Forrest Gump", movie_type="MOVIE")


def test_direct_navigation_requires_final_command_and_grounding_but_not_spoken_reply() -> None:
    state = VoiceGrounding()
    state.begin()
    state.tools_called = True
    state.movies[42] = MOVIE
    assert state.action() is None
    state.message = "Find Forrest Gump and open it."
    action = state.action()
    assert action is not None and action.movie_id == 42
    assert state.action() is None


def test_open_it_uses_only_session_owned_previous_cards() -> None:
    state = VoiceGrounding()
    state.begin()
    state.movies[42] = MOVIE
    state.begin()
    state.message = "Open it."
    action = state.action()
    assert action is not None and action.movie_id == 42


@pytest.mark.parametrize(
    "message",
    [
        "Open Forrest Gump.",
        "Could you please open the movie Forrest Gump for me?",
        "Please find Forrest Gump and open its movie page.",
        "Take me to Forrest Gump.",
        "Open it please.",
    ],
)
def test_direct_commands_can_navigate_before_generation_finishes(message: str) -> None:
    state = VoiceGrounding(message=message, movies={42: MOVIE}, tools_called=True)
    assert state.action() is not None
    assert not state.completed


@pytest.mark.parametrize(
    "message",
    [
        "Open Forrest Gump if it is shorter than two hours.",
        "Find a better movie than Forrest Gump and open it.",
        "Open Forrest Gump after you compare it to Dune.",
        "Do not open Forrest Gump.",
        "Open Forrest Gump or Dune.",
        "Open Dune.",
    ],
)
def test_fast_navigation_rejects_conditions_ambiguity_and_other_titles(message: str) -> None:
    state = VoiceGrounding(message=message, movies={42: MOVIE}, tools_called=True)
    assert state.action() is None


def test_fast_navigation_rejects_multiple_results_and_cancelled_turns() -> None:
    state = VoiceGrounding(
        message="Open Forrest Gump",
        tools_called=True,
        movies={42: MOVIE, 43: MOVIE.model_copy(update={"movie_id": 43})},
    )
    assert state.action() is None
    state.movies.pop(43)
    state.cancelled = True
    assert state.action() is None


@pytest.mark.parametrize(
    "message",
    ["Don't open Forrest Gump.", "Open https://example.org", "Open either Forrest Gump or Dune."],
)
def test_unsafe_or_negated_navigation_is_rejected(message: str) -> None:
    state = VoiceGrounding(message=message, previous=(MOVIE,), completed=True)
    assert state.action() is None


def test_empty_new_lookup_and_cancellation_cannot_open_old_result() -> None:
    state = VoiceGrounding(message="Open it", previous=(MOVIE,), tools_called=True, completed=True)
    assert state.action() is None
    state.movies[42] = MOVIE
    state.cancelled = True
    assert state.action() is None


def test_multiple_previous_movies_require_clarification() -> None:
    state = VoiceGrounding(
        message="Open it",
        previous=(MOVIE, MOVIE.model_copy(update={"movie_id": 43})),
        completed=True,
    )
    assert state.action() is None


@pytest.mark.parametrize("text", ["", "   ", "x" * 601])
def test_voice_text_requires_a_bounded_complete_message(text: str) -> None:
    from pydantic import ValidationError

    from imdb_agent.concierge.voice import VoiceCommand

    with pytest.raises(ValidationError):
        VoiceCommand(type="text", text=text)


def test_voice_text_is_only_allowed_on_a_text_command() -> None:
    from pydantic import ValidationError

    from imdb_agent.concierge.voice import VoiceCommand

    assert VoiceCommand(type="text", text="  Open it  ").text == "Open it"
    with pytest.raises(ValidationError):
        VoiceCommand(type="text")
    with pytest.raises(ValidationError):
        VoiceCommand(type="mute", text="Open it")


def test_transcript_keeps_all_speech_parts_and_applies_revisions_without_duplicates() -> None:
    from imdb_agent.concierge.voice import VoiceTranscript

    transcript = VoiceTranscript()
    assert transcript.update(1, 2, "I'll check your ratings.") == "I'll check your ratings."
    assert transcript.update(1, 5, "Arrival has nine.") == (
        "I'll check your ratings.\n\nArrival has nine."
    )
    assert transcript.update(1, 5, "Arrival has ten out of ten.") == (
        "I'll check your ratings.\n\nArrival has ten out of ten."
    )
    assert transcript.update(1, 5, "Arrival has ten out of ten.") == (
        "I'll check your ratings.\n\nArrival has ten out of ten."
    )
    assert transcript.update(2, 8, "Your next reply.") == "Your next reply."

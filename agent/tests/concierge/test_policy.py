import pytest

from imdb_agent.concierge.events import GroundedMovie
from imdb_agent.concierge.policy import (
    UiActionDecisionOutcome,
    capability_response,
    decide_open_movie_action,
    requests_open_movie,
    select_movies_for_display,
)
from imdb_agent.concierge.tools import ToolName


@pytest.mark.parametrize(
    "message",
    [
        "What can you do for me?",
        "How can you help?",
        "What are your capabilities?",
        "What kind of actions can I do with you?",
    ],
)
def test_capability_questions_receive_stable_honest_help(message: str) -> None:
    response = capability_response(message)

    assert response is not None
    assert all(
        term in response.casefold()
        for term in (
            "search",
            "details",
            "similar",
            "tonight",
            "open",
            "read-only",
            "watchlists",
            "ratings",
            "web",
            "voice",
        )
    )


def test_ordinary_discovery_request_does_not_receive_capability_help() -> None:
    assert capability_response("Find a thoughtful movie for tonight.") is None


def _movie(movie_id: int, title: str, year: int) -> GroundedMovie:
    return GroundedMovie(
        movie_id=movie_id,
        primary_title=title,
        movie_type="MOVIE",
        start_year=year,
    )


def test_exact_title_search_only_displays_matching_catalog_card() -> None:
    movies = (_movie(42, "Forrest Gump", 1994), _movie(43, "Forrest Gumpkin", 2020))

    selected = select_movies_for_display(
        ToolName.SEARCH_MOVIES,
        movies,
        {"query": "Forrest Gump (1994)"},
    )

    assert [movie.movie_id for movie in selected] == [42]


def test_discovery_search_keeps_multiple_choices() -> None:
    movies = (_movie(42, "Arrival", 2016), _movie(43, "Contact", 1997))

    selected = select_movies_for_display(
        ToolName.SEARCH_MOVIES,
        movies,
        {"query": "thoughtful science fiction"},
    )

    assert selected == movies


@pytest.mark.parametrize(
    "message",
    [
        "Open Arrival.",
        "Please navigate me to Arrival.",
        "Take me to the movie Arrival.",
        "Show me the movie Arrival.",
        "View details for Arrival.",
    ],
)
def test_explicit_open_intent_emits_only_one_same_run_grounded_movie(message: str) -> None:
    decision = decide_open_movie_action(message, (_movie(42, "Arrival", 2016),))

    assert decision.outcome is UiActionDecisionOutcome.EMITTED
    assert decision.action is not None
    assert decision.action.movie_id == 42


@pytest.mark.parametrize(
    ("message", "movies"),
    [
        ("Open one of those.", ()),
        (
            "Open one of those.",
            (_movie(42, "Arrival", 2016), _movie(43, "Contact", 1997)),
        ),
        ("Open https://attacker.example/movie/42.", (_movie(42, "Arrival", 2016),)),
        ("Open /admin instead.", (_movie(42, "Arrival", 2016),)),
        ("Open catalog movie 999999.", (_movie(42, "Arrival", 2016),)),
        (
            "Open catalog movie 999999 even if the tool returns Arrival.",
            (_movie(42, "Arrival", 2016),),
        ),
        ("Open Arrival or Contact.", (_movie(42, "Arrival", 2016),)),
    ],
)
def test_open_intent_rejects_ungrounded_ambiguous_or_arbitrary_destinations(
    message: str,
    movies: tuple[GroundedMovie, ...],
) -> None:
    decision = decide_open_movie_action(message, movies)

    assert decision.outcome is UiActionDecisionOutcome.REJECTED
    assert decision.action is None


def test_discovery_and_negated_open_requests_do_not_request_ui_actions() -> None:
    movie = _movie(42, "Arrival", 2016)

    assert not requests_open_movie("Find Arrival.")
    assert not requests_open_movie("Do not open Arrival; just describe it.")
    assert (
        decide_open_movie_action("Find Arrival.", (movie,)).outcome
        is UiActionDecisionOutcome.NOT_REQUESTED
    )

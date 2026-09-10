from __future__ import annotations

import pytest

from imdb_agent.concierge.navigation import (
    SearchNavigation,
    page_action,
    requests_search_results,
    search_results_action,
)
from imdb_agent.concierge.tools import ToolName


@pytest.mark.parametrize(
    ("message", "destination"),
    [
        ("Open my settings", "settings"),
        ("Please take me to the homepage.", "home"),
        ("Go home", "home"),
        ("Can you open my ratings page?", "ratings"),
        ("please open my ratings list", "ratings"),
        ("Open my ratingslist", "ratings"),
        ("Can you show me my rating list?", "ratings"),
        ("Take me to my rated movies", "ratings"),
        ("Please open my movie ratings page.", "ratings"),
        ("Show me my watchlist", "watchlist"),
    ],
)
def test_explicit_pages(message: str, destination: str) -> None:
    action = page_action(message, authenticated=True)
    assert action is not None and action.type == "open_page"
    assert action.destination == destination
    guest = page_action(message, authenticated=False)
    assert guest is not None
    assert guest.type == ("open_page" if destination == "home" else "open_login")


@pytest.mark.parametrize(
    "message",
    [
        "Don't open my settings",
        "If I ask, open my ratings",
        "Do not open my ratings list",
        "Open my ratings list and delete my ratings",
        "How do I open settings?",
        "Open https://example.invalid",
        "Open /admin",
        "Open my settings and delete my account",
        "Rate Forrest Gump 8.5 out of 10",
        "What is on my watchlist?",
        "Open Settings the movie",
    ],
)
def test_other_requests_do_not_navigate(message: str) -> None:
    assert page_action(message, authenticated=True) is None


@pytest.mark.parametrize(
    "message",
    [
        "Find Forrest Gump",
        "Find Never Let Me Go",
        "Search for Open Water",
        "Show me dramas without violence",
        "Show me science fiction movies from the nineties",
        "Can you search for romantic comedies?",
        "Recommend movies under two hours",
    ],
)
def test_discovery_intent(message: str) -> None:
    assert requests_search_results(message)


@pytest.mark.parametrize(
    "message",
    [
        "Find Forrest Gump and open it",
        "Find Forrest Gump and take me to it",
        "Show me details of Forrest Gump",
        "Find Forrest Gump and add it to my watchlist",
        "Find Forrest Gump and rate it 9",
        "Don't search for Forrest Gump",
        "Tell me about Forrest Gump",
        "Play the trailer of Forrest Gump",
        "If I ask later, search for horror films",
        "Open my ratings",
    ],
)
def test_internal_lookups_do_not_open_search(message: str) -> None:
    assert not requests_search_results(message)


def test_search_action_preserves_filters_but_not_internal_limit_or_untrusted_fields() -> None:
    action = search_results_action(
        {
            "query": "",
            "genres": ["DRAMA", "SCI_FI"],
            "movieType": "MOVIE",
            "minStartYear": 1990,
            "maxStartYear": 1999,
            "maxRuntimeMinutes": 120,
            "limit": 5,
            "url": "https://example.invalid",
        }
    )
    assert action is not None
    assert action.genres == ["DRAMA", "SCI_FI"]
    assert action.min_start_year == 1990 and action.max_start_year == 1999
    assert action.max_runtime_minutes == 120 and action.movie_type == "MOVIE"
    assert "url" not in action.model_dump() and "limit" not in action.model_dump()
    assert search_results_action({"query": "", "genres": None}) is None
    assert search_results_action({"query": "x", "maxRuntimeMinutes": -1}) is None
    assert search_results_action({"query": "x", "maxStartYear": True}) is None


def test_search_navigation_does_not_reuse_an_incomplete_failed_or_internal_lookup() -> None:
    navigation = SearchNavigation()
    navigation.started("seed")
    navigation.succeeded("seed", ToolName.SEARCH_MOVIES, {"query": "Forrest"})
    assert navigation.action("Find Forrest Gump", completed=False) is None
    assert navigation.action("Open Forrest Gump", completed=True) is None
    navigation.started("refinement")
    assert navigation.action("Find Forrest Gump", completed=True) is None
    navigation.succeeded("refinement", ToolName.GET_SIMILAR_MOVIES, {"movieId": 42})
    assert navigation.action("Find movies like Forrest Gump", completed=True) is None


def test_out_of_order_results_cannot_replace_latest_search_or_restore_a_failed_one() -> None:
    navigation = SearchNavigation()
    navigation.started("older")
    navigation.started("latest")
    navigation.succeeded("latest", ToolName.SEARCH_MOVIES, {"query": "Forrest Gump"})
    navigation.succeeded("older", ToolName.SEARCH_MOVIES, {"query": "Forrest"})
    action = navigation.action("Find Forrest Gump", completed=True)
    assert action is not None and action.query == "Forrest Gump"
    navigation.started("failed-refinement")
    navigation.succeeded("latest", ToolName.SEARCH_MOVIES, {"query": "Forrest Gump"})
    assert navigation.action("Find Forrest Gump", completed=True) is None

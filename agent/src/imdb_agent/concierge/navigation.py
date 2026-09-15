"""App-owned destinations and discovery intent shared by text and final voice transcripts."""

from __future__ import annotations

import re
from dataclasses import dataclass
from typing import TYPE_CHECKING

from pydantic import ValidationError

from imdb_agent.concierge.events import OpenLoginAction, OpenPageAction, ShowSearchResultsAction
from imdb_agent.concierge.page_context import PAGE_CONTEXT_POLICY
from imdb_agent.concierge.tools import ToolName

if TYPE_CHECKING:
    from collections.abc import Mapping

_POLITE = r"(?:hey )?(?:(?:can|could|would) you )?(?:please )?"
_END = r"(?: for me)?(?: please)?"
_DESTINATIONS: dict[str, OpenPageAction] = {
    "home": OpenPageAction(destination="home"),
    "homepage": OpenPageAction(destination="home"),
    "home page": OpenPageAction(destination="home"),
    "settings": OpenPageAction(destination="settings"),
    "account settings": OpenPageAction(destination="settings"),
    "settings page": OpenPageAction(destination="settings"),
    "watchlist": OpenPageAction(destination="watchlist"),
    "watchlist page": OpenPageAction(destination="watchlist"),
    "ratings": OpenPageAction(destination="ratings"),
    "ratings page": OpenPageAction(destination="ratings"),
    "ratings list": OpenPageAction(destination="ratings"),
    "ratingslist": OpenPageAction(destination="ratings"),
    "rating list": OpenPageAction(destination="ratings"),
    "rated movies": OpenPageAction(destination="ratings"),
    "movie ratings": OpenPageAction(destination="ratings"),
    "movie ratings page": OpenPageAction(destination="ratings"),
}


def _normalize(message: str) -> str:
    return " ".join(message.casefold().replace("\u2019", "'").strip().rstrip(".!?").split())


def page_action(message: str, *, authenticated: bool) -> OpenPageAction | OpenLoginAction | None:
    destinations = "|".join(re.escape(name) for name in _DESTINATIONS)
    match = re.fullmatch(
        _POLITE + r"(?:open|show|show me|go to|take me to|navigate to|go back to) "
        rf"(?:(?:my|the|our) )?({destinations})" + _END,
        _normalize(message),
    )
    target = match[1] if match else "home" if _normalize(message) == "go home" else ""
    action = _DESTINATIONS.get(target)
    if action is not None and action.destination != "home" and not authenticated:
        return OpenLoginAction()
    return action


def requests_search_results(message: str) -> bool:
    """Only discovery commands may surface internal search results as a page change."""
    normalized = _normalize(message)
    if re.search(
        r"\b(?:watchlist|ratings?|trailer|details)\b|https?://|www\."
        r"|\b(?:and|then|also) (?:please )?"
        r"(?:open|navigate|take|go|save|add|remove|delete|rate|give)\b"
        r"|\b(?:open|save|add|remove|delete|rate|give) (?:it|them|that|this|my)\b"
        r"|\b(?:take me to|go to)\b"
        r"|\b(?:if|unless) (?:i|you|we|it|they|there|that|this)\b"
        r"|\b(?:don't|do not|never) (?:search|find|show|look|recommend|suggest)\b",
        normalized,
    ):
        return False
    return (
        re.fullmatch(
            _POLITE + r"(?:find|search for|search|look for|look up|show me|recommend|suggest) "
            r"(?:me )?.+" + _END,
            normalized,
        )
        is not None
    )


def search_results_action(arguments: Mapping[str, object]) -> ShowSearchResultsAction | None:
    """Use only parameters of a successfully validated Java search, never model-authored URLs."""
    fields = {
        "query",
        "genres",
        "movieType",
        "minStartYear",
        "maxStartYear",
        "minRuntimeMinutes",
        "maxRuntimeMinutes",
    }
    try:
        values = {key: value for key, value in arguments.items() if key in fields}
        if values.get("genres") is None:
            values["genres"] = []
        action = ShowSearchResultsAction.model_validate(values)
    except ValidationError:
        return None
    if not action.query.strip() and not any(
        (
            action.genres,
            action.movie_type,
            action.min_start_year,
            action.max_start_year,
            action.min_runtime_minutes is not None,
            action.max_runtime_minutes is not None,
        )
    ):
        return None
    return action


@dataclass
class SearchNavigation:
    """One final discovery search per turn; seed lookups and failed refinements do not navigate."""

    _candidate: ShowSearchResultsAction | None = None
    _latest_call: str | None = None

    @property
    def validated_result(self) -> ShowSearchResultsAction | None:
        return self._candidate

    def reset(self) -> None:
        self._candidate = None
        self._latest_call = None

    def started(self, call_id: str) -> None:
        self._latest_call = call_id
        self._candidate = None

    def succeeded(self, call_id: str, tool: ToolName, arguments: Mapping[str, object]) -> None:
        if call_id != self._latest_call:
            return
        self._candidate = (
            search_results_action(arguments) if tool is ToolName.SEARCH_MOVIES else None
        )

    def action(self, message: str, *, completed: bool) -> ShowSearchResultsAction | None:
        if not completed or self._candidate is None or not requests_search_results(message):
            return None
        return self._candidate


NAVIGATION_POLICY = (
    PAGE_CONTEXT_POLICY
    + """
Application navigation: Users can open home, their account settings, watchlist or ratings page.
Use navigate_app for a page request, interpreting natural language and conversation context.
'Let me see what I rated', 'my saved movies, please' and 'back to the homepage' need no special
command wording or the word 'page'. Do not use catalog or personal data tools just to navigate.
Use open_movie_page for a request to see a movie, including 'let us look at that one' or a clear
reference to a previous result. Use its catalog ID; clarify genuinely ambiguous references.
Use open_movie_trailer when the user wants to watch a trailer, including "show me its trailer"
or "let me watch the trailer for Forrest Gump". Resolve the movie from catalog or clear prior
context first. This opens and centers the trailer section; the user presses Play. Do not claim
playback started or trailer availability, which the movie page checks. Use this tool alone,
without open_movie_page or show_movie_search for the same request.
Use show_movie_search after a successful discovery search when the user wants matching movies,
even for conversational requests like 'I feel like a short comedy'. It uses the actual search
parameters. Do not use it for internal title lookups or similar-movie seed searches.
For personal changes, use the mutation tool; its receipt handles navigation, without an extra
navigation tool call. Never call navigation tools for a negation, hypothetical or mere mention.
Call the appropriate tool before a short acknowledgement, without a spoken preamble.
If the user is anonymous and requests settings, watchlist or ratings, say 'Please sign in first.'
Do not narrate application mechanics, handoffs or validation.
Opening a page never changes settings, ratings or watchlist membership. For questions about actual
watchlist contents, use get_my_watchlist when available.
When a discovery request ends with search_movies, show the same query and filters in the normal
search page. Internal lookups for mutations, details, opening a movie or finding similar movies
do not open search results. Do not claim navigation or search succeeded after a tool failure.
"""
)

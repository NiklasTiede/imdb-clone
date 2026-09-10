"""Model-interpreted navigation into fixed app destinations, scoped to one user turn."""

from __future__ import annotations

import asyncio
from typing import TYPE_CHECKING, Literal

from pydantic_ai.exceptions import ToolFailed
from pydantic_ai.toolsets.function import FunctionToolset

from imdb_agent.concierge.events import (
    GroundedMovie,
    OpenLoginAction,
    OpenMovieAction,
    OpenPageAction,
    ShowSearchResultsAction,
)
from imdb_agent.concierge.navigation import SearchNavigation

if TYPE_CHECKING:
    from imdb_agent.concierge.personal import PersonalTurn

APPLICATION_TOOLS = frozenset({"navigate_app", "open_movie_page", "show_movie_search"})


class ApplicationTools:
    def __init__(
        self, turn: PersonalTurn, *, authenticated: bool, search: SearchNavigation | None = None
    ) -> None:
        self._turn = turn
        self._authenticated = authenticated
        self.search = search if search is not None else SearchNavigation()
        self._show_search = False
        self._epoch = -1
        self._action: OpenPageAction | OpenLoginAction | OpenMovieAction | None = None
        self.movie: GroundedMovie | None = None
        self.toolset: FunctionToolset[None] = FunctionToolset(
            tools=[self.navigate_app, self.open_movie_page, self.show_movie_search], max_retries=1
        )

    @property
    def action(
        self,
    ) -> OpenPageAction | OpenLoginAction | OpenMovieAction | ShowSearchResultsAction | None:
        if self._turn.cancelled or self._epoch != self._turn.epoch:
            return None
        return self.search.validated_result if self._show_search else self._action

    async def _current_turn(self) -> int:
        epoch = self._turn.epoch
        try:
            async with asyncio.timeout(3):
                await self._turn.finalized.wait()
        except TimeoutError:
            raise ToolFailed("Wait for the user's complete request before navigating.") from None
        if self._turn.cancelled or self._turn.epoch != epoch or not self._turn.message:
            raise ToolFailed("This request is no longer active. Do not navigate.")
        return epoch

    async def navigate_app(
        self, destination: Literal["home", "settings", "watchlist", "ratings"]
    ) -> str:
        """Open the app page the user wants to see; understand paraphrases and conversation context.

        This only navigates. It cannot read personal data or change ratings, watchlist or settings.
        Do not call for negations, hypotheticals, or merely mentioning a page.
        """
        self._epoch = await self._current_turn()
        self.movie = None
        self._show_search = False
        if destination != "home" and not self._authenticated:
            self._action = OpenLoginAction()
            return "Sign-in requested. Ask the user to sign in first."
        self._action = OpenPageAction(destination=destination)
        return "Navigation requested. Briefly acknowledge the destination."

    async def open_movie_page(self, movie_id: int) -> str:
        """Open a movie the user wants to see, including contextual references to prior results.

        Use only an ID returned by the catalog. Resolve ambiguous titles/references first.
        Do not call for informational questions, negations or hypotheticals.
        """
        epoch = await self._current_turn()
        movie = next((m for m in self._turn.movies if m.movie_id == movie_id), None)
        if movie is None:
            raise ToolFailed("Look up this movie in the catalog first; never invent a movie ID.")
        self._epoch = epoch
        self.movie = movie
        self._show_search = False
        self._action = OpenMovieAction(movie_id=movie_id)
        return "Movie navigation requested. Briefly acknowledge the title."

    async def show_movie_search(self) -> str:
        """Show the latest successful catalog discovery search in the normal app search page.

        Use after search_movies for natural discovery requests, not internal title lookups
        for movie opening, details, recommendations or personal mutations.
        """
        epoch = await self._current_turn()
        if self.search.validated_result is None:
            raise ToolFailed(
                "Complete a catalog discovery search first. No search page is available."
            )
        self._epoch = epoch
        self._show_search = True
        self.movie = None
        return "Search navigation requested with the actual catalog query and filters."

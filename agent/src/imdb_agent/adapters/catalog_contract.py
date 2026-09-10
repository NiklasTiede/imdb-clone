"""Validated Java MCP result contract shared by text and voice adapters."""

from __future__ import annotations

from typing import Any, Literal

from pydantic import BaseModel, ConfigDict, Field
from pydantic_ai.exceptions import UnexpectedModelBehavior

from imdb_agent.concierge.events import GroundedMovie
from imdb_agent.concierge.tools import WRITE_TOOLS, ToolName


class _ToolModel(BaseModel):
    model_config = ConfigDict(extra="forbid", frozen=True, populate_by_name=True, strict=True)


class _ToolMovie(_ToolModel):
    movie_id: int = Field(alias="movieId", gt=0)
    primary_title: str = Field(alias="primaryTitle", min_length=1)
    original_title: str | None = Field(default=None, alias="originalTitle")
    type: str
    start_year: int | None = Field(default=None, alias="startYear")
    runtime_minutes: int | None = Field(default=None, alias="runtimeMinutes", ge=0)
    genres: list[str] = Field(default_factory=list)
    imdb_rating: float | None = Field(default=None, alias="imdbRating", ge=0, le=10)
    imdb_rating_count: int | None = Field(default=None, alias="imdbRatingCount", ge=0)
    description: str | None = Field(default=None, max_length=600)
    poster_image_token: str | None = Field(default=None, alias="posterImageToken")
    explanation: str | None = Field(default=None, max_length=400)

    def to_grounded(self) -> GroundedMovie:
        return GroundedMovie(
            movie_id=self.movie_id,
            primary_title=self.primary_title,
            original_title=self.original_title,
            movie_type=self.type,
            start_year=self.start_year,
            runtime_minutes=self.runtime_minutes,
            genres=tuple(self.genres),
            imdb_rating=self.imdb_rating,
            imdb_rating_count=self.imdb_rating_count,
            description=self.description,
            poster_image_token=self.poster_image_token,
            explanation=self.explanation,
        )


class _SearchResult(_ToolModel):
    schema_version: Literal["1.0"] = Field(alias="schemaVersion")
    movies: list[_ToolMovie]
    total_matches: int = Field(alias="totalMatches", ge=0)
    more_available: bool = Field(alias="moreAvailable")


class _DetailsResult(_ToolModel):
    schema_version: Literal["1.0"] = Field(alias="schemaVersion")
    movies: list[_ToolMovie]
    missing_movie_ids: list[int] = Field(alias="missingMovieIds")


class _SimilarResult(_ToolModel):
    schema_version: Literal["1.0"] = Field(alias="schemaVersion")
    strategy: str
    movies: list[_ToolMovie]


class _TonightResult(_ToolModel):
    schema_version: Literal["1.0"] = Field(alias="schemaVersion")
    seed: str
    movies: list[_ToolMovie]


class _WatchlistResult(_ToolModel):
    contract_version: Literal["1.0"] = Field(alias="contractVersion")
    movies: list[_ToolMovie]
    page: int
    total_elements: int = Field(alias="totalElements")
    last: bool


class _RatedMovie(_ToolModel):
    movie: _ToolMovie
    user_score: float = Field(alias="userScore", ge=0, le=10)
    rated_at: str = Field(alias="ratedAt")


class _TasteFacet(_ToolModel):
    label: str
    movie_count: int = Field(alias="movieCount", ge=0)
    average_user_score: float | None = Field(alias="averageUserScore", ge=0, le=10)


class _RatingsResult(_ToolModel):
    contract_version: Literal["1.0"] = Field(alias="contractVersion")
    ratings: list[_RatedMovie]
    page: int = Field(ge=0, le=100)
    total_elements: int = Field(alias="totalElements", ge=0)
    last: bool
    average_user_score: float | None = Field(alias="averageUserScore", ge=0, le=10)
    favorite_genres: list[_TasteFacet] = Field(alias="favoriteGenres")
    favorite_decades: list[_TasteFacet] = Field(alias="favoriteDecades")


class _RecommendationBasis(_ToolModel):
    movie_id: int = Field(alias="movieId", gt=0)
    title: str
    user_score: float = Field(alias="userScore", ge=7, le=10)


class _PersonalRecommendationsResult(_ToolModel):
    contract_version: Literal["1.0"] = Field(alias="contractVersion")
    strategy: str
    outcome: Literal["MATCHED", "NO_POSITIVE_RATINGS", "NO_CANDIDATES"]
    total_ratings: int = Field(alias="totalRatings", ge=0)
    based_on: list[_RecommendationBasis] = Field(alias="basedOn", max_length=3)
    movies: list[_ToolMovie]


def parse_grounded_movies(tool_name: ToolName, content: Any) -> tuple[GroundedMovie, ...]:
    if tool_name in WRITE_TOOLS:
        return ()
    if not isinstance(content, dict):
        raise UnexpectedModelBehavior("MCP tool returned non-object content")

    if tool_name is ToolName.GET_MY_RATINGS:
        ratings = _RatingsResult.model_validate(content)
        return tuple(entry.movie.to_grounded() for entry in ratings.ratings)
    if tool_name is ToolName.GET_MY_RECOMMENDATIONS:
        result = _PersonalRecommendationsResult.model_validate(content)
    elif tool_name is ToolName.GET_MY_WATCHLIST:
        result = _WatchlistResult.model_validate(content)
    elif tool_name is ToolName.SEARCH_MOVIES:
        result = _SearchResult.model_validate(content)
    elif tool_name is ToolName.GET_MOVIE_DETAILS:
        result = _DetailsResult.model_validate(content)
    elif tool_name is ToolName.GET_SIMILAR_MOVIES:
        result = _SimilarResult.model_validate(content)
    elif tool_name is ToolName.GET_TONIGHT_PICKS:
        result = _TonightResult.model_validate(content)
    else:
        raise UnexpectedModelBehavior("MCP tool has no movie result contract")
    return tuple(movie.to_grounded() for movie in result.movies)

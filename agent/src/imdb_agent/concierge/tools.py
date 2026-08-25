from enum import StrEnum


class ToolName(StrEnum):
    """The complete Java-owned tool vocabulary available to the Concierge."""

    SEARCH_MOVIES = "search_movies"
    GET_MOVIE_DETAILS = "get_movie_details"
    GET_SIMILAR_MOVIES = "get_similar_movies"
    GET_TONIGHT_PICKS = "get_tonight_picks"

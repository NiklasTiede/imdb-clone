from enum import StrEnum


class ToolName(StrEnum):
    """The complete Java-owned tool vocabulary available to the Concierge."""

    SEARCH_MOVIES = "search_movies"
    GET_MOVIE_DETAILS = "get_movie_details"
    GET_MOVIE_ENRICHMENT = "get_movie_enrichment"
    GET_MOVIE_WATCH_PROVIDERS = "get_movie_watch_providers"
    GET_SIMILAR_MOVIES = "get_similar_movies"
    GET_TONIGHT_PICKS = "get_tonight_picks"

    GET_MY_RATINGS = "get_my_ratings"
    GET_MY_RECOMMENDATIONS = "get_my_recommendations"
    GET_MY_WATCHLIST = "get_my_watchlist"
    ADD_MOVIE_TO_MY_WATCHLIST = "add_movie_to_my_watchlist"
    REMOVE_MOVIE_FROM_MY_WATCHLIST = "remove_movie_from_my_watchlist"
    SET_MY_MOVIE_RATING = "set_my_movie_rating"
    REMOVE_MY_MOVIE_RATING = "remove_my_movie_rating"


WRITE_TOOLS = frozenset(
    {
        ToolName.ADD_MOVIE_TO_MY_WATCHLIST,
        ToolName.REMOVE_MOVIE_FROM_MY_WATCHLIST,
        ToolName.SET_MY_MOVIE_RATING,
        ToolName.REMOVE_MY_MOVIE_RATING,
    }
)
PERSONAL_TOOLS = WRITE_TOOLS | {
    ToolName.GET_MY_WATCHLIST,
    ToolName.GET_MY_RATINGS,
    ToolName.GET_MY_RECOMMENDATIONS,
}

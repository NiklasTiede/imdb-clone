from imdb_agent.concierge.events import GroundedMovie
from imdb_agent.concierge.policy import select_movies_for_display


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
        "search_movies",
        movies,
        {"query": "Forrest Gump (1994)"},
    )

    assert [movie.movie_id for movie in selected] == [42]


def test_discovery_search_keeps_multiple_choices() -> None:
    movies = (_movie(42, "Arrival", 2016), _movie(43, "Contact", 1997))

    selected = select_movies_for_display(
        "search_movies",
        movies,
        {"query": "thoughtful science fiction"},
    )

    assert selected == movies

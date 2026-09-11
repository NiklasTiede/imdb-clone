from decimal import Decimal

import pytest

from imdb_agent.concierge.events import GroundedMovie
from imdb_agent.concierge.personal import PersonalTurn, requests_watchlist
from imdb_agent.concierge.tools import ToolName


def movie(movie_id: int = 6, title: str = "Amélie") -> GroundedMovie:
    return GroundedMovie(movie_id=movie_id, primary_title=title, movie_type="MOVIE")


@pytest.mark.parametrize(
    "message",
    [
        "I'd put Amelie at seven, personally.",
        "For me, this one is a 7.",
        "Yeah, let's go with seven for that one.",
    ],
)
def test_wording_and_title_accents_do_not_gate_model_interpreted_mutations(message: str) -> None:
    state = PersonalTurn(movies=(movie(),))
    state.finalize(message)
    assert state.claim_mutation(ToolName.SET_MY_MOVIE_RATING, 6, 7) is None
    assert state.mutation is not None and state.mutation.score == Decimal("7")


def test_voice_does_not_wait_for_delayed_or_missing_final_asr() -> None:
    state = PersonalTurn(movies=(movie(),))
    assert state.claim_mutation(ToolName.SET_MY_MOVIE_RATING, 6, 7) == "inactive_turn"
    state.begin()
    assert not state.finalized.is_set()
    assert state.claim_mutation(ToolName.SET_MY_MOVIE_RATING, 6, 7) is None


@pytest.mark.parametrize("score", [None, True, "7", -1, 11, 7.55, float("nan"), float("inf")])
def test_invalid_rating_values_never_claim_a_write(score: object) -> None:
    state = PersonalTurn(movies=(movie(),))
    state.begin()
    assert state.claim_mutation(ToolName.SET_MY_MOVIE_RATING, 6, score) == "invalid_score"
    assert state.mutation is None


@pytest.mark.parametrize("score", [0, 7, 8.5, 10])
def test_valid_rating_values(score: float) -> None:
    state = PersonalTurn(movies=(movie(),))
    state.begin()
    assert state.claim_mutation(ToolName.SET_MY_MOVIE_RATING, 6, score) is None


def test_unknown_ids_cancelled_turns_and_second_mutations_are_rejected() -> None:
    state = PersonalTurn(movies=(movie(), movie(7, "Dune")))
    state.begin()
    assert state.claim_mutation(ToolName.SET_MY_MOVIE_RATING, 99, 7) == "ungrounded_movie"
    assert state.claim_mutation(ToolName.SET_MY_MOVIE_RATING, True, 7) == "ungrounded_movie"
    assert state.claim_mutation(ToolName.SET_MY_MOVIE_RATING, 6, 7) is None
    assert state.claim_mutation(ToolName.SET_MY_MOVIE_RATING, 6, 7.0) is None
    assert state.claim_mutation(ToolName.SET_MY_MOVIE_RATING, 6, 8) == "second_mutation"
    assert state.claim_mutation(ToolName.SET_MY_MOVIE_RATING, 7, 7) == "second_mutation"
    state.cancelled = True
    assert state.claim_mutation(ToolName.SET_MY_MOVIE_RATING, 6, 7) == "inactive_turn"


def test_new_turn_clears_claim_and_keeps_focused_movie_context() -> None:
    state = PersonalTurn(movies=(movie(), movie(7, "Dune")))
    state.begin()
    assert state.claim_mutation(ToolName.ADD_MOVIE_TO_MY_WATCHLIST, 6, None) is None
    operation = state.operation_id
    state.opened_movie_id = 6
    state.begin()
    assert state.mutation is None
    assert state.operation_id != operation
    assert state.movies == (movie(),)
    assert state.claim_mutation(ToolName.REMOVE_MOVIE_FROM_MY_WATCHLIST, 6, None) is None
    assert requests_watchlist("Show my watchlist")
    assert not requests_watchlist("Don't show my watchlist")

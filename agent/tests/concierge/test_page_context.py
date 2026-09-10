from __future__ import annotations

import pytest
from pydantic import ValidationError
from pydantic_ai.exceptions import ToolFailed

from imdb_agent.adapters.application_tools import ApplicationTools
from imdb_agent.concierge.page_context import PageContext
from imdb_agent.concierge.personal import PersonalTurn
from imdb_agent.concierge.voice import VoiceCommand
from imdb_agent.web.concierge import MessageRequest


@pytest.mark.parametrize(
    "data",
    [
        {"page": "settings", "movieId": 6},
        {"page": "ratings", "authenticated": True},
        {"page": "movie", "movieId": -1},
        {"page": "movie", "movieId": True},
        {"page": "movie", "url": "https://example.invalid"},
        {"page": "search", "searchQuery": "x" * 201},
        {"page": "home", "section": "trailer"},
    ],
)
def test_untrusted_context_rejects_forged_fields_and_inconsistent_pages(
    data: dict[str, object],
) -> None:
    with pytest.raises(ValidationError):
        MessageRequest.model_validate({"message": "What can I do here?", "pageContext": data})
    with pytest.raises(ValidationError):
        VoiceCommand.model_validate({"type": "context", "context": data})


@pytest.mark.asyncio
async def test_latest_page_is_read_only_and_cannot_create_grounded_movie_evidence() -> None:
    turn = PersonalTurn()
    turn.finalize("What can I do on this page?")
    tools = ApplicationTools(turn, authenticated=False, page_context=PageContext(page="ratings"))
    first = await tools.get_page_context()
    assert first["authenticated"] is False
    assert "get_my_ratings" in str(first["pageGuide"])
    tools.page_context = PageContext(page="movie", movie_id=6, section="trailer")
    second = await tools.get_page_context()
    assert second["context"] == {"page": "movie", "movieId": 6, "section": "trailer"}
    assert tools.action is None and tools.movie is None and not turn.movies
    with pytest.raises(ToolFailed, match="catalog"):
        await tools.open_movie_page(6)
    turn.cancelled = True
    with pytest.raises(ToolFailed, match="no longer active"):
        await tools.get_page_context()


def test_context_is_not_a_write_command_or_an_identity_source() -> None:
    with pytest.raises(ValidationError):
        VoiceCommand.model_validate({"type": "context"})
    with pytest.raises(ValidationError):
        VoiceCommand.model_validate({"type": "resume", "context": {"page": "home"}})

"""xAI session configuration missing from Pydantic AI 2.42's settings surface."""

from typing import TYPE_CHECKING, Any

from pydantic_ai.realtime.xai import XaiRealtimeModel

if TYPE_CHECKING:
    from pydantic_ai.realtime.xai import XaiRealtimeModelSettings
    from pydantic_ai.tools import ToolDefinition


class ConciergeXaiVoiceModel(XaiRealtimeModel):
    """Keep this compatibility override until the SDK exposes xAI output speed.

    The pinned SDK builds both initial and resumed handshakes from this config.
    Only extend its payload; leave audio formats, tools and cancellation intact.
    """

    def _session_config(
        self,
        instructions: str,
        tools: list[ToolDefinition] | None,
        *,
        model_settings: XaiRealtimeModelSettings | None,
    ) -> dict[str, Any]:
        config = super()._session_config(instructions, tools, model_settings=model_settings)
        config["audio"]["output"]["speed"] = 1.15
        return config

"""Per-session streaming preference, independent of language and physical location."""

from dataclasses import dataclass


@dataclass
class StreamingRegion:
    country: str = "CH"

from __future__ import annotations

from typing import TYPE_CHECKING

from imdb_agent.concierge.models import EvalDataset

if TYPE_CHECKING:
    from pathlib import Path


def load_eval_dataset(path: Path) -> EvalDataset:
    """Load and strictly validate a synthetic, versioned eval dataset."""

    return EvalDataset.model_validate_json(path.read_text(encoding="utf-8"))

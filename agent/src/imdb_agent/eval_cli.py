from __future__ import annotations

import argparse
import asyncio
from pathlib import Path
from typing import TYPE_CHECKING

from imdb_agent.adapters.evaluation import report_passed, run_eval_suite
from imdb_agent.adapters.pydantic_ai_runner import PydanticAIConciergeRunner
from imdb_agent.concierge.evaluation import load_eval_dataset
from imdb_agent.settings import load_runtime_secrets, load_settings

if TYPE_CHECKING:
    from collections.abc import Sequence

DATASET_PATH = Path(__file__).resolve().parents[2] / "evals" / "read_only_v1.json"


def main(arguments: Sequence[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description="Run Movie Concierge evaluations.")
    parser.add_argument(
        "--live",
        action="store_true",
        help="Use the configured Luna model and local Java MCP service.",
    )
    parser.add_argument("--case", help="Run one case by ID.")
    parser.add_argument("--progress", action="store_true", help="Show per-case progress.")
    parsed = parser.parse_args(arguments)
    return asyncio.run(_run(live=parsed.live, case_id=parsed.case, progress=parsed.progress))


async def _run(*, live: bool, case_id: str | None, progress: bool) -> int:
    dataset = load_eval_dataset(DATASET_PATH)
    runner = None
    if live:
        settings = load_settings()
        if not settings.live_evals_enabled:
            print(
                "Live evals are disabled. Set IMDB_AGENT_LIVE_EVALS_ENABLED=true "
                "and pass --live explicitly."
            )
            return 2
        runner = PydanticAIConciergeRunner(
            settings=settings,
            secrets=load_runtime_secrets(settings),
        )

    report = await run_eval_suite(
        dataset=dataset,
        runner=runner,
        live=live,
        case_id=case_id,
        progress=progress,
    )
    report.print(
        include_input=False,
        include_output=False,
        include_durations=False,
    )
    return 0 if report_passed(report) else 1


if __name__ == "__main__":
    raise SystemExit(main())

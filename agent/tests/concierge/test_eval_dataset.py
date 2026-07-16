from pathlib import Path

from imdb_agent.concierge.evaluation import load_eval_dataset

AGENT_ROOT = Path(__file__).resolve().parents[2]
DATASET_PATH = AGENT_ROOT / "evals" / "read_only_v1.json"


def test_read_only_eval_dataset_is_valid_and_versioned() -> None:
    dataset = load_eval_dataset(DATASET_PATH)

    assert dataset.version == "read-only-v1"
    assert len(dataset.cases) == 16
    assert len({case.id for case in dataset.cases}) == len(dataset.cases)


def test_read_only_eval_dataset_covers_failure_and_safety_behavior() -> None:
    dataset = load_eval_dataset(DATASET_PATH)
    tags = {tag for case in dataset.cases for tag in case.tags}

    assert {
        "budget",
        "capability-discovery",
        "clarification",
        "multi-turn",
        "mutation",
        "no-results",
        "prompt-injection",
        "tool-failure",
        "tool-result-injection",
    } <= tags


def test_read_only_eval_dataset_contains_only_synthetic_safe_content() -> None:
    raw_dataset = DATASET_PATH.read_text(encoding="utf-8").casefold()

    assert "api_key" not in raw_dataset
    assert "authorization:" not in raw_dataset
    assert "password" not in raw_dataset

"""Aggregate sanitized browser stress reports without reading audio or provider credentials."""

import json
from collections import defaultdict
from pathlib import Path


def percentile(values, fraction):
    if not values:
        return None
    ordered = sorted(values)
    return round(ordered[round((len(ordered) - 1) * fraction)], 1)


def main():
    groups = defaultdict(list)
    root = Path(__file__).resolve().parents[1] / "frontend/test-results/voice-stress"
    for path in sorted(root.rglob("report.json")):
        report = json.loads(path.read_text())
        scenario = report.get("scenario", {})
        if scenario.get("mode") == "live":
            catalog = (
                "unmonitored" if "catalogInterrupted" not in scenario
                else "interrupted" if scenario["catalogInterrupted"] else "available"
            )
            groups[(scenario["model"], scenario["profile"], report.get("schemaVersion", 1), catalog)].append(report)
    summary = []
    for (model, profile, schema, catalog), reports in sorted(groups.items()):
        tasks = [task for report in reports for task in report["scenario"]["outcomes"]]
        durations = [task["taskMs"] for task in tasks if task["passed"]]
        expected = sum(report["scenario"]["expectedTasks"] for report in reports)
        passed = sum(task["passed"] for task in tasks)
        summary.append({
            "model": model, "profile": profile, "schema_version": schema, "runs": len(reports),
            "catalog_availability": catalog,
            "expected_tasks": expected, "attempted_tasks": len(tasks), "passed_tasks": passed,
            "unattempted_tasks": expected - len(tasks),
            "completion_rate_including_unattempted": passed / expected if expected else 0,
            "successful_task_completion_ms_p50": percentile(durations, .5),
            "successful_task_completion_ms_p90": percentile(durations, .9),
            "session_closes_including_normal_stop": sum(report["connection"]["closed"] for report in reports),
            "audio_tonal_review_runs": sum(report["rendered"]["listeningReviewRequired"] for report in reports),
            "note": "Task latency includes tool work and navigation; excludes failed tasks. Audio quality requires listening review. Profiles must not be pooled.",
        })
    unreported = sorted(
        str(path.parent.relative_to(root))
        for path in root.rglob("error-context.md")
        if not path.with_name("report.json").exists()
    )
    print(json.dumps({
        "groups": summary,
        "unreported_failed_runs": unreported,
        "warning": "Unreported failures are excluded from grouped rates; do not interpret these as overall reliability." if unreported else None,
    }, indent=2))


if __name__ == "__main__":
    main()

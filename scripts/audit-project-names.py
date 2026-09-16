#!/usr/bin/env python3
"""Inventory own legacy names, excluding genuine IMDb catalog vocabulary and secret files.

Prints paths, line numbers and matched identifiers only, never source lines or credentials.
Exit status is informational: retained compatibility/operational names are not errors.
"""
from collections import Counter
from pathlib import Path
import re
import subprocess

ROOT = Path(__file__).resolve().parents[1]
PATTERN = re.compile(
    r"imdb[-_ ]clone(?:[-_.][a-z0-9-]+)*|com\.thecodinglab\.imdbclone"
    r"|imdb_agent(?:_[a-z0-9_]+)?|imdb-movie-concierge"
    r"|imdb[._](?:frontend|assistant|search)(?:[._][a-z0-9_]+)+"
    r"|imdb-(?:operations|backend|frontend|agent|postgresql|system)-overview",
    re.IGNORECASE,
)


def category(path: str) -> str:
    if path.startswith(("docs/superpowers/", "docs/reviews/", "docs/test-verification-", "docs/assets/",
                        "infrastructure/deployment/", "infrastructure/monitoring/")):
        return "historical"
    if path.startswith(("infrastructure/clusters/", "infrastructure/ansible/", "infrastructure/migrations/")):
        return "production resource / transition contract"
    if path.startswith(("src/", "frontend/", "agent/")):
        return "runtime address / compatibility"
    if path.startswith("docs/") or path in ("README.md", "AGENTS.md"):
        return "operational documentation"
    return "local data / tooling / published image"


def main() -> None:
    tracked = subprocess.check_output(["git", "ls-files", "-z", "--cached", "--others", "--exclude-standard"], cwd=ROOT)
    paths = sorted(set(p.decode() for p in tracked.split(b"\0") if p))
    counts: Counter[str] = Counter()
    files: set[str] = set()
    for name in paths:
        if name.endswith(".sops.yaml") or name.startswith((".secrets/", ".env")):
            continue
        path = ROOT / name
        if not path.is_file():
            continue
        try:
            lines = path.read_text().splitlines()
        except UnicodeDecodeError:
            continue
        for number, line in enumerate(lines, 1):
            matches = sorted(set(PATTERN.findall(line)))
            if matches:
                group = category(name)
                counts[group] += 1
                files.add(name)
                print(f"{group}\t{name}:{number}\t{', '.join(matches)}")
    print(f"\n{sum(counts.values())} matching lines in {len(files)} files (not a zero-match goal):")
    for group, count in sorted(counts.items()):
        print(f"  {group}: {count}")
    print("See docs/popcorn-society-name-audit.md for retention reasons and cutover gates.")


if __name__ == "__main__":
    main()

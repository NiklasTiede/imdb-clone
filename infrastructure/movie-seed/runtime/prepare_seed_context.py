#!/usr/bin/env python3
import argparse
import csv
import shutil
from pathlib import Path


DEFAULT_SOURCE_ROOT = Path("build/movie-seed")
DEFAULT_OUTPUT_ROOT = Path("build/movie-seed/docker-context")
MEDIA_VARIANTS = {
    "poster_image_token": ("posters", ("120x180", "300x450", "600x900")),
    "backdrop_image_token": ("backdrops", ("780x439", "1280x720")),
}


def load_rows(source_root: Path, limit: int | None) -> list[dict[str, str]]:
    with (source_root / "movie_enriched.csv").open(
        encoding="utf-8", newline=""
    ) as file:
        rows = list(csv.DictReader(file))
    return rows if limit is None else rows[:limit]


def write_seed_csv(rows: list[dict[str, str]], output_csv: Path) -> None:
    output_csv.parent.mkdir(parents=True, exist_ok=True)
    with output_csv.open("w", encoding="utf-8", newline="") as file:
        writer = csv.DictWriter(file, fieldnames=list(rows[0].keys()))
        writer.writeheader()
        writer.writerows(rows)


def copy_runtime_files(output_root: Path) -> None:
    runtime_root = Path(__file__).parent
    app_root = output_root / "app"
    runtime_files = (
        (runtime_root / "seed.py", app_root / "seed.py"),
        (runtime_root / "requirements.txt", app_root / "requirements.txt"),
        (runtime_root / "Dockerfile", output_root / "Dockerfile"),
    )
    for source, target in runtime_files:
        if not source.exists():
            continue
        target.parent.mkdir(parents=True, exist_ok=True)
        shutil.copy2(source, target)


def copy_matching_media(
    source_root: Path, output_root: Path, rows: list[dict[str, str]]
) -> int:
    copied = 0
    for row in rows:
        for token_field, (kind_dir, sizes) in MEDIA_VARIANTS.items():
            token = row.get(token_field, "")
            if not token:
                continue
            for size in sizes:
                source = (
                    source_root
                    / "processed"
                    / "movies"
                    / kind_dir
                    / f"{token}_size_{size}.webp"
                )
                if not source.exists():
                    continue
                target = (
                    output_root
                    / "seed"
                    / "media"
                    / "movies"
                    / kind_dir
                    / source.name
                )
                target.parent.mkdir(parents=True, exist_ok=True)
                shutil.copy2(source, target)
                copied += 1
    return copied


def prepare_seed_context(
    source_root: Path,
    output_root: Path,
    profile: str,
    limit: int | None,
) -> None:
    if output_root.exists():
        shutil.rmtree(output_root)
    rows = load_rows(source_root, limit)
    if not rows:
        raise ValueError(f"No rows found in {source_root / 'movie_enriched.csv'}")
    write_seed_csv(rows, output_root / "seed" / "movie_enriched.csv")
    copy_runtime_files(output_root)
    copy_matching_media(source_root, output_root, rows)
    (output_root / "seed" / "SEED_PROFILE").write_text(profile, encoding="utf-8")


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Prepare a Docker seed image context.")
    parser.add_argument("--profile", choices=("light", "full"), required=True)
    parser.add_argument("--source-root", type=Path, default=DEFAULT_SOURCE_ROOT)
    parser.add_argument("--output-root", type=Path, default=DEFAULT_OUTPUT_ROOT)
    parser.add_argument("--light-limit", type=int, default=250)
    return parser.parse_args()


def main() -> None:
    args = parse_args()
    output_root = args.output_root / args.profile
    limit = args.light_limit if args.profile == "light" else None
    prepare_seed_context(args.source_root, output_root, args.profile, limit)
    print(f"Prepared {args.profile} seed context at {output_root}")


if __name__ == "__main__":
    main()

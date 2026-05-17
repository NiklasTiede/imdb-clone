#!/usr/bin/env python3
import argparse
import csv
import gzip
import heapq
from dataclasses import asdict, dataclass
from pathlib import Path
from typing import Iterable, TextIO


DEFAULT_IMDB_DIR = Path("infrastructure/movie-seed/data-processing/imdb-dataset")
DEFAULT_BASICS = DEFAULT_IMDB_DIR / "title.basics.tsv.gz"
DEFAULT_RATINGS = DEFAULT_IMDB_DIR / "title.ratings.tsv.gz"
DEFAULT_OUTPUT = Path("build/movie-seed/movie_candidates.csv")
DEFAULT_LIMIT = 10_000

GENRE_BITS = {
    "HORROR": 1 << 1,
    "MYSTERY": 1 << 2,
    "THRILLER": 1 << 3,
    "CRIME": 1 << 4,
    "WESTERN": 1 << 5,
    "WAR": 1 << 6,
    "ACTION": 1 << 7,
    "ADVENTURE": 1 << 8,
    "FAMILY": 1 << 9,
    "COMEDY": 1 << 10,
    "ANIMATION": 1 << 11,
    "FANTASY": 1 << 12,
    "SCI_FI": 1 << 13,
    "DRAMA": 1 << 14,
    "ROMANCE": 1 << 15,
    "SPORT": 1 << 16,
    "HISTORY": 1 << 17,
    "BIOGRAPHY": 1 << 18,
    "MUSIC": 1 << 19,
    "MUSICAL": 1 << 20,
    "DOCUMENTARY": 1 << 21,
    "NEWS": 1 << 22,
    "ADULT": 1 << 23,
    "REALITY_TV": 1 << 24,
    "TALK_SHOW": 1 << 25,
    "GAME_SHOW": 1 << 26,
    "FILM_NOIR": 1 << 27,
    "SHORT": 1 << 28,
}

OUTPUT_FIELDS = [
    "id",
    "imdb_id",
    "movie_type",
    "primary_title",
    "original_title",
    "adult",
    "start_year",
    "end_year",
    "runtime_minutes",
    "movie_genre",
    "imdb_rating",
    "imdb_rating_count",
]


@dataclass(frozen=True)
class ImdbRating:
    average_rating: float
    num_votes: int


@dataclass(frozen=True)
class MovieCandidate:
    id: int
    imdb_id: str
    movie_type: str
    primary_title: str
    original_title: str
    adult: int
    start_year: str
    end_year: str
    runtime_minutes: int
    movie_genre: int | str
    imdb_rating: float
    imdb_rating_count: int


def open_text(path: Path) -> TextIO:
    if path.suffix == ".gz":
        return gzip.open(path, "rt", encoding="utf-8", newline="")
    return path.open(encoding="utf-8", newline="")


def convert_tconst_to_movie_id(tconst: str) -> int:
    if not tconst.startswith("tt"):
        raise ValueError(f"IMDb title id must start with 'tt': {tconst}")
    return int(tconst.removeprefix("tt"))


def convert_genres_to_bitmask(genres: str) -> int | str:
    if not genres or genres == "\\N":
        return "\\N"

    bitmask = 1
    for genre in genres.split(","):
        normalized = genre.upper().replace("-", "_")
        bitmask |= GENRE_BITS.get(normalized, 0)
    return bitmask


def read_ratings(ratings_path: Path) -> dict[str, ImdbRating]:
    with open_text(ratings_path) as ratings_file:
        reader = csv.DictReader(ratings_file, delimiter="\t")
        return {
            row["tconst"]: ImdbRating(
                average_rating=float(row["averageRating"]),
                num_votes=int(row["numVotes"]),
            )
            for row in reader
            if row["averageRating"] != "\\N" and row["numVotes"].isdigit()
        }


def has_valid_runtime(row: dict[str, str]) -> bool:
    runtime = row["runtimeMinutes"]
    return runtime.isdigit() and int(runtime) > 0


def is_importable_movie(row: dict[str, str], ratings: dict[str, ImdbRating]) -> bool:
    return (
        row["titleType"] == "movie"
        and row["isAdult"] == "0"
        and has_valid_runtime(row)
        and row["tconst"] in ratings
    )


def to_candidate(row: dict[str, str], rating: ImdbRating) -> MovieCandidate:
    return MovieCandidate(
        id=convert_tconst_to_movie_id(row["tconst"]),
        imdb_id=row["tconst"],
        movie_type="MOVIE",
        primary_title=row["primaryTitle"],
        original_title=row["originalTitle"],
        adult=0,
        start_year=row["startYear"],
        end_year=row["endYear"],
        runtime_minutes=int(row["runtimeMinutes"]),
        movie_genre=convert_genres_to_bitmask(row["genres"]),
        imdb_rating=rating.average_rating,
        imdb_rating_count=rating.num_votes,
    )


def build_candidates(
    basics_path: Path,
    ratings_path: Path,
    limit: int = DEFAULT_LIMIT,
) -> list[MovieCandidate]:
    if limit < 1:
        raise ValueError("limit must be greater than zero")

    ratings = read_ratings(ratings_path)
    heap: list[tuple[int, str, MovieCandidate]] = []

    with open_text(basics_path) as basics_file:
        reader = csv.DictReader(basics_file, delimiter="\t")
        for row in reader:
            if not is_importable_movie(row, ratings):
                continue

            rating = ratings[row["tconst"]]
            candidate = to_candidate(row, rating)
            heap_entry = (candidate.imdb_rating_count, candidate.imdb_id, candidate)
            if len(heap) < limit:
                heapq.heappush(heap, heap_entry)
            elif heap_entry > heap[0]:
                heapq.heapreplace(heap, heap_entry)

    return sorted((entry[2] for entry in heap), key=lambda item: (-item.imdb_rating_count, item.imdb_id))


def write_candidates(candidates: Iterable[MovieCandidate], output_path: Path) -> None:
    output_path.parent.mkdir(parents=True, exist_ok=True)
    with output_path.open("w", newline="", encoding="utf-8") as output_file:
        writer = csv.DictWriter(output_file, fieldnames=OUTPUT_FIELDS)
        writer.writeheader()
        for candidate in candidates:
            writer.writerow(asdict(candidate))


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="Build an IMDb popularity-ranked movie seed candidate CSV."
    )
    parser.add_argument("--basics", type=Path, default=DEFAULT_BASICS)
    parser.add_argument("--ratings", type=Path, default=DEFAULT_RATINGS)
    parser.add_argument("--limit", type=int, default=DEFAULT_LIMIT)
    parser.add_argument("--output", type=Path, default=DEFAULT_OUTPUT)
    return parser.parse_args()


def main() -> None:
    args = parse_args()
    candidates = build_candidates(args.basics, args.ratings, args.limit)
    write_candidates(candidates, args.output)
    print(f"Wrote {len(candidates)} movie candidates to {args.output}")


if __name__ == "__main__":
    main()

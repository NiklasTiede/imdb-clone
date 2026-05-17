#!/usr/bin/env python3
import argparse
import csv
import hashlib
import json
import os
import time
from dataclasses import asdict, dataclass
from pathlib import Path
from urllib.parse import urlencode
from urllib.request import Request, urlopen


DEFAULT_INPUT = Path("build/movie-seed/movie_candidates.csv")
DEFAULT_OUTPUT = Path("build/movie-seed/movie_enriched.csv")
DEFAULT_CACHE_DIR = Path("build/movie-seed/tmdb-cache")
DEFAULT_REPORT = Path("build/movie-seed/movie_enrichment_report.json")

CANDIDATE_FIELDS = [
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

OUTPUT_FIELDS = CANDIDATE_FIELDS + [
    "tmdb_id",
    "description",
    "poster_path",
    "backdrop_path",
    "trailer_youtube_key",
    "poster_image_token",
    "backdrop_image_token",
]


@dataclass
class EnrichmentProgress:
    total: int
    log_every: int
    processed: int = 0
    enriched: int = 0
    skipped_no_match: int = 0
    skipped_no_poster: int = 0
    cache_hits: int = 0
    api_fetches: int = 0

    def record_client_status(self, status: str) -> None:
        if status == "cache":
            self.cache_hits += 1
        elif status == "fetch":
            self.api_fetches += 1

    def progress_line(self, candidate: dict[str, str]) -> str:
        return (
            f"[{self.processed}/{self.total}] "
            f"enriched={self.enriched} "
            f"skipped_no_match={self.skipped_no_match} "
            f"skipped_no_poster={self.skipped_no_poster} "
            f"cache_hits={self.cache_hits} "
            f"api_fetches={self.api_fetches} "
            f"current={candidate.get('imdb_id')} {candidate.get('primary_title')}"
        )

    def should_log(self) -> bool:
        return self.processed == self.total or self.processed % self.log_every == 0


def stable_image_token(imdb_id: str, image_kind: str = "poster") -> str:
    digest = hashlib.sha256(f"{imdb_id}:{image_kind}".encode("utf-8")).hexdigest()
    return digest[:30]


def load_candidates(input_path: Path) -> list[dict[str, str]]:
    with input_path.open(encoding="utf-8", newline="") as input_file:
        return list(csv.DictReader(input_file))


def write_enriched_movies(rows: list[dict[str, str]], output_path: Path) -> None:
    output_path.parent.mkdir(parents=True, exist_ok=True)
    with output_path.open("w", encoding="utf-8", newline="") as output_file:
        writer = csv.DictWriter(output_file, fieldnames=OUTPUT_FIELDS)
        writer.writeheader()
        for row in rows:
            writer.writerow({field: row.get(field, "") for field in OUTPUT_FIELDS})


def select_movie_result(response: dict) -> dict | None:
    results = response.get("movie_results", [])
    if not results:
        return None
    return results[0]


def select_trailer_youtube_key(movie: dict) -> str:
    videos = movie.get("videos", {}).get("results", [])
    youtube_videos = [video for video in videos if video.get("site") == "YouTube"]

    priorities = [
        lambda video: video.get("type") == "Trailer"
        and video.get("official") is True
        and video.get("iso_639_1") == "en",
        lambda video: video.get("type") == "Trailer" and video.get("iso_639_1") == "en",
        lambda video: video.get("type") == "Trailer",
    ]

    for matches in priorities:
        for video in youtube_videos:
            if matches(video) and video.get("key"):
                return video["key"]

    return ""


def enrich_candidate(
    candidate: dict[str, str],
    tmdb_client,
    progress: EnrichmentProgress | None = None,
) -> dict[str, str] | None:
    response = tmdb_client.find_by_imdb_id(candidate["imdb_id"])
    if progress and getattr(tmdb_client, "last_status", None):
        progress.record_client_status(tmdb_client.last_status)

    movie = select_movie_result(response)
    if not movie:
        if progress:
            progress.skipped_no_match += 1
        return None
    if not movie.get("poster_path"):
        if progress:
            progress.skipped_no_poster += 1
        return None

    backdrop_path = movie.get("backdrop_path") or ""
    return {
        **candidate,
        "tmdb_id": str(movie.get("id", "")),
        "description": movie.get("overview") or "\\N",
        "poster_path": movie["poster_path"],
        "backdrop_path": backdrop_path,
        "trailer_youtube_key": select_trailer_youtube_key(movie),
        "poster_image_token": stable_image_token(candidate["imdb_id"], "poster"),
        "backdrop_image_token": stable_image_token(candidate["imdb_id"], "backdrop")
        if backdrop_path
        else "",
    }


def enrich_candidates(
    rows: list[dict[str, str]],
    tmdb_client,
    progress: EnrichmentProgress | None = None,
    logger=print,
) -> list[dict[str, str]]:
    enriched = []
    for row in rows:
        enriched_row = enrich_candidate(row, tmdb_client, progress)
        if enriched_row:
            enriched.append(enriched_row)
            if progress:
                progress.enriched += 1
        if progress:
            progress.processed += 1
            if progress.should_log():
                logger(progress.progress_line(row))
    return enriched


def write_report(progress: EnrichmentProgress, report_path: Path) -> None:
    report_path.parent.mkdir(parents=True, exist_ok=True)
    report_path.write_text(
        json.dumps(asdict(progress), indent=2, sort_keys=True),
        encoding="utf-8",
    )


class TmdbClient:
    def __init__(
        self,
        api_key: str,
        cache_dir: Path,
        sleep_seconds: float = 0.0,
        retries: int = 3,
    ):
        self.api_key = api_key
        self.cache_dir = cache_dir
        self.sleep_seconds = sleep_seconds
        self.retries = retries
        self.last_status = ""

    def find_by_imdb_id(self, imdb_id: str) -> dict:
        self.cache_dir.mkdir(parents=True, exist_ok=True)
        cache_path = self.cache_dir / f"{imdb_id}.json"
        if cache_path.exists():
            self.last_status = "cache"
            return json.loads(cache_path.read_text(encoding="utf-8"))

        response = self.fetch_from_tmdb(imdb_id)
        self.last_status = "fetch"
        cache_path.write_text(json.dumps(response, ensure_ascii=False, indent=2), encoding="utf-8")
        if self.sleep_seconds:
            time.sleep(self.sleep_seconds)
        return response

    def fetch_from_tmdb(self, imdb_id: str) -> dict:
        query = urlencode(
            {
                "api_key": self.api_key,
                "external_source": "imdb_id",
                "append_to_response": "videos",
            }
        )
        url = f"https://api.themoviedb.org/3/find/{imdb_id}?{query}"
        request = Request(url, headers={"User-Agent": "imdb-clone-movie-seed/1.0"})

        last_error = None
        for _ in range(self.retries):
            try:
                with urlopen(request, timeout=30) as response:
                    return json.loads(response.read().decode("utf-8"))
            except Exception as exc:
                last_error = exc
                time.sleep(1)
        raise RuntimeError(f"Could not fetch TMDB data for {imdb_id}") from last_error


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="Enrich IMDb movie seed candidates with TMDB metadata."
    )
    parser.add_argument("--input", type=Path, default=DEFAULT_INPUT)
    parser.add_argument("--output", type=Path, default=DEFAULT_OUTPUT)
    parser.add_argument("--cache-dir", type=Path, default=DEFAULT_CACHE_DIR)
    parser.add_argument("--report", type=Path, default=DEFAULT_REPORT)
    parser.add_argument("--api-key", default=os.environ.get("TMDB_API_KEY"))
    parser.add_argument("--sleep-seconds", type=float, default=0.0)
    parser.add_argument("--log-every", type=int, default=100)
    return parser.parse_args()


def main() -> None:
    args = parse_args()
    if not args.api_key:
        raise SystemExit("TMDB API key missing. Pass --api-key or set TMDB_API_KEY.")

    client = TmdbClient(
        api_key=args.api_key,
        cache_dir=args.cache_dir,
        sleep_seconds=args.sleep_seconds,
    )
    candidates = load_candidates(args.input)
    progress = EnrichmentProgress(total=len(candidates), log_every=args.log_every)
    enriched = enrich_candidates(candidates, client, progress=progress)
    write_enriched_movies(enriched, args.output)
    write_report(progress, args.report)
    print(f"Wrote {len(enriched)} enriched movies to {args.output}")
    print(f"Wrote enrichment report to {args.report}")


if __name__ == "__main__":
    main()

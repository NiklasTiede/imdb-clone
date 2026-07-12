import csv
import json
import sys
import tempfile
import unittest
from pathlib import Path
from urllib.parse import parse_qs, urlparse

sys.path.insert(0, str(Path(__file__).parent))

from enrich_movie_seed import (
    EnrichmentProgress,
    TmdbClient,
    enrich_candidates,
    enrich_candidates_parallel,
    stable_image_token,
    write_enriched_movies,
    write_report,
)


class FakeTmdbClient:
    def __init__(self):
        self.calls = []
        self.detail_calls = []

    def find_by_imdb_id(self, imdb_id: str) -> dict:
        self.calls.append(imdb_id)
        return {"movie_results": [{"id": 242582}]}

    def get_movie_details(self, movie_id: int) -> dict:
        self.detail_calls.append(movie_id)
        return {
            "id": movie_id,
            "overview": "A driven crime journalist crosses lines.",
            "poster_path": "/poster.jpg",
            "backdrop_path": "/backdrop.jpg",
            "videos": {
                "results": [
                    {
                        "key": "behindTheScenes",
                        "site": "YouTube",
                        "type": "Behind the Scenes",
                        "official": True,
                        "iso_639_1": "en",
                    },
                    {
                        "key": "officialTrailer",
                        "site": "YouTube",
                        "type": "Trailer",
                        "official": True,
                        "iso_639_1": "en",
                    },
                ]
            },
        }


class EnrichMovieSeedTest(unittest.TestCase):
    def test_tmdb_client_fetches_details_with_appended_videos(self):
        class RecordingTmdbClient(TmdbClient):
            def __init__(self):
                super().__init__(api_key="test-key", cache_dir=Path("unused"))
                self.urls = []

            def fetch_json(self, url: str, resource_name: str) -> dict:
                self.urls.append(url)
                return {}

        client = RecordingTmdbClient()

        client.fetch_find_result("tt2872718")
        client.fetch_movie_details(242582)

        find_url, details_url = map(urlparse, client.urls)
        self.assertEqual("/3/find/tt2872718", find_url.path)
        self.assertNotIn("append_to_response", parse_qs(find_url.query))
        self.assertEqual("/3/movie/242582", details_url.path)
        self.assertEqual(["videos"], parse_qs(details_url.query)["append_to_response"])

    def test_stable_image_token_is_repeatable_for_imdb_id(self):
        self.assertEqual(stable_image_token("tt2872718"), stable_image_token("tt2872718"))
        self.assertEqual(30, len(stable_image_token("tt2872718")))

    def test_enrich_candidates_adds_tmdb_fields_and_skips_missing_posters(self):
        rows = [
            {
                "id": "2872718",
                "imdb_id": "tt2872718",
                "movie_type": "MOVIE",
                "primary_title": "Nightcrawler",
                "original_title": "Nightcrawler",
                "adult": "0",
                "start_year": "2014",
                "end_year": "\\N",
                "runtime_minutes": "117",
                "movie_genre": "16409",
                "imdb_rating": "7.8",
                "imdb_rating_count": "700000",
            },
            {
                "id": "1",
                "imdb_id": "tt0000001",
                "movie_type": "MOVIE",
                "primary_title": "No Poster",
                "original_title": "No Poster",
                "adult": "0",
                "start_year": "1900",
                "end_year": "\\N",
                "runtime_minutes": "80",
                "movie_genre": "1",
                "imdb_rating": "5.0",
                "imdb_rating_count": "1",
            },
        ]

        class Client(FakeTmdbClient):
            def find_by_imdb_id(self, imdb_id: str) -> dict:
                if imdb_id == "tt0000001":
                    return {"movie_results": [{"id": 1}]}
                return super().find_by_imdb_id(imdb_id)

            def get_movie_details(self, movie_id: int) -> dict:
                if movie_id == 1:
                    return {"id": 1, "overview": "", "poster_path": None}
                return super().get_movie_details(movie_id)

        enriched = enrich_candidates(rows, Client())

        self.assertEqual(1, len(enriched))
        self.assertEqual("tt2872718", enriched[0]["imdb_id"])
        self.assertEqual("242582", enriched[0]["tmdb_id"])
        self.assertEqual("/poster.jpg", enriched[0]["poster_path"])
        self.assertEqual("A driven crime journalist crosses lines.", enriched[0]["description"])
        self.assertEqual("/backdrop.jpg", enriched[0]["backdrop_path"])
        self.assertEqual("officialTrailer", enriched[0]["trailer_youtube_key"])
        self.assertEqual(stable_image_token("tt2872718", "poster"), enriched[0]["poster_image_token"])
        self.assertEqual(stable_image_token("tt2872718", "backdrop"), enriched[0]["backdrop_image_token"])

    def test_enrichment_reuses_existing_tmdb_id(self):
        client = FakeTmdbClient()
        rows = [
            {
                "id": "2872718",
                "imdb_id": "tt2872718",
                "movie_type": "MOVIE",
                "primary_title": "Nightcrawler",
                "original_title": "Nightcrawler",
                "adult": "0",
                "start_year": "2014",
                "end_year": "\\N",
                "runtime_minutes": "117",
                "movie_genre": "16409",
                "imdb_rating": "7.8",
                "imdb_rating_count": "700000",
                "tmdb_id": "242582",
            }
        ]

        enriched = enrich_candidates(rows, client)

        self.assertEqual([], client.calls)
        self.assertEqual([242582], client.detail_calls)
        self.assertEqual("officialTrailer", enriched[0]["trailer_youtube_key"])

    def test_parallel_enrichment_preserves_input_order(self):
        rows = [
            {
                "id": str(movie_id),
                "imdb_id": f"tt{movie_id:07d}",
                "movie_type": "MOVIE",
                "primary_title": f"Movie {movie_id}",
                "original_title": f"Movie {movie_id}",
                "adult": "0",
                "start_year": "2014",
                "end_year": "\\N",
                "runtime_minutes": "117",
                "movie_genre": "16409",
                "imdb_rating": "7.8",
                "imdb_rating_count": "700000",
                "tmdb_id": str(movie_id),
            }
            for movie_id in (3, 1, 2)
        ]
        progress = EnrichmentProgress(total=len(rows), log_every=10)

        enriched = enrich_candidates_parallel(
            rows,
            FakeTmdbClient,
            workers=2,
            progress=progress,
            logger=lambda _message: None,
        )

        self.assertEqual(["3", "1", "2"], [row["id"] for row in enriched])
        self.assertEqual(3, progress.processed)
        self.assertEqual(3, progress.enriched)

    def test_write_enriched_movies_uses_database_and_manifest_columns(self):
        with tempfile.TemporaryDirectory() as tmp_dir:
            output_path = Path(tmp_dir) / "movie_enriched.csv"
            rows = enrich_candidates(
                [
                    {
                        "id": "2872718",
                        "imdb_id": "tt2872718",
                        "movie_type": "MOVIE",
                        "primary_title": "Nightcrawler",
                        "original_title": "Nightcrawler",
                        "adult": "0",
                        "start_year": "2014",
                        "end_year": "\\N",
                        "runtime_minutes": "117",
                        "movie_genre": "16409",
                        "imdb_rating": "7.8",
                        "imdb_rating_count": "700000",
                    }
                ],
                FakeTmdbClient(),
            )

            write_enriched_movies(rows, output_path)

            with output_path.open(encoding="utf-8", newline="") as output_file:
                written = list(csv.DictReader(output_file))

        self.assertEqual("tt2872718", written[0]["imdb_id"])
        self.assertEqual("242582", written[0]["tmdb_id"])
        self.assertEqual("/poster.jpg", written[0]["poster_path"])
        self.assertEqual("/backdrop.jpg", written[0]["backdrop_path"])
        self.assertEqual("officialTrailer", written[0]["trailer_youtube_key"])
        self.assertIn("description", written[0])
        self.assertIn("poster_image_token", written[0])
        self.assertIn("backdrop_image_token", written[0])

    def test_enrichment_uses_cached_tmdb_response(self):
        with tempfile.TemporaryDirectory() as tmp_dir:
            cache_dir = Path(tmp_dir)
            (cache_dir / "tt2872718.json").write_text(
                json.dumps({"movie_results": [{"id": 1}]}), encoding="utf-8"
            )
            (cache_dir / "movie-1.json").write_text(
                json.dumps(
                    {
                        "id": 1,
                        "overview": "Cached overview",
                        "poster_path": "/cached.jpg",
                        "backdrop_path": None,
                        "videos": {"results": []},
                    }
                ),
                encoding="utf-8",
            )

            client = TmdbClient(api_key="unused", cache_dir=cache_dir)
            rows = [
                {
                    "id": "2872718",
                    "imdb_id": "tt2872718",
                    "movie_type": "MOVIE",
                    "primary_title": "Nightcrawler",
                    "original_title": "Nightcrawler",
                    "adult": "0",
                    "start_year": "2014",
                    "end_year": "\\N",
                    "runtime_minutes": "117",
                    "movie_genre": "16409",
                    "imdb_rating": "7.8",
                    "imdb_rating_count": "700000",
                }
            ]

            enriched = enrich_candidates(rows, client)

        self.assertEqual("/cached.jpg", enriched[0]["poster_path"])
        self.assertEqual("cache", client.last_status)

    def test_enrichment_progress_counts_outcomes(self):
        rows = [
            {
                "id": "2872718",
                "imdb_id": "tt2872718",
                "movie_type": "MOVIE",
                "primary_title": "Nightcrawler",
                "original_title": "Nightcrawler",
                "adult": "0",
                "start_year": "2014",
                "end_year": "\\N",
                "runtime_minutes": "117",
                "movie_genre": "16409",
                "imdb_rating": "7.8",
                "imdb_rating_count": "700000",
            },
            {
                "id": "1",
                "imdb_id": "tt0000001",
                "movie_type": "MOVIE",
                "primary_title": "No Match",
                "original_title": "No Match",
                "adult": "0",
                "start_year": "1900",
                "end_year": "\\N",
                "runtime_minutes": "80",
                "movie_genre": "1",
                "imdb_rating": "5.0",
                "imdb_rating_count": "1",
            },
            {
                "id": "2",
                "imdb_id": "tt0000002",
                "movie_type": "MOVIE",
                "primary_title": "No Poster",
                "original_title": "No Poster",
                "adult": "0",
                "start_year": "1901",
                "end_year": "\\N",
                "runtime_minutes": "81",
                "movie_genre": "1",
                "imdb_rating": "5.0",
                "imdb_rating_count": "1",
            },
        ]

        class Client(FakeTmdbClient):
            def find_by_imdb_id(self, imdb_id: str) -> dict:
                if imdb_id == "tt0000001":
                    return {"movie_results": []}
                if imdb_id == "tt0000002":
                    return {"movie_results": [{"id": 2}]}
                return super().find_by_imdb_id(imdb_id)

            def get_movie_details(self, movie_id: int) -> dict:
                if movie_id == 2:
                    return {"id": 2, "poster_path": None}
                return super().get_movie_details(movie_id)

        progress = EnrichmentProgress(total=len(rows), log_every=2)
        messages = []
        enriched = enrich_candidates(rows, Client(), progress=progress, logger=messages.append)

        self.assertEqual(1, len(enriched))
        self.assertEqual(3, progress.processed)
        self.assertEqual(1, progress.enriched)
        self.assertEqual(1, progress.skipped_no_match)
        self.assertEqual(1, progress.skipped_no_poster)
        self.assertEqual(2, len(messages))
        self.assertIn("[2/3]", messages[0])
        self.assertIn("enriched=1", messages[0])

    def test_write_report_records_enrichment_summary(self):
        with tempfile.TemporaryDirectory() as tmp_dir:
            report_path = Path(tmp_dir) / "movie_enrichment_report.json"
            progress = EnrichmentProgress(total=3, log_every=100)
            progress.processed = 3
            progress.enriched = 2
            progress.skipped_no_match = 1
            progress.cache_hits = 2
            progress.api_fetches = 1

            write_report(progress, report_path)

            report = json.loads(report_path.read_text(encoding="utf-8"))

        self.assertEqual(3, report["total"])
        self.assertEqual(2, report["enriched"])
        self.assertEqual(1, report["skipped_no_match"])
        self.assertEqual(2, report["cache_hits"])
        self.assertEqual(1, report["api_fetches"])


if __name__ == "__main__":
    unittest.main()

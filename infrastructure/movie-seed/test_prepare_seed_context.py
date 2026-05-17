import csv
import sys
import tempfile
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).parent / "runtime"))

from prepare_seed_context import prepare_seed_context


class PrepareSeedContextTest(unittest.TestCase):
    def test_light_context_keeps_first_250_rows_and_matching_assets(self):
        with tempfile.TemporaryDirectory() as tmp_dir:
            root = Path(tmp_dir)
            source = root / "source"
            output = root / "context"
            (source / "processed/movies/posters").mkdir(parents=True)
            (source / "processed/movies/backdrops").mkdir(parents=True)
            csv_path = source / "movie_enriched.csv"
            write_enriched_csv(csv_path, row_count=251)
            (source / "processed/movies/posters/poster-000_size_120x180.webp").write_text(
                "poster"
            )
            (source / "processed/movies/posters/poster-250_size_120x180.webp").write_text(
                "poster"
            )
            (
                source / "processed/movies/backdrops/backdrop-000_size_780x439.webp"
            ).write_text("backdrop")
            (
                source / "processed/movies/backdrops/backdrop-250_size_780x439.webp"
            ).write_text("backdrop")

            prepare_seed_context(source, output, profile="light", limit=250)

            with (output / "seed/movie_enriched.csv").open(
                encoding="utf-8", newline=""
            ) as file:
                rows = list(csv.DictReader(file))

            self.assertEqual(250, len(rows))
            self.assertTrue(
                (
                    output / "seed/media/movies/posters/poster-000_size_120x180.webp"
                ).exists()
            )
            self.assertFalse(
                (
                    output / "seed/media/movies/posters/poster-250_size_120x180.webp"
                ).exists()
            )
            self.assertTrue(
                (
                    output
                    / "seed/media/movies/backdrops/backdrop-000_size_780x439.webp"
                ).exists()
            )
            self.assertFalse(
                (
                    output
                    / "seed/media/movies/backdrops/backdrop-250_size_780x439.webp"
                ).exists()
            )


def write_enriched_csv(path: Path, row_count: int) -> None:
    fields = [
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
        "tmdb_id",
        "description",
        "poster_path",
        "backdrop_path",
        "trailer_youtube_key",
        "poster_image_token",
        "backdrop_image_token",
    ]
    path.parent.mkdir(parents=True, exist_ok=True)
    with path.open("w", encoding="utf-8", newline="") as file:
        writer = csv.DictWriter(file, fieldnames=fields)
        writer.writeheader()
        for index in range(row_count):
            writer.writerow(
                {
                    "id": str(index + 1),
                    "imdb_id": f"tt{index:07d}",
                    "movie_type": "MOVIE",
                    "primary_title": f"Movie {index}",
                    "original_title": f"Movie {index}",
                    "adult": "0",
                    "start_year": "2000",
                    "end_year": "\\N",
                    "runtime_minutes": "100",
                    "movie_genre": "1",
                    "imdb_rating": "7.0",
                    "imdb_rating_count": "1000",
                    "tmdb_id": str(index + 1000),
                    "description": "Description",
                    "poster_path": "/poster.jpg",
                    "backdrop_path": "/backdrop.jpg",
                    "trailer_youtube_key": "",
                    "poster_image_token": f"poster-{index:03d}",
                    "backdrop_image_token": f"backdrop-{index:03d}",
                }
            )


if __name__ == "__main__":
    unittest.main()

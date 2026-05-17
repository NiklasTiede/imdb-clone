import csv
import gzip
import sys
import tempfile
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).parent))

from build_movie_seed import (
    build_candidates,
    convert_genres_to_bitmask,
    convert_tconst_to_movie_id,
    write_candidates,
)


def write_gzip_tsv(path: Path, rows: list[list[str]]) -> None:
    with gzip.open(path, "wt", encoding="utf-8", newline="") as file:
        writer = csv.writer(file, delimiter="\t", lineterminator="\n")
        writer.writerows(rows)


class BuildMovieSeedTest(unittest.TestCase):
    def test_converts_imdb_tconst_to_existing_movie_id(self):
        self.assertEqual(2872718, convert_tconst_to_movie_id("tt2872718"))

    def test_converts_imdb_genres_to_existing_bitmask(self):
        self.assertEqual(16409, convert_genres_to_bitmask("Crime,Drama,Thriller"))
        self.assertEqual("\\N", convert_genres_to_bitmask("\\N"))

    def test_build_candidates_filters_movies_and_sorts_by_vote_count(self):
        with tempfile.TemporaryDirectory() as tmp_dir:
            tmp_path = Path(tmp_dir)
            basics_path = tmp_path / "title.basics.tsv.gz"
            ratings_path = tmp_path / "title.ratings.tsv.gz"

            write_gzip_tsv(
                basics_path,
                [
                    [
                        "tconst",
                        "titleType",
                        "primaryTitle",
                        "originalTitle",
                        "isAdult",
                        "startYear",
                        "endYear",
                        "runtimeMinutes",
                        "genres",
                    ],
                    [
                        "tt2872718",
                        "movie",
                        "Nightcrawler",
                        "Nightcrawler",
                        "0",
                        "2014",
                        "\\N",
                        "117",
                        "Crime,Drama,Thriller",
                    ],
                    [
                        "tt0000001",
                        "short",
                        "Carmencita",
                        "Carmencita",
                        "0",
                        "1894",
                        "\\N",
                        "1",
                        "Documentary,Short",
                    ],
                    [
                        "tt9999999",
                        "movie",
                        "Adult Movie",
                        "Adult Movie",
                        "1",
                        "2024",
                        "\\N",
                        "90",
                        "Drama",
                    ],
                    [
                        "tt1201607",
                        "movie",
                        "Harry Potter and the Deathly Hallows: Part 2",
                        "Harry Potter and the Deathly Hallows: Part 2",
                        "0",
                        "2011",
                        "\\N",
                        "130",
                        "Adventure,Family,Fantasy",
                    ],
                    [
                        "tt7654321",
                        "movie",
                        "Missing Runtime",
                        "Missing Runtime",
                        "0",
                        "2021",
                        "\\N",
                        "\\N",
                        "Drama",
                    ],
                ],
            )
            write_gzip_tsv(
                ratings_path,
                [
                    ["tconst", "averageRating", "numVotes"],
                    ["tt2872718", "7.8", "700000"],
                    ["tt0000001", "5.7", "2200"],
                    ["tt9999999", "4.0", "900000"],
                    ["tt1201607", "8.1", "800000"],
                    ["tt7654321", "6.4", "600000"],
                ],
            )

            candidates = build_candidates(basics_path, ratings_path, limit=2)

        self.assertEqual(
            ["tt1201607", "tt2872718"],
            [candidate.imdb_id for candidate in candidates],
        )
        self.assertEqual(1201607, candidates[0].id)
        self.assertEqual("MOVIE", candidates[0].movie_type)
        self.assertEqual(800000, candidates[0].imdb_rating_count)
        self.assertEqual(16409, candidates[1].movie_genre)

    def test_write_candidates_uses_database_import_columns(self):
        with tempfile.TemporaryDirectory() as tmp_dir:
            tmp_path = Path(tmp_dir)
            basics_path = tmp_path / "title.basics.tsv.gz"
            ratings_path = tmp_path / "title.ratings.tsv.gz"
            output_path = tmp_path / "movie_candidates.csv"
            write_gzip_tsv(
                basics_path,
                [
                    [
                        "tconst",
                        "titleType",
                        "primaryTitle",
                        "originalTitle",
                        "isAdult",
                        "startYear",
                        "endYear",
                        "runtimeMinutes",
                        "genres",
                    ],
                    [
                        "tt2872718",
                        "movie",
                        "Nightcrawler",
                        "Nightcrawler",
                        "0",
                        "2014",
                        "\\N",
                        "117",
                        "Crime,Drama,Thriller",
                    ],
                ],
            )
            write_gzip_tsv(
                ratings_path,
                [
                    ["tconst", "averageRating", "numVotes"],
                    ["tt2872718", "7.8", "700000"],
                ],
            )

            write_candidates(build_candidates(basics_path, ratings_path, limit=1), output_path)

            with output_path.open(encoding="utf-8", newline="") as output_file:
                rows = list(csv.DictReader(output_file))

        self.assertEqual(
            [
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
            ],
            list(rows[0].keys()),
        )
        self.assertEqual("2872718", rows[0]["id"])
        self.assertEqual("tt2872718", rows[0]["imdb_id"])
        self.assertEqual("Nightcrawler", rows[0]["primary_title"])


if __name__ == "__main__":
    unittest.main()

import os
import sys
import unittest
from pathlib import Path
from unittest.mock import patch

sys.path.insert(0, str(Path(__file__).parent / "runtime"))

from seed import Config, build_media_key, normalize_db_value, upsert_movie_sql


class SeedRuntimeTest(unittest.TestCase):
    def test_config_reads_defaults_and_required_values(self):
        env = {
            "POSTGRES_HOST": "postgres",
            "POSTGRES_DB": "movie_db",
            "POSTGRES_USER": "myroot",
            "POSTGRES_PASSWORD": "secret",
            "RUSTFS_ENDPOINT": "http://rustfs:9000",
            "RUSTFS_ACCESS_KEY": "ROOTNAME",
            "RUSTFS_SECRET_KEY": "CHANGEME123",
        }
        with patch.dict(os.environ, env, clear=True):
            config = Config.from_env()

        self.assertEqual("5432", config.postgres_port)
        self.assertEqual("imdb-clone", config.rustfs_bucket)
        self.assertEqual("local", config.seed_version)

    def test_media_keys_use_poster_and_backdrop_prefixes(self):
        self.assertEqual(
            "movies/posters/poster-token_size_600x900.webp",
            build_media_key("posters", "poster-token_size_600x900.webp"),
        )
        self.assertEqual(
            "movies/backdrops/backdrop-token_size_1280x720.webp",
            build_media_key("backdrops", "backdrop-token_size_1280x720.webp"),
        )

    def test_db_value_normalization_converts_imdb_null_marker(self):
        self.assertIsNone(normalize_db_value("\\N"))
        self.assertEqual("The Matrix", normalize_db_value("The Matrix"))

    def test_upsert_sql_preserves_user_rating_fields(self):
        sql = upsert_movie_sql()

        self.assertIn("on conflict (imdb_id) do update", sql.lower())
        self.assertNotIn("rating_count = excluded.rating_count", sql.lower())
        self.assertNotIn("rating_sum = excluded.rating_sum", sql.lower())


if __name__ == "__main__":
    unittest.main()

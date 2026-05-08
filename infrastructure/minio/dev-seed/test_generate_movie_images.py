import tempfile
import unittest
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).parent))

from generate_movie_images import (
    DEFAULT_MANIFEST,
    IMAGE_SIZES,
    load_manifest,
    target_image_paths,
)


class GenerateMovieImagesTest(unittest.TestCase):
    def test_load_manifest_reads_required_movie_image_data(self):
        with tempfile.TemporaryDirectory() as tmp_dir:
            manifest = Path(tmp_dir) / "movies.csv"
            manifest.write_text(
                "movie_id,imdb_id,title,poster_path,image_url_token\n"
                "2872718,tt2872718,Nightcrawler,/poster.jpg,token-one\n",
                encoding="utf-8",
            )

            movies = load_manifest(manifest)

        self.assertEqual(1, len(movies))
        self.assertEqual("tt2872718", movies[0].imdb_id)
        self.assertEqual("/poster.jpg", movies[0].poster_path)
        self.assertEqual("token-one", movies[0].image_url_token)

    def test_target_image_paths_follow_frontend_minio_convention(self):
        paths = target_image_paths(Path("movies"), "poster-token")

        self.assertEqual(
            {
                "600x900": Path("movies/poster-token_size_600x900.jpg"),
                "120x180": Path("movies/poster-token_size_120x180.jpg"),
            },
            paths,
        )

    def test_default_manifest_covers_local_init_movies(self):
        movies = load_manifest(DEFAULT_MANIFEST)

        self.assertEqual(21, len(movies))
        self.assertEqual(IMAGE_SIZES, ((600, 900), (120, 180)))
        self.assertEqual(
            {
                "9BGAIYNfdY90aIkV66dIJ6Olee7JGn",
                "vviPMlGImqliPsQSfNHMjodNyfqCip",
                "XFszo4K1Dx46CICcWWIsRDylBBdoIG",
                "GMhJaADtgT6sWNFwTckJoRlEwzUXVL",
                "kp1LDdvQp7O826hLJPRaKINZwnzNo5",
                "Ytfkf2FbKoMSM9H3SF0SYbvNiOuNU8",
                "acHu5vvpRpyzfvzNbUQtdab6D2L4WF",
                "bdtSw7hcj3uelSR4KjpbIROWnDPver",
                "DjDROnwbTUjH5Rh3qqZhXD3eSVeaDe",
                "2ObxIPGNeyjTASgQ2Hy9m39cnwbOhM",
                "L5Yz2AL93WrclurlAx2BicKL2wAsDs",
                "eXFkN9Iy0TzgjkMhVVrPeNYVX6eveo",
                "cIykXRDhgRNUxmcYWd9MjA07W2QXG9",
                "HcFe5cZFGVUivYvn4HiCyXo4uSDonI",
                "fQGsfDj3Bkt7Uzen76LFi2aFd9GGPw",
                "xyY8UzGYRa8DPljn7kTtAZu1ovM6it",
                "0ckteLCCRMw42ww2y7uD8gomOWEnFy",
                "XZibrHddPi9xqHk4jI0iUoByiCkOc1",
                "DoamaWHqAaGNIJVf4RuS1ORnTA1yzb",
                "RAkRPjw3JLR1VIlMwmXMkezl4MFSe2",
                "88BNOZyP06fSHIBDq8aKlSnrXfqlF8",
            },
            {movie.image_url_token for movie in movies},
        )


if __name__ == "__main__":
    unittest.main()

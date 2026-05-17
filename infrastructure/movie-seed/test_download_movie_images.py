import csv
import json
import sys
import tempfile
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).parent))

from download_movie_images import (
    ImageProgress,
    MovieImageAsset,
    build_assets,
    convert_variants,
    download_originals,
    target_paths,
    write_report,
)


class DownloadMovieImagesTest(unittest.TestCase):
    def test_build_assets_creates_poster_and_optional_backdrop_assets(self):
        rows = [
            {
                "id": "2872718",
                "imdb_id": "tt2872718",
                "primary_title": "Nightcrawler",
                "poster_path": "/poster.jpg",
                "backdrop_path": "/backdrop.jpg",
                "poster_image_token": "poster-token",
                "backdrop_image_token": "backdrop-token",
            },
            {
                "id": "1",
                "imdb_id": "tt0000001",
                "primary_title": "Poster Only",
                "poster_path": "/poster-only.jpg",
                "backdrop_path": "",
                "poster_image_token": "poster-only-token",
                "backdrop_image_token": "",
            },
        ]

        assets = build_assets(rows)

        self.assertEqual(
            ["poster-token", "backdrop-token", "poster-only-token"],
            [asset.image_token for asset in assets],
        )
        self.assertEqual(["poster", "backdrop", "poster"], [asset.image_kind for asset in assets])

    def test_target_paths_keep_originals_and_processed_variants_separate(self):
        asset = MovieImageAsset(
            imdb_id="tt2872718",
            title="Nightcrawler",
            image_kind="poster",
            tmdb_path="/poster.jpg",
            image_token="poster-token",
        )

        paths = target_paths(Path("build/movie-seed"), asset)

        self.assertEqual(
            Path("build/movie-seed/originals/posters/poster-token.jpg"),
            paths.original,
        )
        self.assertEqual(
            Path("build/movie-seed/processed/movies/posters/poster-token_size_120x180.webp"),
            paths.variants["120x180"],
        )
        self.assertEqual(
            Path("build/movie-seed/processed/movies/posters/poster-token_size_600x900.webp"),
            paths.variants["600x900"],
        )

    def test_target_paths_can_store_originals_on_external_disk(self):
        asset = MovieImageAsset(
            imdb_id="tt2872718",
            title="Nightcrawler",
            image_kind="poster",
            tmdb_path="/poster.jpg",
            image_token="poster-token",
        )

        paths = target_paths(
            Path("build/movie-seed"),
            asset,
            originals_root=Path("/Volumes/TOSHIBA EXT/movie-images"),
        )

        self.assertEqual(
            Path("/Volumes/TOSHIBA EXT/movie-images/posters/poster-token.jpg"),
            paths.original,
        )
        self.assertEqual(
            Path("build/movie-seed/processed/movies/posters/poster-token_size_300x450.webp"),
            paths.variants["300x450"],
        )

    def test_download_originals_only_writes_original_files(self):
        image_bytes = create_test_image_bytes(1200, 1800)

        def downloader(_path: str) -> bytes:
            return image_bytes

        with tempfile.TemporaryDirectory() as tmp_dir:
            root = Path(tmp_dir)
            progress = ImageProgress(total=1, log_every=1)
            asset = MovieImageAsset(
                imdb_id="tt2872718",
                title="Nightcrawler",
                image_kind="poster",
                tmdb_path="/poster.jpg",
                image_token="poster-token",
            )

            download_originals([asset], root, downloader, progress=progress, logger=lambda _: None)
            paths = target_paths(root, asset)

            self.assertTrue(paths.original.exists())
            self.assertFalse(paths.variants["120x180"].exists())
            self.assertEqual(1, progress.downloaded_originals)
            self.assertEqual(0, progress.written_variants)

    def test_convert_variants_writes_webp_from_existing_originals(self):
        image_bytes = create_test_image_bytes(1200, 1800)

        with tempfile.TemporaryDirectory() as tmp_dir:
            root = Path(tmp_dir)
            asset = MovieImageAsset(
                imdb_id="tt2872718",
                title="Nightcrawler",
                image_kind="poster",
                tmdb_path="/poster.jpg",
                image_token="poster-token",
            )
            paths = target_paths(root, asset)
            paths.original.parent.mkdir(parents=True)
            paths.original.write_bytes(image_bytes)
            progress = ImageProgress(total=1, log_every=1)

            convert_variants([asset], root, progress=progress, logger=lambda _: None)

            self.assertTrue(paths.variants["120x180"].exists())
            self.assertTrue(paths.variants["300x450"].exists())
            self.assertTrue(paths.variants["600x900"].exists())

        self.assertEqual(3, progress.written_variants)

    def test_split_steps_skip_existing_files(self):
        image_bytes = create_test_image_bytes(1200, 1800)

        def downloader(_path: str) -> bytes:
            return image_bytes

        with tempfile.TemporaryDirectory() as tmp_dir:
            root = Path(tmp_dir)
            asset = MovieImageAsset(
                imdb_id="tt2872718",
                title="Nightcrawler",
                image_kind="poster",
                tmdb_path="/poster.jpg",
                image_token="poster-token",
            )
            first_download = ImageProgress(total=1, log_every=1)
            second_download = ImageProgress(total=1, log_every=1)
            first_convert = ImageProgress(total=1, log_every=1)
            second_convert = ImageProgress(total=1, log_every=1)

            download_originals([asset], root, downloader, progress=first_download, logger=lambda _: None)
            download_originals([asset], root, downloader, progress=second_download, logger=lambda _: None)
            convert_variants([asset], root, progress=first_convert, logger=lambda _: None)
            convert_variants([asset], root, progress=second_convert, logger=lambda _: None)

        self.assertEqual(1, second_download.skipped_existing)
        self.assertEqual(0, second_download.downloaded_originals)
        self.assertEqual(1, second_convert.skipped_existing)
        self.assertEqual(0, second_convert.written_variants)

    def test_write_report_records_image_summary(self):
        with tempfile.TemporaryDirectory() as tmp_dir:
            report_path = Path(tmp_dir) / "movie_image_report.json"
            progress = ImageProgress(total=2, log_every=100)
            progress.processed = 2
            progress.downloaded_originals = 1
            progress.reused_originals = 1
            progress.written_variants = 5
            progress.skipped_existing = 1

            write_report(progress, report_path)

            report = json.loads(report_path.read_text(encoding="utf-8"))

        self.assertEqual(2, report["total"])
        self.assertEqual(1, report["downloaded_originals"])
        self.assertEqual(1, report["reused_originals"])
        self.assertEqual(5, report["written_variants"])
        self.assertEqual(1, report["skipped_existing"])


def create_test_image_bytes(width: int, height: int) -> bytes:
    try:
        from PIL import Image
    except ImportError as exc:
        raise unittest.SkipTest("Pillow is required for image pipeline tests") from exc

    from io import BytesIO

    buffer = BytesIO()
    Image.new("RGB", (width, height), color=(40, 80, 120)).save(buffer, format="JPEG")
    return buffer.getvalue()


if __name__ == "__main__":
    unittest.main()

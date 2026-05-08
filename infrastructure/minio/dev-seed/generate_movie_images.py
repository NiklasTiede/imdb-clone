#!/usr/bin/env python3
import argparse
import csv
from dataclasses import dataclass
from io import BytesIO
from pathlib import Path
from urllib.request import Request, urlopen


BASE_POSTER_URL = "https://image.tmdb.org/t/p/original"
IMAGE_SIZES = ((600, 900), (120, 180))
DEFAULT_MANIFEST = Path(__file__).with_name("movies.csv")
DEFAULT_OUTPUT_DIR = Path(__file__).with_name("movies")
REQUIRED_COLUMNS = {"movie_id", "imdb_id", "title", "poster_path", "image_url_token"}


@dataclass(frozen=True)
class MovieImageSeed:
    movie_id: str
    imdb_id: str
    title: str
    poster_path: str
    image_url_token: str


def load_manifest(manifest_path: Path) -> list[MovieImageSeed]:
    with manifest_path.open(newline="", encoding="utf-8") as manifest_file:
        reader = csv.DictReader(manifest_file)
        if not reader.fieldnames or REQUIRED_COLUMNS - set(reader.fieldnames):
            missing = REQUIRED_COLUMNS - set(reader.fieldnames or [])
            raise ValueError(f"Manifest is missing required columns: {sorted(missing)}")

        movies = [
            MovieImageSeed(
                movie_id=row["movie_id"].strip(),
                imdb_id=row["imdb_id"].strip(),
                title=row["title"].strip(),
                poster_path=row["poster_path"].strip(),
                image_url_token=row["image_url_token"].strip(),
            )
            for row in reader
        ]

    validate_manifest(movies)
    return movies


def validate_manifest(movies: list[MovieImageSeed]) -> None:
    tokens = [movie.image_url_token for movie in movies]
    duplicate_tokens = {token for token in tokens if tokens.count(token) > 1}
    if duplicate_tokens:
        raise ValueError(f"Manifest has duplicate image tokens: {sorted(duplicate_tokens)}")

    invalid_rows = [
        movie.imdb_id
        for movie in movies
        if not movie.poster_path.startswith("/") or not movie.image_url_token
    ]
    if invalid_rows:
        raise ValueError(f"Manifest has invalid poster paths or tokens: {invalid_rows}")


def target_image_paths(output_dir: Path, image_url_token: str) -> dict[str, Path]:
    return {
        f"{width}x{height}": output_dir / f"{image_url_token}_size_{width}x{height}.jpg"
        for width, height in IMAGE_SIZES
    }


def download_poster(movie: MovieImageSeed) -> bytes:
    request = Request(
        f"{BASE_POSTER_URL}{movie.poster_path}",
        headers={"User-Agent": "imdb-clone-dev-seed/1.0"},
    )
    with urlopen(request, timeout=30) as response:
        return response.read()


def require_pillow():
    try:
        from PIL import Image
    except ImportError as exc:
        raise SystemExit(
            "Pillow is required. Install it with: python3 -m pip install Pillow"
        ) from exc
    return Image


def crop_to_aspect_ratio(image, target_width: int, target_height: int):
    target_ratio = target_width / target_height
    source_ratio = image.width / image.height

    if source_ratio > target_ratio:
        crop_width = int(image.height * target_ratio)
        left = (image.width - crop_width) // 2
        box = (left, 0, left + crop_width, image.height)
    else:
        crop_height = int(image.width / target_ratio)
        top = (image.height - crop_height) // 2
        box = (0, top, image.width, top + crop_height)

    return image.crop(box)


def write_image_variants(poster: bytes, output_dir: Path, image_url_token: str) -> None:
    image_module = require_pillow()
    output_dir.mkdir(parents=True, exist_ok=True)

    with image_module.open(BytesIO(poster)) as source:
        rgb_source = source.convert("RGB")
        for width, height in IMAGE_SIZES:
            cropped = crop_to_aspect_ratio(rgb_source, width, height)
            resized = cropped.resize((width, height), image_module.Resampling.LANCZOS)
            image_path = target_image_paths(output_dir, image_url_token)[f"{width}x{height}"]
            resized.save(image_path, format="JPEG", quality=90, optimize=True)


def generate_images(manifest_path: Path, output_dir: Path, skip_existing: bool) -> None:
    for movie in load_manifest(manifest_path):
        paths = target_image_paths(output_dir, movie.image_url_token)
        if skip_existing and all(path.exists() for path in paths.values()):
            print(f"Skipping {movie.title}: image variants already exist")
            continue

        print(f"Generating images for {movie.title} ({movie.imdb_id})")
        write_image_variants(download_poster(movie), output_dir, movie.image_url_token)


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="Download and process lightweight dev movie posters for MinIO."
    )
    parser.add_argument(
        "--manifest",
        type=Path,
        default=DEFAULT_MANIFEST,
        help=f"CSV manifest path. Default: {DEFAULT_MANIFEST}",
    )
    parser.add_argument(
        "--output-dir",
        type=Path,
        default=DEFAULT_OUTPUT_DIR,
        help=f"Directory for generated MinIO movie objects. Default: {DEFAULT_OUTPUT_DIR}",
    )
    parser.add_argument(
        "--overwrite",
        action="store_true",
        help="Regenerate image files even when both target variants already exist.",
    )
    return parser.parse_args()


def main() -> None:
    args = parse_args()
    generate_images(args.manifest, args.output_dir, skip_existing=not args.overwrite)


if __name__ == "__main__":
    main()

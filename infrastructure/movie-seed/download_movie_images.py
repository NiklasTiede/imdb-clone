#!/usr/bin/env python3
import argparse
import csv
import json
import time
from dataclasses import asdict, dataclass
from io import BytesIO
from pathlib import Path
from urllib.request import Request, urlopen


BASE_IMAGE_URL = "https://image.tmdb.org/t/p/original"
DEFAULT_INPUT = Path("build/movie-seed/movie_enriched.csv")
DEFAULT_OUTPUT_ROOT = Path("build/movie-seed")
DEFAULT_REPORT = Path("build/movie-seed/movie_image_report.json")

VARIANT_SIZES = {
    "poster": ((120, 180), (300, 450), (600, 900)),
    "backdrop": ((780, 439), (1280, 720)),
}


@dataclass(frozen=True)
class MovieImageAsset:
    imdb_id: str
    title: str
    image_kind: str
    tmdb_path: str
    image_token: str


@dataclass(frozen=True)
class ImageTargetPaths:
    original: Path
    variants: dict[str, Path]


@dataclass
class ImageProgress:
    total: int
    log_every: int
    processed: int = 0
    downloaded_originals: int = 0
    reused_originals: int = 0
    written_variants: int = 0
    reused_variants: int = 0
    skipped_existing: int = 0
    failed: int = 0

    def should_log(self) -> bool:
        return self.processed == self.total or self.processed % self.log_every == 0

    def progress_line(self, asset: MovieImageAsset) -> str:
        return (
            f"[{self.processed}/{self.total}] "
            f"downloaded_originals={self.downloaded_originals} "
            f"reused_originals={self.reused_originals} "
            f"written_variants={self.written_variants} "
            f"reused_variants={self.reused_variants} "
            f"skipped_existing={self.skipped_existing} "
            f"failed={self.failed} "
            f"current={asset.imdb_id} {asset.image_kind} {asset.title}"
        )


def require_pillow():
    try:
        from PIL import Image
    except ImportError as exc:
        raise SystemExit(
            "Pillow is required. Install it with: python3 -m pip install Pillow"
        ) from exc
    return Image


def load_enriched_movies(input_path: Path) -> list[dict[str, str]]:
    with input_path.open(encoding="utf-8", newline="") as input_file:
        return list(csv.DictReader(input_file))


def build_assets(rows: list[dict[str, str]]) -> list[MovieImageAsset]:
    assets = []
    for row in rows:
        title = row.get("primary_title", "")
        poster_path = row.get("poster_path", "")
        poster_token = row.get("poster_image_token", "")
        if poster_path and poster_token:
            assets.append(
                MovieImageAsset(
                    imdb_id=row["imdb_id"],
                    title=title,
                    image_kind="poster",
                    tmdb_path=poster_path,
                    image_token=poster_token,
                )
            )

        backdrop_path = row.get("backdrop_path", "")
        backdrop_token = row.get("backdrop_image_token", "")
        if backdrop_path and backdrop_token:
            assets.append(
                MovieImageAsset(
                    imdb_id=row["imdb_id"],
                    title=title,
                    image_kind="backdrop",
                    tmdb_path=backdrop_path,
                    image_token=backdrop_token,
                )
            )

    return assets


def target_paths(
    output_root: Path,
    asset: MovieImageAsset,
    originals_root: Path | None = None,
) -> ImageTargetPaths:
    original_base = originals_root if originals_root else output_root / "originals"
    original_dir = original_base / f"{asset.image_kind}s"
    processed_dir = output_root / "processed" / "movies" / f"{asset.image_kind}s"
    variants = {
        f"{width}x{height}": processed_dir / f"{asset.image_token}_size_{width}x{height}.webp"
        for width, height in VARIANT_SIZES[asset.image_kind]
    }
    return ImageTargetPaths(
        original=original_dir / f"{asset.image_token}.jpg",
        variants=variants,
    )


def download_original(tmdb_path: str) -> bytes:
    request = Request(
        f"{BASE_IMAGE_URL}{tmdb_path}",
        headers={"User-Agent": "imdb-clone-movie-seed/1.0"},
    )
    with urlopen(request, timeout=30) as response:
        return response.read()


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


def write_variants(original_path: Path, variant_paths: dict[str, Path], overwrite: bool = False) -> int:
    image_module = require_pillow()
    written = 0
    with image_module.open(original_path) as source:
        rgb_source = source.convert("RGB")
        for size, variant_path in variant_paths.items():
            if variant_path.exists() and not overwrite:
                continue
            if overwrite:
                variant_path.unlink(missing_ok=True)

            width, height = [int(value) for value in size.split("x")]
            cropped = crop_to_aspect_ratio(rgb_source, width, height)
            resized = cropped.resize((width, height), image_module.Resampling.LANCZOS)
            variant_path.parent.mkdir(parents=True, exist_ok=True)
            resized.save(variant_path, format="WEBP", quality=84, method=6)
            written += 1
    return written


def download_originals(
    assets: list[MovieImageAsset],
    output_root: Path,
    downloader=download_original,
    progress: ImageProgress | None = None,
    logger=print,
    overwrite: bool = False,
    sleep_seconds: float = 0.0,
    originals_root: Path | None = None,
) -> None:
    for asset in assets:
        paths = target_paths(output_root, asset, originals_root=originals_root)
        if not overwrite and paths.original.exists():
            if progress:
                progress.skipped_existing += 1
                progress.processed += 1
                if progress.should_log():
                    logger(progress.progress_line(asset))
            continue

        try:
            paths.original.parent.mkdir(parents=True, exist_ok=True)
            paths.original.write_bytes(downloader(asset.tmdb_path))
            if progress:
                progress.downloaded_originals += 1
            if sleep_seconds:
                time.sleep(sleep_seconds)
        except Exception as exc:
            if progress:
                progress.failed += 1
            logger(f"Failed {asset.imdb_id} {asset.image_kind} {asset.title}: {exc}")
        finally:
            if progress:
                progress.processed += 1
                if progress.should_log():
                    logger(progress.progress_line(asset))


def convert_variants(
    assets: list[MovieImageAsset],
    output_root: Path,
    progress: ImageProgress | None = None,
    logger=print,
    overwrite: bool = False,
    originals_root: Path | None = None,
) -> None:
    for asset in assets:
        paths = target_paths(output_root, asset, originals_root=originals_root)
        all_variants_exist = all(path.exists() for path in paths.variants.values())
        if not overwrite and all_variants_exist:
            if progress:
                progress.skipped_existing += 1
                progress.processed += 1
                if progress.should_log():
                    logger(progress.progress_line(asset))
            continue

        try:
            if not paths.original.exists():
                raise FileNotFoundError(f"Original image missing: {paths.original}")
            if progress:
                progress.reused_originals += 1

            written = write_variants(paths.original, paths.variants, overwrite=overwrite)
            if progress:
                progress.written_variants += written
                progress.reused_variants += len(paths.variants) - written
        except Exception as exc:
            if progress:
                progress.failed += 1
            logger(f"Failed {asset.imdb_id} {asset.image_kind} {asset.title}: {exc}")
        finally:
            if progress:
                progress.processed += 1
                if progress.should_log():
                    logger(progress.progress_line(asset))


def write_report(progress: ImageProgress, report_path: Path) -> None:
    report_path.parent.mkdir(parents=True, exist_ok=True)
    report_path.write_text(
        json.dumps(asdict(progress), indent=2, sort_keys=True),
        encoding="utf-8",
    )


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="Download TMDB movie image originals or convert them into local variants."
    )
    parser.add_argument(
        "command",
        choices=("download-originals", "convert-variants"),
        help="Choose whether to download originals or generate WebP variants.",
    )
    parser.add_argument("--input", type=Path, default=DEFAULT_INPUT)
    parser.add_argument("--output-root", type=Path, default=DEFAULT_OUTPUT_ROOT)
    parser.add_argument(
        "--originals-root",
        type=Path,
        help="Directory for original downloaded TMDB images. Defaults to OUTPUT_ROOT/originals.",
    )
    parser.add_argument("--report", type=Path, default=DEFAULT_REPORT)
    parser.add_argument("--overwrite", action="store_true")
    parser.add_argument("--sleep-seconds", type=float, default=0.0)
    parser.add_argument("--log-every", type=int, default=100)
    return parser.parse_args()


def main() -> None:
    args = parse_args()
    assets = build_assets(load_enriched_movies(args.input))
    progress = ImageProgress(total=len(assets), log_every=args.log_every)
    if args.command == "download-originals":
        download_originals(
            assets,
            args.output_root,
            progress=progress,
            overwrite=args.overwrite,
            sleep_seconds=args.sleep_seconds,
            originals_root=args.originals_root,
        )
    else:
        convert_variants(
            assets,
            args.output_root,
            progress=progress,
            overwrite=args.overwrite,
            originals_root=args.originals_root,
        )
    write_report(progress, args.report)
    print(f"Wrote image report to {args.report}")


if __name__ == "__main__":
    main()

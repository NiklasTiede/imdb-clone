# Versioned Seed Images Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build versioned light/full Docker seed images that idempotently load enriched movie data into PostgreSQL and WebP movie media into RustFS.

**Architecture:** A Python seeder CLI is packaged with profile-specific seed data into separate light/full Docker image tags. The app is adjusted to request seeded WebP movie poster/backdrop paths while keeping profile photos JPG and allowing JPG fallback for manually uploaded movie posters.

**Tech Stack:** Python 3.14, psycopg 3, boto3, Docker, Docker Compose, PostgreSQL 18, RustFS S3 API, Spring Boot 4, React 19, TypeScript.

---

## File Structure

Create:

- `infrastructure/movie-seed/runtime/requirements.txt` - Python runtime dependencies for the seed image.
- `infrastructure/movie-seed/runtime/seed.py` - Entrypoint CLI for `all`, `db`, and `media`.
- `infrastructure/movie-seed/runtime/prepare_seed_context.py` - Builds light/full Docker contexts from `build/movie-seed`.
- `infrastructure/movie-seed/runtime/Dockerfile` - Dockerfile copied into prepared contexts.
- `infrastructure/movie-seed/test_prepare_seed_context.py` - Unit tests for CSV slicing and asset copying.
- `infrastructure/movie-seed/test_seed_runtime.py` - Unit tests for env parsing, object keys, and SQL shape.
- `infrastructure/kubernetes/seed-job.example.yaml` - k3s Job template using the same env contract.

Modify:

- `frontend/src/shared/media/imageUrls.ts` - Add WebP poster/backdrop URL helpers and JPG fallback helper.
- `frontend/src/shared/media/imageUrls.test.ts` - Assert WebP poster/backdrop URLs and JPG profile URLs.
- `frontend/src/shared/media/PosterImage.tsx` - Retry JPG poster URL on WebP load failure.
- `frontend/src/shared/media/index.ts` - Export new helpers/types.
- `src/main/java/com/thecodinglab/imdbclone/media/internal/images/MovieImageConstants.java` - Move manual movie uploads under `movies/posters/`.
- `src/test/java/com/thecodinglab/imdbclone/media/MediaServiceIntegrationTest.java` - Update expected movie object paths.
- `Makefile` - Add build/push/run seed targets.
- `compose.yaml` - Add optional light/full one-shot seed services.
- `infrastructure/deployment/development/docker-compose.yaml` - Replace old object loader with seed services.
- `infrastructure/deployment/production/docker-compose.stateful-apps.yaml` - Replace old object loader with full seed service.
- `infrastructure/movie-seed/README.md` - Document build/push/run flow.

---

### Task 1: Frontend Movie WebP URLs

**Files:**
- Modify: `frontend/src/shared/media/imageUrls.ts`
- Modify: `frontend/src/shared/media/imageUrls.test.ts`
- Modify: `frontend/src/shared/media/index.ts`

- [ ] **Step 1: Write failing URL tests**

Replace `frontend/src/shared/media/imageUrls.test.ts` with:

```ts
import {
  getMovieBackdropImageUrl,
  getMovieImageUrl,
  getMoviePosterFallbackImageUrl,
  getMoviePosterImageUrl,
  getObjectStorageImageUrl,
  getProfileImageUrl,
  MovieBackdropImageSize,
  MoviePosterImageSize,
  ObjectStorageImageSize,
} from "./imageUrls";
import { vi } from "vitest";

describe("movie image URL helpers", () => {
  afterEach(() => {
    vi.unstubAllEnvs();
  });

  it("builds WebP poster URLs from the configured object storage address", () => {
    vi.stubEnv("VITE_IMDB_CLONE_OBJECT_STORAGE_ADDRESS", "http://localhost:9000");

    expect(getMoviePosterImageUrl("poster-token", MoviePosterImageSize.Small)).toBe(
      "http://localhost:9000/imdb-clone/movies/posters/poster-token_size_120x180.webp",
    );
  });

  it("keeps getMovieImageUrl as a poster compatibility alias", () => {
    expect(getMovieImageUrl("poster-token", ObjectStorageImageSize.Large)).toBe(
      "http://localhost:9000/imdb-clone/movies/posters/poster-token_size_600x900.webp",
    );
    expect(getObjectStorageImageUrl("poster-token", ObjectStorageImageSize.Small)).toBe(
      "http://localhost:9000/imdb-clone/movies/posters/poster-token_size_120x180.webp",
    );
  });

  it("builds JPG poster fallback URLs for manually uploaded movie posters", () => {
    expect(getMoviePosterFallbackImageUrl("poster-token", MoviePosterImageSize.Large)).toBe(
      "http://localhost:9000/imdb-clone/movies/posters/poster-token_size_600x900.jpg",
    );
  });

  it("builds WebP backdrop URLs", () => {
    expect(getMovieBackdropImageUrl("backdrop-token", MovieBackdropImageSize.Large)).toBe(
      "http://localhost:9000/imdb-clone/movies/backdrops/backdrop-token_size_1280x720.webp",
    );
  });

  it("keeps profile photo URLs as JPG", () => {
    vi.stubEnv("VITE_IMDB_CLONE_OBJECT_STORAGE_ADDRESS", "http://localhost:9000");

    expect(getProfileImageUrl("avatar-token")).toBe(
      "http://localhost:9000/imdb-clone/profile-photos/avatar-token_size_800x800.jpg",
    );
  });
});
```

- [ ] **Step 2: Run test to verify it fails**

Run:

```bash
cd frontend && yarn test imageUrls.test.ts
```

Expected: FAIL because `getMovieBackdropImageUrl`, `MovieBackdropImageSize`, and the fallback helper do not exist yet.

- [ ] **Step 3: Implement URL helpers**

Replace `frontend/src/shared/media/imageUrls.ts` with:

```ts
export enum ObjectStorageImageSize {
  Small = "120x180",
  Large = "600x900",
  Profile = "800x800",
}

export enum MoviePosterImageSize {
  Small = "120x180",
  Medium = "300x450",
  Large = "600x900",
}

export enum MovieBackdropImageSize {
  Small = "780x439",
  Large = "1280x720",
}

const getObjectStorageHost = () =>
  import.meta.env.VITE_IMDB_CLONE_OBJECT_STORAGE_ADDRESS ??
  "http://localhost:9000";

export type MovieImageSize =
  | ObjectStorageImageSize.Small
  | ObjectStorageImageSize.Large;

const buildObjectUrl = (path: string): string =>
  `${getObjectStorageHost()}/imdb-clone/${path}`;

export const getMoviePosterImageUrl = (
  token: string,
  size: MoviePosterImageSize | MovieImageSize,
): string => buildObjectUrl(`movies/posters/${token}_size_${size}.webp`);

export const getMoviePosterFallbackImageUrl = (
  token: string,
  size: MoviePosterImageSize | MovieImageSize,
): string => buildObjectUrl(`movies/posters/${token}_size_${size}.jpg`);

export const getMovieBackdropImageUrl = (
  token: string,
  size: MovieBackdropImageSize,
): string => buildObjectUrl(`movies/backdrops/${token}_size_${size}.webp`);

export const getMovieImageUrl = (
  token: string,
  size: MovieImageSize,
): string => getMoviePosterImageUrl(token, size);

export const getProfileImageUrl = (token: string): string =>
  buildObjectUrl(
    `profile-photos/${token}_size_${ObjectStorageImageSize.Profile}.jpg`,
  );

export const getObjectStorageImageUrl = (
  token: string,
  size: MovieImageSize,
): string => getMovieImageUrl(token, size);
```

Update `frontend/src/shared/media/index.ts` exports:

```ts
export { default as PosterImage } from "./PosterImage";
export { default as ProfileAvatar } from "./ProfileAvatar";
export {
  getMovieBackdropImageUrl,
  getMovieImageUrl,
  getMoviePosterFallbackImageUrl,
  getMoviePosterImageUrl,
  getObjectStorageImageUrl,
  getProfileImageUrl,
  MovieBackdropImageSize,
  MoviePosterImageSize,
  ObjectStorageImageSize,
} from "./imageUrls";
export type { MovieImageSize } from "./imageUrls";
```

- [ ] **Step 4: Run test to verify it passes**

Run:

```bash
cd frontend && yarn test imageUrls.test.ts
```

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add frontend/src/shared/media/imageUrls.ts frontend/src/shared/media/imageUrls.test.ts frontend/src/shared/media/index.ts
git commit -m "feat(media): use WebP movie image URLs"
```

---

### Task 2: Frontend JPG Poster Fallback

**Files:**
- Modify: `frontend/src/shared/media/PosterImage.tsx`
- Create: `frontend/src/shared/media/PosterImage.test.tsx`

- [ ] **Step 1: Write failing fallback test**

Create `frontend/src/shared/media/PosterImage.test.tsx`:

```tsx
import { fireEvent, render, screen } from "@testing-library/react";
import PosterImage from "./PosterImage";
import { ObjectStorageImageSize } from "./imageUrls";

describe("PosterImage", () => {
  it("falls back to JPG when the WebP poster object is unavailable", () => {
    render(
      <PosterImage imageUrlToken="poster-token" size={ObjectStorageImageSize.Large} />,
    );

    const image = screen.getByAltText("movie poster") as HTMLImageElement;
    expect(image.src).toContain(
      "/imdb-clone/movies/posters/poster-token_size_600x900.webp",
    );

    fireEvent.error(image);

    expect(image.src).toContain(
      "/imdb-clone/movies/posters/poster-token_size_600x900.jpg",
    );
  });
});
```

- [ ] **Step 2: Run test to verify it fails**

Run:

```bash
cd frontend && yarn test PosterImage.test.tsx
```

Expected: FAIL because `PosterImage` does not switch to a fallback URL on load error.

- [ ] **Step 3: Implement fallback**

Replace `frontend/src/shared/media/PosterImage.tsx` with:

```tsx
import { CardMedia } from "@mui/material";
import type { SxProps, Theme } from "@mui/material/styles";
import { useMemo, useState } from "react";
import placeholderSearch from "../../assets/img/placeholder_search.png";
import {
  getMoviePosterFallbackImageUrl,
  getMoviePosterImageUrl,
  type MovieImageSize,
} from "./imageUrls";

type PosterImageProps = {
  imageUrlToken?: string;
  size: MovieImageSize;
  sx?: SxProps<Theme>;
};

const PosterImage = ({ imageUrlToken, size, sx }: PosterImageProps) => {
  const [useFallback, setUseFallback] = useState(false);
  const src = useMemo(() => {
    if (!imageUrlToken) {
      return placeholderSearch;
    }
    return useFallback
      ? getMoviePosterFallbackImageUrl(imageUrlToken, size)
      : getMoviePosterImageUrl(imageUrlToken, size);
  }, [imageUrlToken, size, useFallback]);

  return (
    <CardMedia
      component="img"
      alt="movie poster"
      sx={sx}
      src={src}
      onError={() => setUseFallback(true)}
    />
  );
};

export default PosterImage;
```

- [ ] **Step 4: Run frontend media tests**

Run:

```bash
cd frontend && yarn test imageUrls.test.ts PosterImage.test.tsx
```

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add frontend/src/shared/media/PosterImage.tsx frontend/src/shared/media/PosterImage.test.tsx
git commit -m "fix(media): fallback to JPG movie posters"
```

---

### Task 3: Backend Movie Object Paths

**Files:**
- Modify: `src/main/java/com/thecodinglab/imdbclone/media/internal/images/MovieImageConstants.java`
- Modify: `src/test/java/com/thecodinglab/imdbclone/media/MediaServiceIntegrationTest.java`

- [ ] **Step 1: Update expected movie object paths in integration tests**

In `MediaServiceIntegrationTest`, change movie image assertions from:

```java
String detailImageName = MovieImageConstants.getDetailViewImageName(imageUrlToken);
String thumbnailImageName = MovieImageConstants.getThumbNailImageName(imageUrlToken);
```

to continue using the constants, then add direct assertions where useful:

```java
assertThat(detailImageName).startsWith("movies/posters/");
assertThat(detailImageName).endsWith("_size_600x900.jpg");
assertThat(thumbnailImageName).startsWith("movies/posters/");
assertThat(thumbnailImageName).endsWith("_size_120x180.jpg");
```

- [ ] **Step 2: Run test to verify it fails**

Run:

```bash
./gradlew test --tests "com.thecodinglab.imdbclone.media.MediaServiceIntegrationTest"
```

Expected: FAIL because `MovieImageConstants.BUCKET_DIRECTORY_NAME` is still `movies/`.

- [ ] **Step 3: Move manual movie uploads under poster prefix**

Modify `src/main/java/com/thecodinglab/imdbclone/media/internal/images/MovieImageConstants.java`:

```java
public static final String FORMAT = "jpg";

public static final String BUCKET_DIRECTORY_NAME = "movies/posters/";
```

Keep `FORMAT = "jpg"` for manual uploads. The frontend WebP helper loads seeded
WebP first and falls back to JPG for manually uploaded posters.

- [ ] **Step 4: Run backend media test**

Run:

```bash
./gradlew test --tests "com.thecodinglab.imdbclone.media.MediaServiceIntegrationTest"
```

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/thecodinglab/imdbclone/media/internal/images/MovieImageConstants.java src/test/java/com/thecodinglab/imdbclone/media/MediaServiceIntegrationTest.java
git commit -m "fix(media): store manual posters under poster path"
```

---

### Task 4: Seed Context Builder

**Files:**
- Create: `infrastructure/movie-seed/runtime/prepare_seed_context.py`
- Create: `infrastructure/movie-seed/test_prepare_seed_context.py`

- [ ] **Step 1: Write failing context tests**

Create `infrastructure/movie-seed/test_prepare_seed_context.py`:

```python
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
            (source / "processed/movies/posters/poster-000_size_120x180.webp").write_text("poster")
            (source / "processed/movies/posters/poster-250_size_120x180.webp").write_text("poster")
            (source / "processed/movies/backdrops/backdrop-000_size_780x439.webp").write_text("backdrop")
            (source / "processed/movies/backdrops/backdrop-250_size_780x439.webp").write_text("backdrop")

            prepare_seed_context(source, output, profile="light", limit=250)

            with (output / "seed/movie_enriched.csv").open(encoding="utf-8", newline="") as file:
                rows = list(csv.DictReader(file))

            self.assertEqual(250, len(rows))
            self.assertTrue(
                (output / "seed/media/movies/posters/poster-000_size_120x180.webp").exists()
            )
            self.assertFalse(
                (output / "seed/media/movies/posters/poster-250_size_120x180.webp").exists()
            )
            self.assertTrue(
                (output / "seed/media/movies/backdrops/backdrop-000_size_780x439.webp").exists()
            )
            self.assertFalse(
                (output / "seed/media/movies/backdrops/backdrop-250_size_780x439.webp").exists()
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
```

- [ ] **Step 2: Run test to verify it fails**

Run:

```bash
PYTHONDONTWRITEBYTECODE=1 python3 -m unittest discover -s infrastructure/movie-seed -p "test_prepare_seed_context.py"
```

Expected: FAIL because `prepare_seed_context.py` does not exist.

- [ ] **Step 3: Implement context builder**

Create `infrastructure/movie-seed/runtime/prepare_seed_context.py`:

```python
#!/usr/bin/env python3
import argparse
import csv
import shutil
from pathlib import Path


DEFAULT_SOURCE_ROOT = Path("build/movie-seed")
DEFAULT_OUTPUT_ROOT = Path("build/movie-seed/docker-context")
MEDIA_VARIANTS = {
    "poster_image_token": ("posters", ("120x180", "300x450", "600x900")),
    "backdrop_image_token": ("backdrops", ("780x439", "1280x720")),
}


def load_rows(source_root: Path, limit: int | None) -> list[dict[str, str]]:
    with (source_root / "movie_enriched.csv").open(encoding="utf-8", newline="") as file:
        rows = list(csv.DictReader(file))
    return rows if limit is None else rows[:limit]


def write_seed_csv(rows: list[dict[str, str]], output_csv: Path) -> None:
    output_csv.parent.mkdir(parents=True, exist_ok=True)
    with output_csv.open("w", encoding="utf-8", newline="") as file:
        writer = csv.DictWriter(file, fieldnames=list(rows[0].keys()))
        writer.writeheader()
        writer.writerows(rows)


def copy_runtime_files(output_root: Path) -> None:
    runtime_root = Path(__file__).parent
    app_root = output_root / "app"
    runtime_files = (
        (runtime_root / "seed.py", app_root / "seed.py"),
        (runtime_root / "requirements.txt", app_root / "requirements.txt"),
        (runtime_root / "Dockerfile", output_root / "Dockerfile"),
    )
    for source, target in runtime_files:
        if not source.exists():
            continue
        target.parent.mkdir(parents=True, exist_ok=True)
        shutil.copy2(source, target)


def copy_matching_media(source_root: Path, output_root: Path, rows: list[dict[str, str]]) -> int:
    copied = 0
    for row in rows:
        for token_field, (kind_dir, sizes) in MEDIA_VARIANTS.items():
            token = row.get(token_field, "")
            if not token:
                continue
            for size in sizes:
                source = (
                    source_root
                    / "processed"
                    / "movies"
                    / kind_dir
                    / f"{token}_size_{size}.webp"
                )
                if not source.exists():
                    continue
                target = (
                    output_root
                    / "seed"
                    / "media"
                    / "movies"
                    / kind_dir
                    / source.name
                )
                target.parent.mkdir(parents=True, exist_ok=True)
                shutil.copy2(source, target)
                copied += 1
    return copied


def prepare_seed_context(
    source_root: Path,
    output_root: Path,
    profile: str,
    limit: int | None,
) -> None:
    if output_root.exists():
        shutil.rmtree(output_root)
    rows = load_rows(source_root, limit)
    if not rows:
        raise ValueError(f"No rows found in {source_root / 'movie_enriched.csv'}")
    write_seed_csv(rows, output_root / "seed" / "movie_enriched.csv")
    copy_runtime_files(output_root)
    copy_matching_media(source_root, output_root, rows)
    (output_root / "seed" / "SEED_PROFILE").write_text(profile, encoding="utf-8")


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Prepare a Docker seed image context.")
    parser.add_argument("--profile", choices=("light", "full"), required=True)
    parser.add_argument("--source-root", type=Path, default=DEFAULT_SOURCE_ROOT)
    parser.add_argument("--output-root", type=Path, default=DEFAULT_OUTPUT_ROOT)
    parser.add_argument("--light-limit", type=int, default=250)
    return parser.parse_args()


def main() -> None:
    args = parse_args()
    output_root = args.output_root / args.profile
    limit = args.light_limit if args.profile == "light" else None
    prepare_seed_context(args.source_root, output_root, args.profile, limit)
    print(f"Prepared {args.profile} seed context at {output_root}")


if __name__ == "__main__":
    main()
```

- [ ] **Step 4: Run context builder tests**

Run:

```bash
PYTHONDONTWRITEBYTECODE=1 python3 -m unittest discover -s infrastructure/movie-seed -p "test_prepare_seed_context.py"
```

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add infrastructure/movie-seed/runtime/prepare_seed_context.py infrastructure/movie-seed/test_prepare_seed_context.py
git commit -m "feat(seed): prepare light and full contexts"
```

---

### Task 5: Seeder Runtime DB Logic

**Files:**
- Create: `infrastructure/movie-seed/runtime/requirements.txt`
- Create: `infrastructure/movie-seed/runtime/seed.py`
- Create: `infrastructure/movie-seed/test_seed_runtime.py`

- [ ] **Step 1: Write failing runtime unit tests**

Create `infrastructure/movie-seed/test_seed_runtime.py` with tests for config defaults, key generation, and SQL constants:

```python
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
```

- [ ] **Step 2: Run test to verify it fails**

Run:

```bash
PYTHONDONTWRITEBYTECODE=1 python3 -m unittest discover -s infrastructure/movie-seed -p "test_seed_runtime.py"
```

Expected: FAIL because `seed.py` does not exist.

- [ ] **Step 3: Add runtime dependencies**

Create `infrastructure/movie-seed/runtime/requirements.txt`:

```text
boto3>=1.35,<2
psycopg[binary]>=3.2,<4
```

- [ ] **Step 4: Implement DB runtime**

Create `infrastructure/movie-seed/runtime/seed.py` with the initial DB/media shell:

```python
#!/usr/bin/env python3
import argparse
import csv
import os
from dataclasses import dataclass
from pathlib import Path
from typing import Iterable

import boto3
import psycopg


SEED_ROOT = Path(os.environ.get("SEED_ROOT", "/seed"))
CSV_PATH = SEED_ROOT / "movie_enriched.csv"
MEDIA_ROOT = SEED_ROOT / "media" / "movies"


@dataclass(frozen=True)
class Config:
    postgres_host: str
    postgres_port: str
    postgres_db: str
    postgres_user: str
    postgres_password: str
    rustfs_endpoint: str
    rustfs_access_key: str
    rustfs_secret_key: str
    rustfs_bucket: str
    seed_name: str
    seed_version: str

    @classmethod
    def from_env(cls) -> "Config":
        return cls(
            postgres_host=require_env("POSTGRES_HOST"),
            postgres_port=os.environ.get("POSTGRES_PORT", "5432"),
            postgres_db=require_env("POSTGRES_DB"),
            postgres_user=require_env("POSTGRES_USER"),
            postgres_password=require_env("POSTGRES_PASSWORD"),
            rustfs_endpoint=require_env("RUSTFS_ENDPOINT"),
            rustfs_access_key=require_env("RUSTFS_ACCESS_KEY"),
            rustfs_secret_key=require_env("RUSTFS_SECRET_KEY"),
            rustfs_bucket=os.environ.get("RUSTFS_BUCKET", "imdb-clone"),
            seed_name=os.environ.get("SEED_NAME", "local"),
            seed_version=os.environ.get("SEED_VERSION", "local"),
        )

    def postgres_dsn(self) -> str:
        return (
            f"host={self.postgres_host} port={self.postgres_port} "
            f"dbname={self.postgres_db} user={self.postgres_user} "
            f"password={self.postgres_password}"
        )


def require_env(name: str) -> str:
    value = os.environ.get(name)
    if not value:
        raise SystemExit(f"Missing required environment variable: {name}")
    return value


def normalize_db_value(value: str) -> str | None:
    return None if value == "\\N" or value == "" else value


def create_seed_run_table_sql() -> str:
    return """
    create table if not exists movie_seed_run (
        id bigint generated by default as identity primary key,
        seed_name varchar(100) not null,
        seed_version varchar(100) not null,
        command varchar(20) not null,
        status varchar(20) not null,
        row_count int not null default 0,
        media_count int not null default 0,
        error_message text,
        started_at_in_utc timestamp(6) with time zone not null default current_timestamp,
        finished_at_in_utc timestamp(6) with time zone
    )
    """


def staging_table_sql() -> str:
    return """
    create temporary table movie_seed_staging (
        id bigint,
        imdb_id varchar(20),
        movie_type varchar(50),
        primary_title varchar(1000),
        original_title varchar(1000),
        adult boolean,
        start_year int,
        end_year int,
        runtime_minutes int,
        movie_genre bigint,
        imdb_rating real,
        imdb_rating_count int,
        tmdb_id bigint,
        description text,
        poster_path text,
        backdrop_path text,
        trailer_youtube_key varchar(255),
        poster_image_token varchar(255),
        backdrop_image_token varchar(255)
    ) on commit drop
    """


def upsert_movie_sql() -> str:
    return """
    insert into movie (
        imdb_id,
        tmdb_id,
        movie_type,
        primary_title,
        original_title,
        adult,
        start_year,
        end_year,
        runtime_minutes,
        movie_genre,
        imdb_rating,
        imdb_rating_count,
        description,
        poster_image_token,
        backdrop_image_token,
        trailer_youtube_key
    )
    select
        imdb_id,
        tmdb_id,
        movie_type,
        primary_title,
        original_title,
        adult,
        start_year,
        end_year,
        runtime_minutes,
        movie_genre,
        imdb_rating,
        imdb_rating_count,
        description,
        poster_image_token,
        backdrop_image_token,
        trailer_youtube_key
    from movie_seed_staging
    on conflict (imdb_id) do update
    set tmdb_id = excluded.tmdb_id,
        movie_type = excluded.movie_type,
        primary_title = excluded.primary_title,
        original_title = excluded.original_title,
        adult = excluded.adult,
        start_year = excluded.start_year,
        end_year = excluded.end_year,
        runtime_minutes = excluded.runtime_minutes,
        movie_genre = excluded.movie_genre,
        imdb_rating = excluded.imdb_rating,
        imdb_rating_count = excluded.imdb_rating_count,
        description = excluded.description,
        poster_image_token = excluded.poster_image_token,
        backdrop_image_token = excluded.backdrop_image_token,
        trailer_youtube_key = excluded.trailer_youtube_key,
        modified_at_in_utc = current_timestamp
    """


def load_csv_rows(csv_path: Path) -> list[dict[str, str | None]]:
    with csv_path.open(encoding="utf-8", newline="") as file:
        return [
            {key: normalize_db_value(value) for key, value in row.items()}
            for row in csv.DictReader(file)
        ]


def start_seed_run(connection, config: Config, command: str) -> int:
    with connection.cursor() as cursor:
        cursor.execute(create_seed_run_table_sql())
        cursor.execute(
            """
            insert into movie_seed_run (seed_name, seed_version, command, status)
            values (%s, %s, %s, 'RUNNING')
            returning id
            """,
            (config.seed_name, config.seed_version, command),
        )
        return cursor.fetchone()[0]


def finish_seed_run(connection, run_id: int, status: str, row_count: int, media_count: int, error: str | None = None) -> None:
    with connection.cursor() as cursor:
        cursor.execute(
            """
            update movie_seed_run
            set status = %s,
                row_count = %s,
                media_count = %s,
                error_message = %s,
                finished_at_in_utc = current_timestamp
            where id = %s
            """,
            (status, row_count, media_count, error, run_id),
        )


def seed_db(config: Config) -> int:
    rows = load_csv_rows(CSV_PATH)
    with psycopg.connect(config.postgres_dsn()) as connection:
        run_id = start_seed_run(connection, config, "db")
        connection.commit()
        try:
            with connection.transaction():
                with connection.cursor() as cursor:
                    cursor.execute(staging_table_sql())
                    with cursor.copy(
                        "copy movie_seed_staging from stdin with (format csv, header true, null '\\N')"
                    ) as copy:
                        with CSV_PATH.open(encoding="utf-8", newline="") as file:
                            for line in file:
                                copy.write(line)
                    cursor.execute(upsert_movie_sql())
            finish_seed_run(connection, run_id, "SUCCEEDED", len(rows), 0)
            connection.commit()
            return len(rows)
        except Exception as exc:
            connection.rollback()
            finish_seed_run(connection, run_id, "FAILED", 0, 0, str(exc))
            connection.commit()
            raise


def build_media_key(kind_dir: str, filename: str) -> str:
    return f"movies/{kind_dir}/{filename}"


def iter_media_files() -> Iterable[tuple[str, Path]]:
    for kind_dir in ("posters", "backdrops"):
        directory = MEDIA_ROOT / kind_dir
        if not directory.exists():
            continue
        for path in sorted(directory.glob("*.webp")):
            yield build_media_key(kind_dir, path.name), path


def seed_media(config: Config) -> int:
    client = boto3.client(
        "s3",
        endpoint_url=config.rustfs_endpoint,
        aws_access_key_id=config.rustfs_access_key,
        aws_secret_access_key=config.rustfs_secret_key,
    )
    with psycopg.connect(config.postgres_dsn()) as connection:
        run_id = start_seed_run(connection, config, "media")
        connection.commit()
        uploaded = 0
        try:
            ensure_bucket(client, config.rustfs_bucket)
            for key, path in iter_media_files():
                client.upload_file(
                    str(path),
                    config.rustfs_bucket,
                    key,
                    ExtraArgs={"ContentType": "image/webp"},
                )
                uploaded += 1
            finish_seed_run(connection, run_id, "SUCCEEDED", 0, uploaded)
            connection.commit()
            return uploaded
        except Exception as exc:
            connection.rollback()
            finish_seed_run(connection, run_id, "FAILED", 0, uploaded, str(exc))
            connection.commit()
            raise


def ensure_bucket(client, bucket: str) -> None:
    existing = [item["Name"] for item in client.list_buckets().get("Buckets", [])]
    if bucket not in existing:
        client.create_bucket(Bucket=bucket)
    client.put_bucket_policy(
        Bucket=bucket,
        Policy=f"""
        {{
          "Version": "2012-10-17",
          "Statement": [
            {{
              "Effect": "Allow",
              "Principal": "*",
              "Action": ["s3:GetObject"],
              "Resource": ["arn:aws:s3:::{bucket}/movies/*"]
            }}
          ]
        }}
        """,
    )


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Seed imdb-clone data and media.")
    parser.add_argument("command", choices=("all", "db", "media"))
    return parser.parse_args()


def main() -> None:
    args = parse_args()
    config = Config.from_env()
    if args.command in ("all", "db"):
        rows = seed_db(config)
        print(f"Seeded {rows} movie rows")
    if args.command in ("all", "media"):
        objects = seed_media(config)
        print(f"Seeded {objects} media objects")


if __name__ == "__main__":
    main()
```

- [ ] **Step 5: Run unit tests**

Run:

```bash
PYTHONDONTWRITEBYTECODE=1 python3 -m unittest discover -s infrastructure/movie-seed -p "test_seed_runtime.py"
```

Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add infrastructure/movie-seed/runtime/requirements.txt infrastructure/movie-seed/runtime/seed.py infrastructure/movie-seed/test_seed_runtime.py
git commit -m "feat(seed): add database and media seeder"
```

---

### Task 6: Docker Seed Image Build

**Files:**
- Create: `infrastructure/movie-seed/runtime/Dockerfile`
- Modify: `Makefile`
- Modify: `.gitignore`

- [ ] **Step 1: Add Dockerfile**

Create `infrastructure/movie-seed/runtime/Dockerfile`:

```Dockerfile
FROM python:3.14-slim

WORKDIR /app

COPY app/requirements.txt /app/requirements.txt
RUN pip install --no-cache-dir -r /app/requirements.txt

COPY app/seed.py /app/seed.py
COPY seed /seed

ENTRYPOINT ["python", "/app/seed.py"]
CMD ["all"]
```

- [ ] **Step 2: Ignore prepared contexts**

Add to `.gitignore`:

```gitignore
/build/movie-seed/docker-context/
```

- [ ] **Step 3: Add Makefile targets**

Add near existing seed variables in `Makefile`:

```make
SEED_IMAGE = niklastiede/imdb-clone-seed
SEED_VERSION ?= local
SEED_LIGHT_TAG = $(SEED_IMAGE):light-$(SEED_VERSION)
SEED_FULL_TAG = $(SEED_IMAGE):full-$(SEED_VERSION)
SEED_CONTEXT_ROOT = build/movie-seed/docker-context

.PHONY: prepare-seed-light prepare-seed-full build-seed-light build-seed-full push-seed-light push-seed-full seed-light seed-full

prepare-seed-light: ## prepare lightweight seed Docker context
	python3 infrastructure/movie-seed/runtime/prepare_seed_context.py --profile light

prepare-seed-full: ## prepare full seed Docker context
	python3 infrastructure/movie-seed/runtime/prepare_seed_context.py --profile full

build-seed-light: prepare-seed-light ## build lightweight seed image
	docker build -t $(SEED_LIGHT_TAG) $(SEED_CONTEXT_ROOT)/light

build-seed-full: prepare-seed-full ## build full seed image
	docker build -t $(SEED_FULL_TAG) $(SEED_CONTEXT_ROOT)/full

push-seed-light: ## push lightweight seed image
	docker push $(SEED_LIGHT_TAG)

push-seed-full: ## push full seed image
	docker push $(SEED_FULL_TAG)

seed-light: ## run lightweight seed against local Docker Compose services
	docker run --rm --network imdb-clone-network \
		-e POSTGRES_HOST=imdb-clone-postgresql \
		-e POSTGRES_DB=movie_db \
		-e POSTGRES_USER=myroot \
		-e POSTGRES_PASSWORD=secret \
		-e RUSTFS_ENDPOINT=http://imdb-clone-rustfs:9000 \
		-e RUSTFS_ACCESS_KEY=ROOTNAME \
		-e RUSTFS_SECRET_KEY=CHANGEME123 \
		-e RUSTFS_BUCKET=imdb-clone \
		-e SEED_NAME=light \
		-e SEED_VERSION=$(SEED_VERSION) \
		$(SEED_LIGHT_TAG) all

seed-full: ## run full seed against local Docker Compose services
	docker run --rm --network imdb-clone-network \
		-e POSTGRES_HOST=imdb-clone-postgresql \
		-e POSTGRES_DB=movie_db \
		-e POSTGRES_USER=myroot \
		-e POSTGRES_PASSWORD=secret \
		-e RUSTFS_ENDPOINT=http://imdb-clone-rustfs:9000 \
		-e RUSTFS_ACCESS_KEY=ROOTNAME \
		-e RUSTFS_SECRET_KEY=CHANGEME123 \
		-e RUSTFS_BUCKET=imdb-clone \
		-e SEED_NAME=full \
		-e SEED_VERSION=$(SEED_VERSION) \
		$(SEED_FULL_TAG) all
```

- [ ] **Step 4: Build light image**

Run:

```bash
make build-seed-light SEED_VERSION=dev
```

Expected: Docker image `niklastiede/imdb-clone-seed:light-dev` exists.

- [ ] **Step 5: Commit**

```bash
git add infrastructure/movie-seed/runtime/Dockerfile Makefile .gitignore
git commit -m "build(seed): add versioned seed image targets"
```

---

### Task 7: Compose Seed Services

**Files:**
- Modify: `compose.yaml`
- Modify: `infrastructure/deployment/development/docker-compose.yaml`
- Modify: `infrastructure/deployment/production/docker-compose.stateful-apps.yaml`

- [ ] **Step 1: Add local optional seed services**

In `compose.yaml`, add services after RustFS:

```yaml
  imdb-clone-seed-light:
    image: ${IMDB_CLONE_SEED_LIGHT_IMAGE:-niklastiede/imdb-clone-seed:light-local}
    profiles:
      - seed-light
    depends_on:
      - imdb-clone-postgresql
      - imdb-clone-rustfs
    environment:
      - POSTGRES_HOST=imdb-clone-postgresql
      - POSTGRES_DB=movie_db
      - POSTGRES_USER=myroot
      - POSTGRES_PASSWORD=secret
      - RUSTFS_ENDPOINT=http://imdb-clone-rustfs:9000
      - RUSTFS_ACCESS_KEY=ROOTNAME
      - RUSTFS_SECRET_KEY=CHANGEME123
      - RUSTFS_BUCKET=imdb-clone
      - SEED_NAME=light
      - SEED_VERSION=${SEED_VERSION:-local}
    networks:
      - imdb-clone-network

  imdb-clone-seed-full:
    image: ${IMDB_CLONE_SEED_FULL_IMAGE:-niklastiede/imdb-clone-seed:full-local}
    profiles:
      - seed-full
    depends_on:
      - imdb-clone-postgresql
      - imdb-clone-rustfs
    environment:
      - POSTGRES_HOST=imdb-clone-postgresql
      - POSTGRES_DB=movie_db
      - POSTGRES_USER=myroot
      - POSTGRES_PASSWORD=secret
      - RUSTFS_ENDPOINT=http://imdb-clone-rustfs:9000
      - RUSTFS_ACCESS_KEY=ROOTNAME
      - RUSTFS_SECRET_KEY=CHANGEME123
      - RUSTFS_BUCKET=imdb-clone
      - SEED_NAME=full
      - SEED_VERSION=${SEED_VERSION:-local}
    networks:
      - imdb-clone-network
```

- [ ] **Step 2: Replace development object loader**

In `infrastructure/deployment/development/docker-compose.yaml`, delete
`load-object-storage-data` and add equivalent `imdb-clone-seed-light` and
`imdb-clone-seed-full` services using the same env values as `compose.yaml`.

- [ ] **Step 3: Replace production object loader**

In `infrastructure/deployment/production/docker-compose.stateful-apps.yaml`,
delete `load-object-storage-data` and add:

```yaml
  imdb-clone-seed-full:
    image: ${IMDB_CLONE_SEED_FULL_IMAGE}
    profiles:
      - seed-full
    depends_on:
      - imdb-clone-postgresql
      - imdb-clone-rustfs
    environment:
      - POSTGRES_HOST=imdb-clone-postgresql
      - POSTGRES_DB=movie_db
      - POSTGRES_USER=${POSTGRESQL_USERNAME_ENV_VAR}
      - POSTGRES_PASSWORD=${POSTGRESQL_PASSWORD_ENV_VAR}
      - RUSTFS_ENDPOINT=http://imdb-clone-rustfs:9000
      - RUSTFS_ACCESS_KEY=${OBJECT_STORAGE_ACCESS_KEY_ENV_VAR}
      - RUSTFS_SECRET_KEY=${OBJECT_STORAGE_SECRET_KEY_ENV_VAR}
      - RUSTFS_BUCKET=imdb-clone
      - SEED_NAME=full
      - SEED_VERSION=${SEED_VERSION_ENV_VAR}
    networks:
      - imdb-clone-network
```

- [ ] **Step 4: Validate compose syntax**

Run:

```bash
docker compose config >/tmp/imdb-clone-compose-config.yaml
docker compose -f infrastructure/deployment/development/docker-compose.yaml config >/tmp/imdb-clone-dev-compose-config.yaml
```

Expected: both commands exit 0.

- [ ] **Step 5: Commit**

```bash
git add compose.yaml infrastructure/deployment/development/docker-compose.yaml infrastructure/deployment/production/docker-compose.stateful-apps.yaml
git commit -m "chore(seed): wire seed images into compose"
```

---

### Task 8: k3s Seed Job Template

**Files:**
- Create: `infrastructure/kubernetes/seed-job.example.yaml`
- Modify: `infrastructure/kubernetes/README.md`

- [ ] **Step 1: Add Job example**

Create `infrastructure/kubernetes/seed-job.example.yaml`:

```yaml
apiVersion: batch/v1
kind: Job
metadata:
  name: imdb-clone-full-seed
  namespace: imdb-clone
spec:
  backoffLimit: 1
  template:
    spec:
      restartPolicy: Never
      containers:
        - name: seed
          image: niklastiede/imdb-clone-seed:full-REPLACE_WITH_VERSION
          args: ["all"]
          env:
            - name: POSTGRES_HOST
              value: imdb-clone-postgresql.databases.svc.cluster.local
            - name: POSTGRES_PORT
              value: "5432"
            - name: POSTGRES_DB
              value: movie_db
            - name: POSTGRES_USER
              valueFrom:
                secretKeyRef:
                  name: imdb-clone-postgresql
                  key: username
            - name: POSTGRES_PASSWORD
              valueFrom:
                secretKeyRef:
                  name: imdb-clone-postgresql
                  key: password
            - name: RUSTFS_ENDPOINT
              value: http://imdb-clone-rustfs.databases.svc.cluster.local:9000
            - name: RUSTFS_ACCESS_KEY
              valueFrom:
                secretKeyRef:
                  name: imdb-clone-rustfs
                  key: access-key
            - name: RUSTFS_SECRET_KEY
              valueFrom:
                secretKeyRef:
                  name: imdb-clone-rustfs
                  key: secret-key
            - name: RUSTFS_BUCKET
              value: imdb-clone
            - name: SEED_NAME
              value: full
            - name: SEED_VERSION
              value: REPLACE_WITH_VERSION
```

- [ ] **Step 2: Document k3s usage**

Append to `infrastructure/kubernetes/README.md`:

```markdown
## Movie Seed Job

`seed-job.example.yaml` documents the Kubernetes Job shape for the versioned full
seed image. Copy it into the GitOps app tree once PostgreSQL and RustFS services
exist in k3s, replace the image tag and secret names, then let Argo CD apply it.

The seed job is idempotent. Rerunning it upserts movie rows and uploads media
objects without deleting existing catalog data.
```

- [ ] **Step 3: Commit**

```bash
git add infrastructure/kubernetes/seed-job.example.yaml infrastructure/kubernetes/README.md
git commit -m "docs(seed): add k3s seed job template"
```

---

### Task 9: Documentation And Smoke Flow

**Files:**
- Modify: `infrastructure/movie-seed/README.md`
- Modify: `README.md`

- [ ] **Step 1: Document seed image workflow**

Add to `infrastructure/movie-seed/README.md`:

```markdown
## Build Versioned Seed Images

Build the lightweight local seed image:

```bash
make build-seed-light SEED_VERSION=2026-05-17
```

Build the full seed image:

```bash
make build-seed-full SEED_VERSION=2026-05-17
```

Push to Docker Hub:

```bash
make push-seed-light SEED_VERSION=2026-05-17
make push-seed-full SEED_VERSION=2026-05-17
```

Run the lightweight seed against local Compose services:

```bash
make docker-compose-dev-up
make seed-light SEED_VERSION=2026-05-17
```

The light image contains 250 movies and matching WebP media. The full image
contains every row in `build/movie-seed/movie_enriched.csv` and all matching
WebP media.
```

- [ ] **Step 2: Add root README local seed note**

Add a concise local setup note to `README.md`:

```markdown
### Seed Local Movie Data

After starting PostgreSQL, RustFS, and Elasticsearch with Docker Compose, load
the lightweight movie catalog:

```bash
make docker-compose-dev-up
make seed-light SEED_VERSION=2026-05-17
```

The seed is idempotent, so rerunning it updates movies and media without wiping
user data.
```

- [ ] **Step 3: Run docs scan**

Run:

```bash
rg -n "compressed_movie_images|load-object-storage-data|Dropbox|jpg" infrastructure/movie-seed README.md infrastructure/deployment
```

Expected: no stale Dropbox object-loader references in deployment docs. JPG hits are allowed only for profile photos or manual upload fallback explanations.

- [ ] **Step 4: Commit**

```bash
git add infrastructure/movie-seed/README.md README.md
git commit -m "docs(seed): document seed image workflow"
```

---

### Task 10: End-to-End Verification

**Files:**
- No planned source edits.

- [ ] **Step 1: Run Python unit tests**

Run:

```bash
PYTHONDONTWRITEBYTECODE=1 python3 -m unittest discover -s infrastructure/movie-seed
```

Expected: PASS.

- [ ] **Step 2: Run frontend tests and build**

Run:

```bash
cd frontend && yarn test imageUrls.test.ts PosterImage.test.tsx
cd frontend && yarn build
```

Expected: PASS and build exit 0.

- [ ] **Step 3: Run backend media tests**

Run:

```bash
./gradlew test --tests "com.thecodinglab.imdbclone.media.MediaServiceIntegrationTest"
```

Expected: PASS.

- [ ] **Step 4: Build and smoke-test light seed image**

Run:

```bash
make docker-compose-dev-up
make build-seed-light SEED_VERSION=smoke
make seed-light SEED_VERSION=smoke
docker exec imdb-clone-postgresql psql -U myroot -d movie_db -c "select count(*) from movie;"
docker run --rm --network imdb-clone-network minio/mc \
  alias set rustfs http://imdb-clone-rustfs:9000 ROOTNAME CHANGEME123
```

Expected:

- movie count is at least `250`.
- `make seed-light SEED_VERSION=smoke` can be rerun without duplicate movies.
- RustFS contains objects under `imdb-clone/movies/posters/` and
  `imdb-clone/movies/backdrops/`.

- [ ] **Step 5: Final status check**

Run:

```bash
git status --short
```

Expected: clean working tree after task commits.

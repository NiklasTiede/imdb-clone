# Versioned Seed Images Design

## Goal

Create versioned Docker seed images that can initialize PostgreSQL movie data and
RustFS movie media for local development and the future k3s homeserver
deployment.

The seed system must support two artifact sizes:

- `light`: 250 enriched movies and matching poster/backdrop assets for local
  development.
- `full`: all enriched movies and matching poster/backdrop assets for production
  initial loads and full demos.

Both variants use the same seeder implementation. Only the packaged data differs.

## Context

The application has moved from MySQL to PostgreSQL and from MinIO to RustFS.
`build/movie-seed/movie_enriched.csv` contains 10,000 enriched movies with the
columns needed by the `movie` table, including IMDb/TMDB identifiers,
description, trailer key, and poster/backdrop image tokens.

Generated movie media currently lives under:

```text
build/movie-seed/processed/movies/posters/
build/movie-seed/processed/movies/backdrops/
```

Those assets are WebP files. The application still has older JPG movie image
assumptions in frontend URL builders and backend movie image constants, so the
seed work must include the WebP app adjustment for movie media. Profile photos
can stay JPG.

## Seed Image Shape

One Dockerfile and one seeder CLI will produce two independently pushed image
tags:

```text
niklastiede/imdb-clone-seed:light-<version>
niklastiede/imdb-clone-seed:full-<version>
```

The light image contains only the first 250 rows from `movie_enriched.csv` and
only the matching poster/backdrop WebP files. Developers pulling the light image
do not download the full multi-GB seed image.

The full image contains all rows and all matching poster/backdrop WebP files.

Each image contains:

```text
/seed/movie_enriched.csv
/seed/media/movies/posters/*.webp
/seed/media/movies/backdrops/*.webp
/app/seed.py
/app/requirements.txt
```

The image entrypoint supports:

```bash
python /app/seed.py all
python /app/seed.py db
python /app/seed.py media
```

`all` runs DB seeding first, then media seeding. `db` and `media` are useful for
retrying one side of a failed seed run.

## Runtime Configuration

The container is configured only through environment variables so Docker
Compose and Kubernetes Jobs can use the same contract:

```text
POSTGRES_HOST
POSTGRES_PORT
POSTGRES_DB
POSTGRES_USER
POSTGRES_PASSWORD
RUSTFS_ENDPOINT
RUSTFS_ACCESS_KEY
RUSTFS_SECRET_KEY
RUSTFS_BUCKET
SEED_NAME
SEED_VERSION
```

`POSTGRES_PORT` defaults to `5432`. `RUSTFS_BUCKET` defaults to `imdb-clone`.
`SEED_NAME` is `light` or `full`. `SEED_VERSION` is the image/data version.

## Database Seeding

Database seeding is idempotent and non-destructive by default.

The seeder creates a helper table if it is missing:

```text
movie_seed_run
```

The table records seed name, seed version, command mode, row count, media count,
timestamps, status, and error message. It is operational metadata only and does
not replace Flyway migrations.

The DB seed flow is:

1. Start a `movie_seed_run` row with status `RUNNING`.
2. Create a temporary or unlogged staging table for the current connection.
3. Load `/seed/movie_enriched.csv` into staging with PostgreSQL `COPY`.
4. Upsert into `movie` using `imdb_id` as the stable key.
5. Preserve internal generated `movie.id` for existing rows.
6. Preserve app-owned user rating fields: `rating`, `rating_count`, and
   `rating_sum`.
7. Mark the seed run `SUCCEEDED` with row counts, or `FAILED` with the error
   message.

Running light after full does not delete movies. Running full after light expands
and updates the catalog.

## Media Seeding

Movie media is stored as WebP under separate poster and backdrop prefixes:

```text
imdb-clone/movies/posters/{poster_token}_size_120x180.webp
imdb-clone/movies/posters/{poster_token}_size_300x450.webp
imdb-clone/movies/posters/{poster_token}_size_600x900.webp

imdb-clone/movies/backdrops/{backdrop_token}_size_780x439.webp
imdb-clone/movies/backdrops/{backdrop_token}_size_1280x720.webp
```

The media seed flow is:

1. Connect to RustFS through the S3-compatible API.
2. Create the bucket if it does not exist.
3. Ensure public read access for movie media.
4. Upload poster and backdrop WebP objects from `/seed/media`.
5. Use overwrite-safe/idempotent uploads so reruns are safe.
6. Record object counts in `movie_seed_run`.

The seed container does not delete objects by default.

## App WebP Adjustment

Movie images become WebP end-to-end:

- Frontend poster URLs use `/movies/posters/...webp`.
- Frontend backdrop URLs use `/movies/backdrops/...webp`.
- Backend movie image constants use WebP for movie objects.
- Existing manual movie poster upload must either generate WebP objects or be
  updated in the same implementation so uploaded movie posters still render.
- Profile photo storage remains JPG for now.

The frontend can keep using `imageUrlToken` as the compatibility field for
posters, while also exposing a dedicated backdrop URL helper based on
`backdropImageToken`.

## Elasticsearch

The seeder does not write Elasticsearch directly in the first version.

The backend already creates the `movies` index and indexes popular movies from
PostgreSQL on startup when Elasticsearch has too few documents. That remains the
initial search-projection path. A future `seed search` mode can be added if
explicit indexing becomes necessary.

## Local Developer Workflow

The default local path uses the light seed image:

```bash
make docker-compose-dev-up
make seed-light
./gradlew bootRun
cd frontend && yarn start
```

`make seed-light` runs a one-shot container on `imdb-clone-network` using
`niklastiede/imdb-clone-seed:light-<version>`.

The Makefile will expose:

```text
build-seed-light
build-seed-full
push-seed-light
push-seed-full
seed-light
seed-full
```

Docker Compose can also expose optional profiles for seed services so developers
can choose Make targets or Compose profile commands.

## Production And k3s Path

Production can use the full seed image as a one-shot Docker Compose service
until the k3s manifests are ready.

For k3s, the same image and environment contract will be represented as a
Kubernetes `Job` under the GitOps tree. The Job will use the full seed image by
default and can be rerun safely because the seed is idempotent.

Secrets for PostgreSQL and RustFS credentials will come from the existing SOPS
secret flow when k3s app manifests are introduced.

## Testing

Tests should cover:

- CSV slicing for light seed artifacts.
- Object-key generation for posters and backdrops.
- DB upsert behavior against PostgreSQL Testcontainers.
- Idempotent rerun behavior: the second run updates rows without duplicates.
- Frontend movie image URL helpers for WebP poster/backdrop paths.
- Backend movie image constants for WebP movie object names.

A container smoke test should run the light seed against local PostgreSQL and
RustFS before the first Docker Hub push.

## Non-Goals

- Do not make the Spring Boot backend own the heavy seed import.
- Do not write Elasticsearch directly in the first seed version.
- Do not make light seeding destructive.
- Do not move profile photos to WebP in this work.
- Do not require developers to download the full seed image for local light
  setup.

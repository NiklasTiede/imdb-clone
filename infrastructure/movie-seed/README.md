# Movie Seed Pipeline

Build IMDb movie seed candidates from the official IMDb non-commercial dataset
files:

- `title.basics.tsv.gz`
- `title.ratings.tsv.gz`

The first step is intentionally local and deterministic. It does not call TMDB
or download images. It creates the popularity-ranked movie candidate CSV that
later TMDB enrichment and RustFS poster generation can consume.

## Build Candidates

```bash
python3 infrastructure/movie-seed/build_movie_seed.py \
  --basics infrastructure/movie-seed/data-processing/imdb-dataset/title.basics.tsv.gz \
  --ratings infrastructure/movie-seed/data-processing/imdb-dataset/title.ratings.tsv.gz \
  --limit 10000 \
  --output build/movie-seed/movie_candidates.csv
```

The output contains both the legacy numeric seed `id` and the source IMDb ID,
for example `2872718` and `tt2872718`.

Rows are filtered to non-adult IMDb `movie` titles with ratings and valid
runtime, then sorted by `numVotes` descending.

## Enrich With TMDB

Create `movie_enriched.csv` with TMDB metadata, poster/backdrop paths, a best
YouTube trailer key, and deterministic image tokens:

```bash
TMDB_API_KEY=your-api-key \
python3 infrastructure/movie-seed/enrich_movie_seed.py \
  --input build/movie-seed/movie_candidates.csv \
  --output build/movie-seed/movie_enriched.csv \
  --cache-dir build/movie-seed/tmdb-cache \
  --report build/movie-seed/movie_enrichment_report.json \
  --sleep-seconds 0.25 \
  --log-every 100
```

The enrichment step caches one TMDB response per IMDb ID under the cache
directory. Reruns reuse cached responses and only call TMDB for missing cache
files.

Rows without a TMDB movie result or without a poster are skipped. Backdrops and
trailers are optional.

Progress is printed every `--log-every` rows:

```text
[100/10000] enriched=94 skipped_no_match=2 skipped_no_poster=4 cache_hits=80 api_fetches=20 current=tt0111161 The Shawshank Redemption
```

At the end, the same counters are written to
`build/movie-seed/movie_enrichment_report.json`.

## Download Image Originals

Download TMDB originals referenced by `movie_enriched.csv` and keep them
locally:

```bash
infrastructure/object-storage/dev-seed/.venv/bin/python \
  infrastructure/movie-seed/download_movie_images.py \
  download-originals \
  --input build/movie-seed/movie_enriched.csv \
  --output-root build/movie-seed \
  --originals-root "/Volumes/TOSHIBA EXT/movie-images" \
  --report build/movie-seed/movie_originals_report.json \
  --sleep-seconds 0.05 \
  --log-every 100
```

Original files are kept under:

```text
/Volumes/TOSHIBA EXT/movie-images/posters/
/Volumes/TOSHIBA EXT/movie-images/backdrops/
```

## Convert WebP Variants

Generate app-ready WebP variants from the local originals:

```bash
infrastructure/object-storage/dev-seed/.venv/bin/python \
  infrastructure/movie-seed/download_movie_images.py \
  convert-variants \
  --input build/movie-seed/movie_enriched.csv \
  --output-root build/movie-seed \
  --originals-root "/Volumes/TOSHIBA EXT/movie-images" \
  --report build/movie-seed/movie_variants_report.json \
  --log-every 100
```

Processed app variants are written under:

```text
build/movie-seed/processed/movies/posters/
build/movie-seed/processed/movies/backdrops/
```

Poster variants:

```text
120x180
300x450
600x900
```

Backdrop variants:

```text
780x439
1280x720
```

The download command skips originals that already exist. The conversion command
skips assets whose WebP variants already exist. Use `--overwrite` to regenerate
files.

## Test

```bash
PYTHONDONTWRITEBYTECODE=1 infrastructure/object-storage/dev-seed/.venv/bin/python \
  -m unittest discover -s infrastructure/movie-seed
```

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
make seed-local-users
make seed-light SEED_VERSION=2026-05-17
```

The light image contains 250 movies and matching WebP media. The full image
contains every row in `build/movie-seed/movie_enriched.csv` and all matching
WebP media.

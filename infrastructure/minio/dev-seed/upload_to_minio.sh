#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
MOVIES_DIR="${1:-$SCRIPT_DIR/movies}"

MINIO_ALIAS="${MINIO_ALIAS:-imdb-clone-local}"
MINIO_ENDPOINT="${MINIO_ENDPOINT:-http://localhost:9000}"
MINIO_ACCESS_KEY="${MINIO_ACCESS_KEY:-ROOTNAME}"
MINIO_SECRET_KEY="${MINIO_SECRET_KEY:-CHANGEME123}"
MINIO_BUCKET="${MINIO_BUCKET:-imdb-clone}"
DOCKER_NETWORK="${DOCKER_NETWORK:-imdb-clone-network}"
DOCKER_MINIO_ENDPOINT="${DOCKER_MINIO_ENDPOINT:-http://imdb-clone-minio:9000}"

if [[ ! -d "$MOVIES_DIR" ]]; then
  echo "Movie image directory does not exist: $MOVIES_DIR"
  echo "Generate it first with: python3 infrastructure/minio/dev-seed/generate_movie_images.py"
  exit 1
fi

if command -v mc >/dev/null 2>&1; then
  mc alias set "$MINIO_ALIAS" "$MINIO_ENDPOINT" "$MINIO_ACCESS_KEY" "$MINIO_SECRET_KEY"
  mc mb --ignore-existing "$MINIO_ALIAS/$MINIO_BUCKET"
  mc anonymous set download "$MINIO_ALIAS/$MINIO_BUCKET/movies"
  mc cp --recursive "$MOVIES_DIR/" "$MINIO_ALIAS/$MINIO_BUCKET/movies/"
elif command -v docker >/dev/null 2>&1; then
  docker run --rm \
    --network "$DOCKER_NETWORK" \
    --entrypoint /bin/sh \
    -v "$MOVIES_DIR:/seed-movies:ro" \
    -e MINIO_ACCESS_KEY="$MINIO_ACCESS_KEY" \
    -e MINIO_SECRET_KEY="$MINIO_SECRET_KEY" \
    -e MINIO_BUCKET="$MINIO_BUCKET" \
    -e DOCKER_MINIO_ENDPOINT="$DOCKER_MINIO_ENDPOINT" \
    minio/mc \
    -c 'mc alias set minio "$DOCKER_MINIO_ENDPOINT" "$MINIO_ACCESS_KEY" "$MINIO_SECRET_KEY" &&
        mc mb --ignore-existing "minio/$MINIO_BUCKET" &&
        mc anonymous set download "minio/$MINIO_BUCKET/movies" &&
        mc cp --recursive /seed-movies/ "minio/$MINIO_BUCKET/movies/"'
else
  echo "Neither mc nor docker is available. Install the MinIO client or run Docker."
  exit 1
fi

#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
MOVIES_DIR="${1:-$SCRIPT_DIR/movies}"

OBJECT_STORAGE_ALIAS="${OBJECT_STORAGE_ALIAS:-imdb-clone-local}"
OBJECT_STORAGE_ENDPOINT="${OBJECT_STORAGE_ENDPOINT:-http://localhost:9000}"
OBJECT_STORAGE_ACCESS_KEY="${OBJECT_STORAGE_ACCESS_KEY:-ROOTNAME}"
OBJECT_STORAGE_SECRET_KEY="${OBJECT_STORAGE_SECRET_KEY:-CHANGEME123}"
OBJECT_STORAGE_BUCKET="${OBJECT_STORAGE_BUCKET:-imdb-clone}"
DOCKER_NETWORK="${DOCKER_NETWORK:-imdb-clone-network}"
DOCKER_OBJECT_STORAGE_ENDPOINT="${DOCKER_OBJECT_STORAGE_ENDPOINT:-http://imdb-clone-rustfs:9000}"

if [[ ! -d "$MOVIES_DIR" ]]; then
  echo "Movie image directory does not exist: $MOVIES_DIR"
  echo "Generate it first with: python3 infrastructure/minio/dev-seed/generate_movie_images.py"
  exit 1
fi

if command -v mc >/dev/null 2>&1; then
  mc alias set "$OBJECT_STORAGE_ALIAS" "$OBJECT_STORAGE_ENDPOINT" "$OBJECT_STORAGE_ACCESS_KEY" "$OBJECT_STORAGE_SECRET_KEY"
  mc mb --ignore-existing "$OBJECT_STORAGE_ALIAS/$OBJECT_STORAGE_BUCKET"
  mc anonymous set download "$OBJECT_STORAGE_ALIAS/$OBJECT_STORAGE_BUCKET/movies"
  mc cp --recursive "$MOVIES_DIR/" "$OBJECT_STORAGE_ALIAS/$OBJECT_STORAGE_BUCKET/movies/"
elif command -v docker >/dev/null 2>&1; then
  docker run --rm \
    --network "$DOCKER_NETWORK" \
    --entrypoint /bin/sh \
    -v "$MOVIES_DIR:/seed-movies:ro" \
    -e OBJECT_STORAGE_ACCESS_KEY="$OBJECT_STORAGE_ACCESS_KEY" \
    -e OBJECT_STORAGE_SECRET_KEY="$OBJECT_STORAGE_SECRET_KEY" \
    -e OBJECT_STORAGE_BUCKET="$OBJECT_STORAGE_BUCKET" \
    -e DOCKER_OBJECT_STORAGE_ENDPOINT="$DOCKER_OBJECT_STORAGE_ENDPOINT" \
    minio/mc \
    -c 'mc alias set rustfs "$DOCKER_OBJECT_STORAGE_ENDPOINT" "$OBJECT_STORAGE_ACCESS_KEY" "$OBJECT_STORAGE_SECRET_KEY" &&
        mc mb --ignore-existing "rustfs/$OBJECT_STORAGE_BUCKET" &&
        mc anonymous set download "rustfs/$OBJECT_STORAGE_BUCKET/movies" &&
        mc cp --recursive /seed-movies/ "rustfs/$OBJECT_STORAGE_BUCKET/movies/"'
else
  echo "Neither mc nor docker is available. Install the MinIO client or run Docker."
  exit 1
fi

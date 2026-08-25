#!/usr/bin/env bash

set -euo pipefail

readonly IMAGE="${BACKEND_IMAGE:-imdb-clone-backend:local}"
readonly PLATFORM="${BACKEND_DOCKER_PLATFORM:-linux/amd64}"
readonly PORT="${BACKEND_SMOKE_PORT:-18081}"
readonly POSTGRES_IMAGE="${BACKEND_SMOKE_POSTGRES_IMAGE:-postgres:18}"
readonly SUFFIX="$$"
readonly NETWORK="imdb-clone-backend-smoke-${SUFFIX}"
readonly POSTGRES_CONTAINER="imdb-clone-backend-smoke-postgres-${SUFFIX}"
readonly BACKEND_CONTAINER="imdb-clone-backend-smoke-${SUFFIX}"

cleanup() {
  docker rm --force --volumes "${BACKEND_CONTAINER}" >/dev/null 2>&1 || true
  docker rm --force --volumes "${POSTGRES_CONTAINER}" >/dev/null 2>&1 || true
  docker network rm "${NETWORK}" >/dev/null 2>&1 || true
}
trap cleanup EXIT

docker network create "${NETWORK}" >/dev/null
docker run \
  --detach \
  --name "${POSTGRES_CONTAINER}" \
  --network "${NETWORK}" \
  --platform "${PLATFORM}" \
  --env POSTGRES_DB=movie_db \
  --env POSTGRES_USER=movie_user \
  --env POSTGRES_PASSWORD=movie_password \
  "${POSTGRES_IMAGE}" >/dev/null

postgres_ready=false
for _ in {1..60}; do
  if [[ "$(docker inspect --format '{{.State.Running}}' "${POSTGRES_CONTAINER}")" != "true" ]]; then
    docker logs "${POSTGRES_CONTAINER}"
    exit 1
  fi
  if docker exec "${POSTGRES_CONTAINER}" \
    pg_isready --quiet --username movie_user --dbname movie_db; then
    postgres_ready=true
    break
  fi
  sleep 1
done
if [[ "${postgres_ready}" != "true" ]]; then
  docker logs "${POSTGRES_CONTAINER}"
  exit 1
fi

docker run \
  --detach \
  --name "${BACKEND_CONTAINER}" \
  --network "${NETWORK}" \
  --platform "${PLATFORM}" \
  --read-only \
  --cap-drop ALL \
  --security-opt no-new-privileges \
  --tmpfs /tmp:rw,exec,nosuid,size=64m \
  --publish "127.0.0.1:${PORT}:8081" \
  --env SPRING_PROFILES_ACTIVE=dev \
  --env SPRING_DATASOURCE_URL="jdbc:postgresql://${POSTGRES_CONTAINER}:5432/movie_db" \
  --env SPRING_DATASOURCE_USERNAME=movie_user \
  --env SPRING_DATASOURCE_PASSWORD=movie_password \
  --env OPENSEARCH_URIS=http://127.0.0.1:1 \
  --env IMDB_CLONE_MEDIA_STORAGE_URI=http://127.0.0.1:1 \
  --env IMDB_CLONE_CATALOG_SEARCH_EMBEDDING_BASE_URL=http://127.0.0.1:1 \
  --env PYROSCOPE_APPLICATION_NAME=imdb-clone-backend-smoke \
  --env PYROSCOPE_SERVER_ADDRESS=http://127.0.0.1:1 \
  --env PYROSCOPE_PROFILE_EXPORT_TIMEOUT=1s \
  --env PYROSCOPE_INGEST_MAX_TRIES=1 \
  "${IMAGE}" >/dev/null

backend_ready=false
readiness_response=""
for _ in {1..120}; do
  if readiness_response="$(curl --fail --silent \
    "http://127.0.0.1:${PORT}/actuator/health/readiness")"; then
    backend_ready=true
    break
  fi
  if [[ "$(docker inspect --format '{{.State.Running}}' "${BACKEND_CONTAINER}")" != "true" ]]; then
    break
  fi
  sleep 1
done
if [[ "${backend_ready}" != "true" ]]; then
  docker logs "${BACKEND_CONTAINER}"
  exit 1
fi

liveness_response="$(curl --fail --silent \
  "http://127.0.0.1:${PORT}/actuator/health/liveness")"
if [[ "${readiness_response}" != *'"readinessState"'* ||
  "${readiness_response}" != *'"db"'* ]]; then
  echo "backend readiness must include application state and PostgreSQL" >&2
  exit 1
fi
if [[ "${liveness_response}" != *'"livenessState"'* ]]; then
  echo "backend liveness must include application state" >&2
  exit 1
fi

container_uid="$(docker exec "${BACKEND_CONTAINER}" id -u)"
if [[ "${container_uid}" == "0" ]]; then
  echo "backend container must not run as root" >&2
  exit 1
fi

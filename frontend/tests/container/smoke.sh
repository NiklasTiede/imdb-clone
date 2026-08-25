#!/usr/bin/env bash

set -euo pipefail

readonly IMAGE="${FRONTEND_IMAGE:-imdb-clone-frontend:local}"
readonly PLATFORM="${FRONTEND_DOCKER_PLATFORM:-linux/amd64}"
readonly PORT="${FRONTEND_SMOKE_PORT:-18080}"
readonly CONTAINER_NAME="imdb-clone-frontend-smoke-$$"

cleanup() {
  docker rm --force --volumes "${CONTAINER_NAME}" >/dev/null 2>&1 || true
}
trap cleanup EXIT

docker run \
  --detach \
  --name "${CONTAINER_NAME}" \
  --platform "${PLATFORM}" \
  --read-only \
  --cap-drop ALL \
  --security-opt no-new-privileges \
  --tmpfs /tmp:rw,noexec,nosuid,size=16m \
  --publish "127.0.0.1:${PORT}:8080" \
  "${IMAGE}" >/dev/null

frontend_ready=false
for _ in {1..30}; do
  if curl --fail --silent "http://127.0.0.1:${PORT}/" >/dev/null; then
    frontend_ready=true
    break
  fi
  if [[ "$(docker inspect --format '{{.State.Running}}' "${CONTAINER_NAME}")" != "true" ]]; then
    break
  fi
  sleep 1
done
if [[ "${frontend_ready}" != "true" ]]; then
  docker logs "${CONTAINER_NAME}"
  exit 1
fi

spa_response="$(curl --fail --silent "http://127.0.0.1:${PORT}/movie?id=42")"
if [[ "${spa_response}" != *'<div id="root">'* ]]; then
  echo "frontend container did not serve the SPA fallback" >&2
  exit 1
fi

container_uid="$(docker exec "${CONTAINER_NAME}" id -u)"
if [[ "${container_uid}" == "0" ]]; then
  echo "frontend container must not run as root" >&2
  exit 1
fi

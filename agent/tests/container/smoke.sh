#!/usr/bin/env bash

set -euo pipefail

readonly IMAGE="${AGENT_IMAGE:-imdb-clone-agent:local}"
readonly PLATFORM="${AGENT_DOCKER_PLATFORM:-linux/amd64}"
readonly PORT="${AGENT_SMOKE_PORT:-18090}"
readonly CONTAINER_NAME="imdb-clone-agent-smoke-$$"

cleanup() {
  docker rm --force "${CONTAINER_NAME}" >/dev/null 2>&1 || true
}
trap cleanup EXIT

docker run \
  --detach \
  --name "${CONTAINER_NAME}" \
  --platform "${PLATFORM}" \
  --publish "127.0.0.1:${PORT}:8090" \
  "${IMAGE}" >/dev/null

ready=false
for _ in {1..30}; do
  if curl --fail --silent "http://127.0.0.1:${PORT}/readyz" >/dev/null; then
    ready=true
    break
  fi
  sleep 1
done

if [[ "${ready}" != "true" ]]; then
  docker logs "${CONTAINER_NAME}"
  exit 1
fi

curl --fail --silent "http://127.0.0.1:${PORT}/healthz" >/dev/null
metrics="$(curl --fail --silent "http://127.0.0.1:${PORT}/metrics")"
if [[ "${metrics}" != *"imdb_agent_build_info"* ]]; then
  echo "agent metrics do not contain imdb_agent_build_info" >&2
  exit 1
fi

container_uid="$(docker exec "${CONTAINER_NAME}" id -u)"
if [[ "${container_uid}" == "0" ]]; then
  echo "agent container must not run as root" >&2
  exit 1
fi

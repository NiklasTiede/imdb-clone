# Command index for local development, seed data, Docker images, and release support.

SEED_IMAGE = niklastiede/imdb-clone-seed
SEED_VERSION ?= local
SEED_LIGHT_TAG = $(SEED_IMAGE):light-$(SEED_VERSION)
SEED_FULL_TAG = $(SEED_IMAGE):full-$(SEED_VERSION)
SEED_CONTEXT_ROOT = build/movie-seed/docker-context
LOCAL_USERS_SQL = src/main/resources/sql/local-users.sql
APP_DOCKER_PLATFORM ?= linux/amd64
APP_DOCKER_BUILD_PLATFORM_FLAG ?= --platform $(APP_DOCKER_PLATFORM)
SEED_DOCKER_BUILD_PLATFORM_FLAG ?=
SEED_PUBLISH_PLATFORMS ?= linux/amd64,linux/arm64
K8S_RENDER_OUTPUT ?= /tmp/imdb-clone-home-apps.yaml
KUBECONFORM_IMAGE ?= ghcr.io/yannh/kubeconform:v0.6.7
OPENAPI_CHECK_DIR ?= /tmp/imdb-clone-openapi-check

.DEFAULT_GOAL := help

##@ Prerequisites

.PHONY: check-local-tools check-seed-tools check-verification-tools

check-local-tools: ## check tools needed for the README local workflow
	@missing=0; \
	for tool in docker java curl sed node yarn; do \
		if ! command -v $$tool >/dev/null 2>&1; then \
			echo "missing: $$tool"; \
			missing=1; \
		fi; \
	done; \
	if ! docker compose version >/dev/null 2>&1; then \
		echo "missing: docker compose"; \
		missing=1; \
	fi; \
	if [ $$missing -eq 0 ]; then \
		echo "All local development tools are available."; \
	else \
		exit 1; \
	fi

check-seed-tools: check-local-tools ## check extra tools needed to build/publish seed images
	@missing=0; \
	for tool in python3; do \
		if ! command -v $$tool >/dev/null 2>&1; then \
			echo "missing: $$tool"; \
			missing=1; \
		fi; \
	done; \
	if ! docker buildx version >/dev/null 2>&1; then \
		echo "missing: docker buildx"; \
		missing=1; \
	fi; \
	if [ $$missing -eq 0 ]; then \
		echo "All seed image tools are available."; \
	else \
		exit 1; \
	fi

check-verification-tools: check-local-tools ## check extra tools needed for verification gates
	@missing=0; \
	for tool in git kubectl diff; do \
		if ! command -v $$tool >/dev/null 2>&1; then \
			echo "missing: $$tool"; \
			missing=1; \
		fi; \
	done; \
	if [ $$missing -eq 0 ]; then \
		echo "All verification tools are available."; \
	else \
		exit 1; \
	fi

##@ Local development

.PHONY: docker-compose-dev-up docker-compose-dev-down seed-local-users seed-light seed-full reindex-local-search

docker-compose-dev-up: ## start PostgreSQL, Elasticsearch, and RustFS for local development
	docker compose up -d

docker-compose-dev-down: ## stop local Docker Compose services
	docker compose down

seed-local-users: ## create local roles and demo accounts without touching movie data
	docker exec -i imdb-clone-postgresql psql -U myroot -d movie_db < $(LOCAL_USERS_SQL)

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

reindex-local-search: ## rebuild local Elasticsearch movie index from PostgreSQL
	@TOKEN=$$(curl -fsS -H 'Content-Type: application/json' \
		-d '{"usernameOrEmail":"les_grossman","password":"Encrypted!Pa55worD"}' \
		http://localhost:8080/api/auth/login \
		| sed -n 's/.*"accessToken":"\([^"]*\)".*/\1/p'); \
	curl -fsS -X POST -H "Authorization: Bearer $$TOKEN" \
		http://localhost:8080/api/search/movies/reindex

##@ Seed images

.PHONY: prepare-seed-light prepare-seed-full build-seed-light build-seed-full publish-seed-light publish-seed-full push-seed-light push-seed-full

prepare-seed-light: ## prepare lightweight seed Docker context
	python3 infrastructure/movie-seed/runtime/prepare_seed_context.py --profile light

prepare-seed-full: ## prepare full seed Docker context
	python3 infrastructure/movie-seed/runtime/prepare_seed_context.py --profile full

build-seed-light: prepare-seed-light ## build lightweight seed image
	docker build $(SEED_DOCKER_BUILD_PLATFORM_FLAG) -t $(SEED_LIGHT_TAG) $(SEED_CONTEXT_ROOT)/light

build-seed-full: prepare-seed-full ## build full seed image
	docker build $(SEED_DOCKER_BUILD_PLATFORM_FLAG) -t $(SEED_FULL_TAG) $(SEED_CONTEXT_ROOT)/full

publish-seed-light: prepare-seed-light ## build and push multi-arch lightweight seed image
	docker buildx build --platform $(SEED_PUBLISH_PLATFORMS) -t $(SEED_LIGHT_TAG) --push $(SEED_CONTEXT_ROOT)/light

publish-seed-full: prepare-seed-full ## build and push multi-arch full seed image
	docker buildx build --platform $(SEED_PUBLISH_PLATFORMS) -t $(SEED_FULL_TAG) --push $(SEED_CONTEXT_ROOT)/full

push-seed-light: publish-seed-light ## build and push multi-arch lightweight seed image

push-seed-full: publish-seed-full ## build and push multi-arch full seed image

##@ Backend

.PHONY: run-backend generate-jar docker-build-backend docker-run-backend

run-backend: ## bootRun java backend
	./gradlew bootRun

generate-jar: ## clean and build jar file (for building docker image)
	./gradlew clean
	./gradlew bootJar

DOCKER_IMG_BACKEND = imdb-clone-backend

docker-build-backend: ## build backend docker image from Dockerfile
	docker build $(APP_DOCKER_BUILD_PLATFORM_FLAG) -t $(DOCKER_IMG_BACKEND) .

docker-run-backend: ## run backend docker container
	docker run --name $(DOCKER_IMG_BACKEND) -p 8080:8080 $(DOCKER_IMG_BACKEND)

##@ Frontend

.PHONY: npm-install generate-client npm-lint run-frontend docker-build-frontend docker-run-frontend

npm-install: ## install NPM dependencies
	cd ./frontend; yarn install

generate-client: ## generate client code from openapi spec
	cd ./frontend; yarn run build:moviesGen

npm-lint: ## lint frontend code
	cd ./frontend; yarn run lint

run-frontend: ## run frontend
	cd ./frontend; yarn run start

DOCKER_IMG_FRONTEND = imdb-clone-frontend

docker-build-frontend: ## build frontend docker image from Dockerfile
	cd ./frontend; docker build $(APP_DOCKER_BUILD_PLATFORM_FLAG) -t $(DOCKER_IMG_FRONTEND) .

docker-run-frontend: ## run frontend docker container
	docker run --name $(DOCKER_IMG_FRONTEND) -p 3000:3000 $(DOCKER_IMG_FRONTEND)

##@ Verification

.PHONY: verify-kubernetes-render verify-kubernetes-schema verify-openapi-drift

verify-kubernetes-render: check-verification-tools ## render home-cluster Kubernetes manifests
	kubectl kustomize infrastructure/clusters/home/apps > $(K8S_RENDER_OUTPUT)

verify-kubernetes-schema: verify-kubernetes-render ## validate rendered Kubernetes manifests with pinned kubeconform
	docker run --rm -i $(KUBECONFORM_IMAGE) \
		-strict \
		-summary \
		-ignore-missing-schemas \
		< $(K8S_RENDER_OUTPUT)

verify-openapi-drift: ## compare checked-in OpenAPI/client output with a running backend
	rm -rf $(OPENAPI_CHECK_DIR)
	mkdir -p $(OPENAPI_CHECK_DIR)
	curl -fsS http://localhost:8080/v3/api-docs.yaml > $(OPENAPI_CHECK_DIR)/imdb-clone-backend.yaml
	diff -u frontend/src/client/imdb-clone-backend.yaml $(OPENAPI_CHECK_DIR)/imdb-clone-backend.yaml
	cd ./frontend; yarn openapi-generator-cli generate -i $(OPENAPI_CHECK_DIR)/imdb-clone-backend.yaml -g typescript-axios -o $(OPENAPI_CHECK_DIR)/generator-output
	diff -qr --exclude=FILES frontend/src/client/movies/generator-output $(OPENAPI_CHECK_DIR)/generator-output

##@ Docker housekeeping

.PHONY: docker-show docker-clean

docker-show: ## show all images and containers
	docker image ls -a
	docker container ls -a

docker-clean: ## remove imdb-clone docker images and containers
	docker rmi -f $(DOCKER_IMG_FRONTEND) $(DOCKER_IMG_BACKEND)
	docker rm -f $(DOCKER_IMG_FRONTEND) $(DOCKER_IMG_BACKEND)

##@ Help

.PHONY: help

help: ## show this command index
	@awk 'BEGIN {FS = ":.*?## "; printf "\nUsage:\n  make \033[36m<target>\033[0m\n"} /^##@ / {printf "\n\033[1m%s\033[0m\n", substr($$0, 5)} /^[a-zA-Z0-9_-]+:.*?## / {printf "  \033[36m%-28s\033[0m %s\n", $$1, $$2}' $(MAKEFILE_LIST)

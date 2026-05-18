# This file serves also as a cheat sheet

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

# ------------ Set-up and run PostgreSQL / ElasticSearch / Object Storage  --------------------------------------------

.PHONY: pull-db run-db stop-db start-db remove-db-container seed-local-users prepare-seed-light prepare-seed-full build-seed-light build-seed-full publish-seed-light publish-seed-full push-seed-light push-seed-full seed-light seed-full

docker-compose-dev-up: ## run services for backend
	docker compose up -d

docker-compose-dev-down: ## stop services for backend
	docker compose down

seed-local-users: ## create local roles and demo accounts without touching movie data
	docker exec -i imdb-clone-postgresql psql -U myroot -d movie_db < $(LOCAL_USERS_SQL)

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



# ------------ Backend - Gradle/Docker --------------------------------------------------------------------------------

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



# ------------ Frontend - NPM/Docker  ---------------------------------------------------------------------------------

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



# ------------  Docker  -----------------------------------------------------------------------------------------------

.PHONY: docker-show docker-clean

docker-show: ## show all images and containers
	docker image ls -a
	docker container ls -a

docker-clean: ## remove imdb-clone docker images and containers
	docker rmi -f $(DOCKER_IMG_FRONTEND) $(DOCKER_IMG_BACKEND)
	docker rm -f $(DOCKER_IMG_FRONTEND) $(DOCKER_IMG_BACKEND)



# ------------  Help  -------------------------------------------------------------------------------------------------

.PHONY: help

help: ## This help.
	@awk 'BEGIN {FS = ":.*?## "} /^[a-zA-Z_-]+:.*?## / {printf "\033[36m%-30s\033[0m %s\n", $$1, $$2}' $(MAKEFILE_LIST)

.DEFAULT_GOAL := help

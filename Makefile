# This file serves also as a cheat sheet

DEV_SEED_PYTHON = infrastructure/object-storage/dev-seed/.venv/bin/python
DEV_SEED_REQUIREMENTS = infrastructure/object-storage/dev-seed/requirements.txt

# ------------ Set-up and run PostgreSQL / ElasticSearch / Object Storage  --------------------------------------------

.PHONY: pull-db run-db stop-db start-db remove-db-container seed-postgresql-dev-data

docker-compose-dev-up: ## run services for backend
	docker compose up -d

docker-compose-dev-down: ## stop services for backend
	docker compose down

seed-postgresql-dev-data: ## load lightweight local movie/user data into PostgreSQL
	docker exec -i imdb-clone-postgresql psql -U myroot -d movie_db < src/main/resources/sql/2_init_data.sql

$(DEV_SEED_PYTHON): $(DEV_SEED_REQUIREMENTS)
	python3 -m venv infrastructure/object-storage/dev-seed/.venv
	$(DEV_SEED_PYTHON) -m pip install -r $(DEV_SEED_REQUIREMENTS)

generate-dev-movie-images: $(DEV_SEED_PYTHON) ## generate lightweight movie images for local object storage seed
	$(DEV_SEED_PYTHON) infrastructure/object-storage/dev-seed/generate_movie_images.py

seed-object-storage-dev-movie-images: ## upload generated lightweight movie images to local object storage
	infrastructure/object-storage/dev-seed/upload_to_object_storage.sh

seed-object-storage-dev-movie-images: seed-object-storage-dev-movie-images



# ------------ Backend - Gradle/Docker --------------------------------------------------------------------------------

.PHONY: run-backend generate-jar docker-build-backend docker-run-backend

run-backend: ## bootRun java backend
	./gradlew bootRun

generate-jar: ## clean and build jar file (for building docker image)
	./gradlew clean
	./gradlew bootJar

DOCKER_IMG_BACKEND = imdb-clone-backend

docker-build-backend: ## build backend docker image from Dockerfile
	docker build -t $(DOCKER_IMG_BACKEND) .

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
	cd ./frontend; docker build -t $(DOCKER_IMG_FRONTEND) .

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

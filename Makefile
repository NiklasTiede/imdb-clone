# This file serves also as a cheat sheet

# ------------ Set-up and run MySQL / ElasticSearch / MinIO  ----------------------------------------------------------

.PHONY: pull-db run-db stop-db start-db remove-db-container

docker-compose-dev-up: ## run services for backend
	cd ./infrastructure/deployment/development; docker-compose up -d

docker-compose-dev-down: ## stop services for backend
	cd ./infrastructure/deployment/development; docker-compose down



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
# This file serves also as a cheat sheet

# ------------ Set-up and run MySQL DB  -------------------------------------------------------------------------------

.PHONY: pull-db run-db stop-db start-db remove-db-container

DB_IMD = imdb-db

pull-db: ## pull mysql image
	docker pull mysql

run-db: ## run container opened to port 3306
	docker run --name $(DB_IMD) -p 3306:3306 -e MYSQL_ROOT_PASSWORD=secret -e MYSQL_DATABASE=moviesdb -d mysql:latest

stop-db: ## stop running container
	docker stop $(DB_IMD)

start-db: ## start container
	docker start $(DB_IMD)

remove-db-container: ## removes container (instance of image)
	docker rm -f $(DB_IMD)



# ------------ Backend - Gradle/Docker --------------------------------------------------------------------------------

.PHONY: run-backend generate-jar docker-build-backend docker-run-backend

run-backend: ## bootRun java backend
	gradle bootRun

generate-jar: ## clean and build jar file (for dockerizing)
	gradle clean
	gradle bootJar

DOCKER_IMG_BACKEND = imdb-backend

docker-build-backend: ## build backend docker image from Dockerfile
	docker build -t $(DOCKER_IMG_BACKEND) .

docker-run-backend: ## run backend docker container
	docker run --name $(DOCKER_IMG_BACKEND) -p 8080:8080 $(DOCKER_IMG_BACKEND)



# ------------ Frontend - NPM/Docker  ---------------------------------------------------------------------------------

.PHONY: npm-install generate-client npm-lint run-frontend docker-build-frontend docker-run-frontend

npm-install: ## install NPM dependencies
	cd ./frontend; npm install

generate-client: ## generate client code from openapi spec
	cd ./frontend; npm run build:moviesGen

npm-lint: ## lint frontend code
	cd ./frontend; npm run lint

run-frontend: ## run frontend
	cd ./frontend; npm run start

DOCKER_IMG_FRONTEND = imdb-frontend

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
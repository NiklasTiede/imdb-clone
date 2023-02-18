# This file serves also as a cheat sheet

# ------------ Set-up and run MySQL DB (containing movie data)  -------------------------------------------------------

.PHONY: pull-db run-db stop-db start-db remove-db-container

DB_IMG = niklastiede/movie-db

pull-db: ## pull mysql image
	docker pull $(DB_IMG):latest

run-db: ## run container opened to port 3310
	docker run --name movie-db -d --restart=always -p 3310:3306 $(DB_IMG) --secure-file-priv=tmp

stop-db: ## stop running container
	docker stop $(DB_IMG)

start-db: ## start container
	docker start $(DB_IMG)

remove-db-container: ## removes container
	docker rm $(DB_IMG)



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



# ------------  Elasticsearch  ----------------------------------------------------------------------------------------

.PHONY: es-run

es-run: ## show all images and containers
	docker network create elastic
	docker run --name elasticsearch -d --restart=always --net elastic -p 9200:9200 -e discovery.type=single-node -e ES_JAVA_OPTS="-Xms1g -Xmx1g" -e ELASTIC_PASSWORD=elastic -it docker.elastic.co/elasticsearch/elasticsearch:8.5.3
	docker pull docker.elastic.co/kibana/kibana:8.5.3
	docker run --name kibana --net elastic -p 5601:5601 docker.elastic.co/kibana/kibana:8.5.3

es-logs:
	docker logs <container-id-or-name>


# ------------  Help  -------------------------------------------------------------------------------------------------

.PHONY: help

help: ## This help.
	@awk 'BEGIN {FS = ":.*?## "} /^[a-zA-Z_-]+:.*?## / {printf "\033[36m%-30s\033[0m %s\n", $$1, $$2}' $(MAKEFILE_LIST)

.DEFAULT_GOAL := help
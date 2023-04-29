
# Elasticsearch

We will set up an Elasticsearch container with docker. Data are indexed with MySQL movie
data when the spring boot backend container starts (and cannot find indexed movie data)
using an application listener.

The `docker run` command used for running the ES image can be found in the 
[deployment/docker-compose.yaml](https://github.com/niklastiede/imdb-clone/blob/master/infrastructure/deployment/development/docker-compose.yaml#L55).

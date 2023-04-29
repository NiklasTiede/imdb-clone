
# Deployment: Locally for Development

When running locally we spin up 3 docker containers.
- 
- MySQL - Rel. Database
- Elasticsearch - SearchEngine
- MinIO - FileStorage

we run them by executing the provided docker-compose.yaml

```bash
docker-compose up -d
```

The provided simple credentials will be read from the `.env`
file and injected into tye container. We can then start the 
backend:

```bash
./gradlew build bootRun
```

And then start the frontend

```bash

```


# Deployment: Production

In this configuration we use some generated safe credentials.
Besides the the before mentioned containers we will also
deploy a spring boot and react container.

You Can download the content of this `/production` directory onto
you computer or a server and deploy everthing by running

```bash
docker-compose up -d
```



# Deployment: Locally for Development

When running this app locally we need to spin up 3 docker 
containers.
 
- MySQL - Rel. Database
- Elasticsearch - SearchEngine
- MinIO - FileStorage

We run them by executing the provided `docker-compose.yaml` file (in the 
`development`-directory).

```bash
docker-compose up -d
```

The development setup uses some unsafe but simple credentials. 
Now we can start the backend:

```bash
./gradlew bootRun
```

And then start the frontend

```bash

```


# Deployment: Production on Home Server

In this configuration we use some generated safe credentials.
Besides the before mentioned containers we will also
deploy a spring boot and react container and handle the 
incoming traffic with traefik to enable encryption (SSL).

You can download the content of this `/production` directory onto 
a server and deploy everything by running.

```bash
# generate credentials
./generate_credentials.sh

# deploy stateful apps
docker-compose -f docker-compose.stateful-apps.yaml up -d

# deploy stateless apps
docker-compose -f docker-compose.stateless-apps.yaml up -d
```

But this process also involves port-forwarding of your server, setting up DNS
with your domain and configuring ddclient to update DNS for public IP address. 
So for each case some additional work has to be done.

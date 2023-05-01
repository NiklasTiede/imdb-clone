
# How to Run on Production

## Initial Deployment

5 Docker container will be deployed to run the App. First, download the
docker-compose:

```bash
curl -L -o docker-compose.stateful-apps.yaml https://raw.githubusercontent.com/NiklasTiede/imdb-clone/master/infrastructure/deployment/production/docker-compose.stateful-apps.yaml
curl -L -o docker-compose.stateless-apps.yaml https://raw.githubusercontent.com/NiklasTiede/imdb-clone/master/infrastructure/deployment/production/docker-compose.stateless-apps.yaml
curl -L -o generate_credentials.sh https://raw.githubusercontent.com/NiklasTiede/imdb-clone/master/infrastructure/deployment/production/generate_credentials.sh
curl -L -o deploy-new-images.sh https://raw.githubusercontent.com/NiklasTiede/imdb-clone/master/infrastructure/deployment/production/deploy-new-images.sh
```

let's start creating credentials:

```bash
./generate_credentials.sh
```

If you want to enable email confirmation then
add a valid email. Furthermore, add the name of your server.

Now we will start at first the stateful services. This can take up to 20 min:

```bash
docker-compose -f docker-compose.stateful-apps.yaml up -d
```

When each container is running and loaded (check this by looking into the container 
and/or connecting with client)...

```bash
docker logs imdb-clone-mysql
```

...then we can deploy our spring boot backend and the React frontend:

```bash
docker-compose -f docker-compose.stateful-apps.yaml up -d
```

Now it should be reachable using the host-address of your server in your internal network.

## deploy new BE / FE Version

For this, we just have to shut down the old spring boot and react container and pull / run the new images:

```bash
docker-compose -f docker-compose.stateful-apps.yaml down
docker-compose -f docker-compose.stateful-apps.yaml up -d
```

I added a little bash script which helps to remember:

```bash
./deploy-new-images.sh
```

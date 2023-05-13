
<p align="center">
  <a href="https://imdb-clone.the-coding-lab.com/" target="_blank">
    <img alt="imdb-clone-logo" width="500" src="docs/imdb-clone-logo.jpg" />
  </a>

  <h3 align="center">This <a href="https://imdb-clone.the-coding-lab.com/" target="_blank">Project</a>  exemplifies a Real-World Java / React Web App.</h3>
</p>

---

<p align="center">

  <a href="https://stats.uptimerobot.com/5KMN7t0E5M">
    <img alt="Uptime Robot Status" src="https://img.shields.io/uptimerobot/status/m794347971-509793e3b2e4d89beb04d2fb" />
  </a>
  <a href="https://github.com/NiklasTiede/IMDb-Clone/issues">
    <img alt="issues" src="https://img.shields.io/github/issues-raw/niklastiede/imdb-clone" />
  </a>
  <a href="https://github.com/NiklasTiede/IMDb-Clone/commits/master">
    <img alt="commit" src="https://img.shields.io/github/last-commit/NiklasTiede/IMDb-Clone">
  </a>
  <a>
    <img alt="code-size" src="https://img.shields.io/github/languages/code-size/niklastiede/imdb-clone" />
  </a>
  <a>
    <img alt="license" src="https://img.shields.io/github/license/niklastiede/imdb-clone" />
  </a>
</p>

## Techstack
- Languages: Java JDK20 / Typescript v4
- Frameworks: Spring Boot v3 / React v18 (MaterialUI v5)
- Rel. Database: MySQL v8
- SearchEngine: Elasticsearch v8
- File Storage: MinIO

The app is secured with JWT authentication. The techstack is kept up-to-date. 

---

## Motivation

When entering the field of software engineering you need to learn how to build applications professionally.
You need to learn from good code bases (at best: similar to company code). There are the typical blog examples 
([here](https://github.com/gothinkster/realworld)) but what is about search functionality or the handling 
of images? How is the App deployed on a home server? How to generate client code with openapi-specifications?
How can I preload my App with data? The answer to all these questions and more can be found in this codebase.

The project can be rather easily rebuild locally (for a project of this size). If you want to explore a deployed 
instance of the IMDB Clone then visit [imdb-clone.the-coding-lab.com](https://imdb-clone.the-coding-lab.com/). 
Here's a diagram of the setup:

<p align="center">
  <img  alt="architecture-diagram" width="500" src="docs/imdb-clone-flow-schema.svg" />

<h4 align="center">Architecture Diagram showing the App's Service Interactions.</h4>
</p>

---

## How to Run this Project Locally

The app can be built in 3 steps:

1. Run `docker-compose` to set up preloaded backend services (MySQL, Elasticsearch and
  MinIO)
2. Run the Spring Boot Backend with `./gradlew bootRun`
3. Run the React Frontend with `yarn install` & `yarn start`

---

### 1. Set Up Stateful Services: MySQL, Elasticsearch and MinIO

At first, we have to run the with data preloaded stateful services (MySQL, Elasticsearch and 
MinIO) which are used by the backend. I created a docker image of each service preloaded with 
data, so we just have to execute the `docker-compose.yaml`.

```shell
cd infrastructure/deployment/development
docker-compose up -d
```

Be aware: the images are x86-based. So when you're using ARM-based apple silicone machine, 
pay attention that emulation is activated. All images will be pulled and mounted to your device. 
The spring boot backend can connect to the containers now.

For more information on how data were collected, processed and imported look into 
the [infrastructure](./infrastructure/README.md)-folder.

--- 

### 2. Set Up Spring Boot Backend

Now we can start the Spring Boot app:

```shell
./gradlew build
./gradlew bootRun
```

The backend can now be reached at port 8080 on localhost. You can test if the backend works properly by 
sending some http requests. Use the provided [.http](./src/main/resources/api-calls) files.

---

### 3. Set Up React Frontend

Now we can run the React frontend. We have to move into the frontend-folder and build & run with yarn or npm. 

```shell
cd ./frontend
yarn install
yarn run build:moviesGen
yarn start
```

The FE is served to `http://localhost:3000/. We can search for movies and more.

I also added a [Makefile](Makefile) as a little cheat sheet to refresh our memory for all the important commands 
we use during development.

---

### Todo:

- [x] Set up Database and import Movie Data
- [x] Create Java Backend
- [x] Set up Elasticsearch, Photos / File Storage
- [x] Deploy on Home Server with Docker-Compose
- [x] enable HTTPS with reverse-proxy
- [ ] Create React Frontend
  - [x] Account Settings Page
  - [x] Movie Search Page
  - [ ] Movie Detail View with Rating / Watchlist Feature
  - [ ] Watchlist Page
  - [ ] Edit / Create Movies Page
  - [ ] Home Page
  - [ ] Detail View: Comments Feature
  - [ ] Make Mobile Compatible

### Future Ideas
- [ ] Deploy on HA K3s Home Server
- [ ] Use Flux for GitOps CD
- [ ] Add Integration Namespace in K3s next to the Prod Env for Testing
- [ ] Add Unit / Integration Tests in BE and FE
- [ ] Add Monitoring (Graylog, Prometheus, Grafana, cAdvisor, glances)
- [ ] Add more Features like Chat Functionality

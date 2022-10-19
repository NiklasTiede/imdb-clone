
<p align="center">
<img  alt="imdb-clone" align="center" width="500" src="docs/imdb-clone-logo.jpg" />
<h3 align="center">This Project exemplifies a Real-World Java / React Web App.</h3>
<p>

---

<p id="Badges" align="center">
  <a href="https://github.com/NiklasTiede/IMDb-Clone/commits/master">
    <img src="https://img.shields.io/github/last-commit/NiklasTiede/IMDb-Clone">
  </a>
  <a href="https://github.com/NiklasTiede/IMDb-Clone/issues">
    <img src="https://img.shields.io/github/issues-raw/niklastiede/imdb-clone" />
  </a>
  <a>
    <img src="https://img.shields.io/github/languages/code-size/niklastiede/imdb-clone" />
  </a>
  <a>
    <img src="https://img.shields.io/github/license/niklastiede/imdb-clone" />
  </a>
</p>

## Techstack
- Languages: Java JDK17 / Typescript
- Frameworks: Spring Boot / React (MaterialUI)
- Database: MySQL

App is secured with modern JWT authentication. 

## How to Run this Project from Source

You can either download the processed [Movie Dataset](https://www.dropbox.com/s/rzmhet4qf2joczz/processed_imdb_movies.csv?dl=0) 
by yourself and import it or go the easy way and pull/run the docker image I created for this purpose:

```shell
docker pull niklastiede/movie-db:latest
docker run --name niklastiede/movie-db -d --restart=always -p 3310:3306 niklastiede/movie-db --secure-file-priv=tmp
```

---

The backend is finished in the current state. YAfter rebuilding the project you can run it.

```shell
./gradlew clean bootRun
```

Email verification is turned off by default, so you can easily register the first User which will also 
have admin permissions. Just go into [Authentication.http](src/main/resources/api-calls/Authentication.http) and 
make a registration post request.

```shell
POST {{protocol}}{{host}}:{{port}}/api/auth/registration
Content-Type: application/json

{
  "username": "OneManArmy",
  "password": "Str0nG!Pa55Word?",
  "email": "your@email.com"
}
```

Then you can login and you will get a JWT token back, which can be used to use all protected endpoints.

```shell
POST {{protocol}}{{host}}:{{port}}/api/auth/login
Content-Type: application/json

{
"usernameOrEmail": "your@email.com",
"password": "Str0nG!Pa55Word?"
}
```

Now you can search for movies, add comments, rate them or put them on a watchlist!

---

Lastly, we build up the frontend and serve it to `http://localhost:3000/`. Although the frontend is 
currently under construction :wink: 

```shell
cd ./frontend
run build:moviesGen
npm run start
```

I also added a [Makefile](Makefile) as a little cheat sheet to refresh our memory for all the important commands 
we use during development.

### Todo:

- [x] Set up Database and import Movie Data
- [x] Create Java Backend 
- [ ] Create React Frontend
- [ ] Add CI, CD and Monitoring
- [ ] Add elasticsearch and http client, microservice architecture

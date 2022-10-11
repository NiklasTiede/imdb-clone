
<p align="center">
<img  alt="imdb-clone" align="center" width="500" src="docs/imdb-clone-logo.png" />
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

## How to Run this Project from Source

To set up the project we have to build up the database (see [here](database/README.md)), 
the [backend](src/main/java/com/thecodinglab/imdbclone/Application.java) and the [frontend](frontend/package.json).

We have to download and import the processed [Movie Dataset](https://www.dropbox.com/s/rzmhet4qf2joczz/processed_imdb_movies.csv?dl=0) 
into the database. I use DBeaver to connect to the database and use its import functionality. 

Now we can run our backend.

```bash
./gradlew clean bootRun
```

Lastly, we build up the frontend and serve it to `http://localhost:3000/`.

```bash
cd ./frontend
run build:moviesGen
npm run start
```

I also added a `Makefile` as a little cheat sheet to refresh our memory for all the important commands 
we use during development.

### Todo:

- [x] Set up Database and import Movie Data
- [x] Create Java Backend 
- [ ] Create React Frontend
- [ ] Add CI, CD and Monitoring

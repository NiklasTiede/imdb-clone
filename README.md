
# Movie Database Clone

This webapp persists Movie data and presents them the user by searching.

## Techstack
- Languages: Java JDK17 / Typescript
- Frameworks: Spring Boot / React
- Database: MySQL

## How to Run this Project from Source

To set up the project we have to install the database, build up the backend and the frontend.
At first, we will pull and run a MySQL db image.

```bash
❯ docker pull mysql
❯ docker run --name imdb-db -p 3306:3306 -e MYSQL_ROOT_PASSWORD=secret -e MYSQL_DATABASE=moviesdb -d mysql:latest
```

Next, we have to import some processed movie data into our running db-container using `./data/movieset_100_indexed.csv`. 
I use DBeaver to connect to the database and use its import functionality. 

Now we can run our backend.

```bash
❯ gradle bootRun
```

If we add `http://localhost:8080/movies` to the browser a list of our previously imported movie data should be shown.

Lastly, we build up the frontend and serve it to `http://localhost:3000/`.

```bash
❯ cd ./frontend
❯ run build:moviesGen
❯ npm run start
```

I also added a `Makefile` as a little cheat sheet to refresh our memory for all the important commands 
we use during development.

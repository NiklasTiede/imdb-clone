
# docker build -t niklastiede/movie-db .

FROM mysql:latest

ENV MYSQL_DATABASE movie_db
ENV MYSQL_ROOT_PASSWORD root
ENV MYSQL_ROOT_PASSWORD secret

COPY init.sql /docker-entrypoint-initdb.d/init.sql

COPY ../../infrastructure/database-docker/processed_imdb_movies.csv /tmp/processed_imdb_movies.csv

RUN chown -R mysql:mysql /docker-entrypoint-initdb.d/

CMD [ "mysqld", "--init-file=/docker-entrypoint-initdb.d/init.sql" ]

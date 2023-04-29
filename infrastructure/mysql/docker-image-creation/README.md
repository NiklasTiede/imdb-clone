
## Creation of MySQL Docker Image with Movie Data

At first, download this compressed [Dataset](https://www.dropbox.com/s/87wwsn2z3eziskb/processed_imdb_movies.csv.gz?dl=0)
into this folder and decompress it:

```bash
gzip --decompress --keep processed_imdb_movies.csv.gz
```

Then build the docker image:

```bash
docker build -t niklastiede/movie-db .
```

The image contains the dataset as .csv file. You can run 
the container like (container exposed to port 3310!)

```bash
docker run -d \
    --name movie-db \
    --restart=always \
    -p 3310:3306 \
    -v imdb-clone-mysql-data:/var/lib/mysql \
    -e MYSQL_ROOT_PASSWORD=supersecret \
    -e MYSQL_DATABASE=movie_db \
    -e MYSQL_USER=myroot \
    -e MYSQL_PASSWORD=secret \
    mysql:latest \
    --secure-file-priv=tmp
```

The creation of the container can take up to 15 min (so be patient). 
This is due to the import and indexing of the movie data!

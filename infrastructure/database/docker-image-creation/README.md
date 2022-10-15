
## Creation of MySQL Docker Image with Movie Data

At first, navigate into this folder and decompress the 
processed dataset:

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
docker run --name niklastiede/movie-db -d --restart=always -p 3310:3306 niklastiede/movie-db --secure-file-priv=tmp
```

The creation of the container can take up to 10 min (so be patient). 
This is due to the import of the movie data!

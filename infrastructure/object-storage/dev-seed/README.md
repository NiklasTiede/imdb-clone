# Lightweight Movie Image Seed

This folder creates object-storage poster objects for the movies in
`src/main/resources/sql/2_init_data.sql`.

The database stores only `movie.image_url_token`. The frontend builds URLs with
this pattern:

```text
http://localhost:9000/imdb-clone/movies/{image_url_token}_size_{width}x{height}.jpg
```

## Generate Images

```bash
make generate-dev-movie-images
```

The script downloads posters from TMDB image URLs listed in `movies.csv` and
writes:

```text
infrastructure/object-storage/dev-seed/movies/{token}_size_600x900.jpg
infrastructure/object-storage/dev-seed/movies/{token}_size_120x180.jpg
```

Generated images are local dev artifacts and are ignored by git.

## Upload Images to Local Object Storage

Start the local infrastructure first:

```bash
make docker-compose-dev-up
```

Then upload the generated images:

```bash
infrastructure/object-storage/dev-seed/upload_to_object_storage.sh
```

The upload script uses the local `mc` command if available. Otherwise, it falls
back to a Dockerized `mc` client on the `imdb-clone-network` network.

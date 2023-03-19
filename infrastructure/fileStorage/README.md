
# File Storage for Images

Storing data like images, videos or pdf's is a valuable feature. Here we want to show images for the 5000 most 
popular movies. Without calling an external Api we want to achieve this by scraping image data and storing 
them in our own mounted MinIO container. This gives us more control on write operations (add/delete) on 
the images and what image size we want to serve to our frontend.

documentation:

## Scrape Image Data

The original IMDb dataset does not contain descriptions about the movies. That's why we will also scrape description 
data about movies and merge them later with an imageUrlToken (to connect a movie with its image) into our movie dataset. 
I prepared a `most_popular_movies_6000.csv` file containing the 6000 entries with highest ratingsCounts (most voted movies) 
which we will use for scraping image/description data.

```bash
# scraping imageUrl and movie overview using the imdbId
$ python 1_data_scraping.py

# download .jpg images and name them according to self generated token
$ python 2_image_scraping.py

# for our purposes we create a larger image (600x900) and a thumbnail image (120x180)
$ python 3_image_processing.py
```

The `overview`- and `imageUrlToken`-columns will later be merged into our dataset. Now we have to set up MinIO to 
be able to load our images into our bucket.

## set Up a MinIO Container

We will run a mounted MinIO instance. Lets pull and run it:

```bash
$ docker pull bitnami/minio

# mount volume
mkdir -p ~/minio/data

# mounted to volume on host machine
docker run \
   -p 9000:9000 \
   -p 9090:9090 \
   --name minio \
   --restart=always \
   -d \
   -v ~/minio/data:/data \
   -e "MINIO_ROOT_USER=ROOTNAME" \
   -e "MINIO_ROOT_PASSWORD=CHANGEME123" \
   minio/minio server /data --console-address ":9090"
```

Our container is exposed on port 9000 and 9090. Lets take a look into our [MinIO](http://localhost:9090/browser) instance.
We will use our just specified credentials to login. We just have to set bucket policies to finish our setup.

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Principal": {
        "AWS": [
          "*"
        ]
      },
      "Action": [
        "s3:GetObject"
      ],
      "Resource": [
        "arn:aws:s3:::imdb-clone/movies/*",
        "arn:aws:s3:::imdb-clone/profile-photos/*"
      ]
    }
  ]
}
```

We will give access to the movie-images folder for read operations without authentication. Write-operations can only 
be performed with authentication. Our backend will do this in the future to add or delete images.

## Load Data into MinIO Container

Next we will transfer the scraped images into our bucket using the minio-client `mc`. Let's install it.

```bash
$ curl https://dl.min.io/client/mc/release/linux-amd64/mc \
  --create-dirs \
  -o $HOME/minio-binaries/mc

$ chmod +x $HOME/minio-binaries/mc
$ export PATH=$PATH:$HOME/minio-binaries/
```

Now we connect the MinIO client with our container.

```bash
$ ./mc alias set ALIAS http://localhost:9090/ ROOTNAME CHANGEME123

$ ./mc config host add minio http://localhost:9000 ROOTNAME CHANGEME123
```

Lastly we copy our images into the buckets `movies`-folder

```bash
$ ./mc cp --recursive ~/PathToProject/IMDB-Clone/infrastructure/fileStorage/movie_images_processed/ minio/imdb-clone/movies/
```

Now we can access our image with GET requests like `http://localhost:9000/imdb-clone/movies/{imageUrlToken}_size_600x900.jpg`

## How we work with our Imdb-clone Bucket

Each time a new image is stored in our bucket we generate a imageUrlToken and save this in the movies db entry. This 
token is then contained in our response object if we ask our backend for movie data. We use the token to parse the URL
together which we then use to call for image data. Here's an example:

`http://localhost:9000/imdb-clone/movies/{imageUrlToken}_size_600x900.jpg`

We just have to replace the imageUrlToken-placeholder by a real token and get a valid address.


# File Storage for Images

Storing data like images, videos or pdf files is a valuable feature. Here we want to show images for the 5000 most 
popular movies. Without calling an external Api we want to achieve this by scraping image data and storing 
them in our own mounted RustFS object-storage container. This gives us more control on write operations (add/delete) on 
the images and what image size we want to serve to our frontend.

documentation:

## Scrape Image Data

The original IMDb dataset does not contain descriptions about the movies. That's why we will also scrape description 
data about movies and merge them later with an imageUrlToken (to connect a movie with its image) into our movie dataset. 
I prepared a `most_popular_movies_6000.csv` file containing the 6000 entries with highest ratingsCounts (most voted movies) 
which we will use for scraping image/description data.

```bash
# scraping imageUrl and movie overview using the imdbId
python 1_data_scraping.py

# download .jpg images and name them according to self generated token
python 2_image_scraping.py

# for our purposes we create a larger image (600x900) and a thumbnail image (120x180)
python 3_image_processing.py
```

The `overview`- and `imageUrlToken`-columns will later be merged into our dataset. Now we have to set up object storage to 
be able to load our images into our bucket.

## Set Up a RustFS Container

We will run a mounted RustFS instance. Lets pull and run it:

```bash
docker pull rustfs/rustfs:1.0.0-beta.2

# mount volume
mkdir -p ~/rustfs/data

# mounted to volume on host machine
docker run \
   -p 9000:9000 \
   -p 9001:9001 \
   --name rustfs \
   --restart=always \
   -d \
   -v ~/rustfs/data:/data \
   -e "RUSTFS_ACCESS_KEY=ROOTNAME" \
   -e "RUSTFS_SECRET_KEY=CHANGEME123" \
   -e "RUSTFS_CONSOLE_ENABLE=true" \
   rustfs/rustfs:1.0.0-beta.2 /data
```

Our container exposes the S3 API on port 9000 and the console on port 9001. Lets take a look into our [RustFS](http://localhost:9001) instance.
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

## Load Data into Object Storage

Next we will transfer the scraped images into our bucket using the `mc` client. Let's install it.

```bash
curl https://dl.min.io/client/mc/release/linux-amd64/mc \
  --create-dirs \
  -o $HOME/rustfs-binaries/mc

chmod +x $HOME/rustfs-binaries/mc
export PATH=$PATH:$HOME/rustfs-binaries/
```

Now we connect the `mc` client with our container.

```bash
mc alias set rustfs http://localhost:9000 ROOTNAME CHANGEME123
```

Lastly we copy our images into the buckets `movies`-folder

```bash
mc cp --recursive ~/PathToProject/IMDB-Clone/infrastructure/fileStorage/movie_images_processed/ rustfs/imdb-clone/movies/
```

Now we can access our image with GET requests like `http://localhost:9000/imdb-clone/movies/{imageUrlToken}_size_600x900.jpg`

## How we work with our Imdb-clone Bucket

Each time a new image is stored in our bucket we generate a imageUrlToken and save this in the movies db entry. This 
token is then contained in our response object if we ask our backend for movie data. We use the token to parse the URL
together which we then use to call for image data. Here's an example:

`http://localhost:9000/imdb-clone/movies/{imageUrlToken}_size_600x900.jpg`

We just have to replace the imageUrlToken-placeholder by a real token and get a valid address.

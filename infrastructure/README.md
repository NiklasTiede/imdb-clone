
# Provisionally Setup

To make things simple I will deploy this app at 
first by just using docker.  

- React (Frontend) Container
- Spring Boot (Backend) Container
- MySQL (preloaded Database) Container
- Elasticsearch (SearchEngine) Container
- MinIO (preloaded FileStorage) Container

For CI / CD I use Github Workflows.

# Set Up MySQL Database

I created a [entity-relationship diagram](datamodel.puml) to simplify schema creation. 
I installed mysql-server on my machine, processed the dataset and imported it into the database.

## Process Movies / Rating Datasets

For this I used the powerful capabilities of the Python framework Pandas which can easily process big datasets. 
All steps are verifiable through a [jupyter notebook](mysql/data-processing/process_movie_dataset.ipynb).

- download `title.basics.tsv.gz` and `title.ratings.tsv.gz` from [IMDb](https://www.imdb.com/interfaces/)
- process dataset using Python, Pandas, Numpy:
  - replace empty values by '\N'
  - remove incorrect values (consistent datatype per column)
  - merge Rating-, Movie- and image/description dataframes
  - set `tconst` as index

Instead of rerunning the jupyter notebook you can also just download the 
[Processed Dataset](https://www.dropbox.com/s/rzmhet4qf2joczz/processed_imdb_movies.csv?dl=0).

## Create Database Tables and import data
- execute `create table` statements and `load infile` using [init.sql](mysql/docker-image-creation/init.sql) file

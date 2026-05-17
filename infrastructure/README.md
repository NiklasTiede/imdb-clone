
# Provisionally Setup

These are the containers I'm running:

- React (Frontend) Container
- Spring Boot (Backend) Container
- PostgreSQL Database Container
- Elasticsearch (SearchEngine) Container
- RustFS (S3-compatible object storage) Container
- Traefik (reverse proxy) Container

For CI / CD I use GitHub Workflows.

# Set Up PostgreSQL Database

The application schema is managed through PostgreSQL Flyway migrations. I created an
[entity-relationship diagram](datamodel.puml) to simplify schema creation.

The legacy IMDb dataset processing notes still live under `infrastructure/mysql` until the import
pipeline is rebuilt for PostgreSQL.

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
- execute the PostgreSQL Flyway migrations from `src/main/resources/db/migration`

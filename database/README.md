
# Set Up MySQL Database (Linux/Ubuntu)

I created a [entity-relationship diagram](datamodel.puml) to simplify schema creation. 
Then I went through the following steps to build everything up.

## Install MySQL DB

- download MySQL
- execute `mysql_secure_installation` script
- connect to database
- split dataset
- execute import bash script (which executes sql script)
- evtl. restart mysql-server


- [ ] write bash script for installing MySQL DB

## Process Movies/Rating Datasets

For this I used the powerful capabilities of the Python framework Pandas which can easily process big datasets. 
All steps are verifiable through a [jupyter notebook](data-processing/process_movie_dataset.ipynb).

- download `title.basics.tsv.gz` and `title.ratings.tsv.gz` from [IMDb](https://www.imdb.com/interfaces/)
- process dataset using Python, Pandas, Numpy:
  - replace empty values by '\N'
  - remove incorrect values (consistent datatype per column)
  - merge Rating- and Movie-table
  - set `tconst` as index

Instead of rerunning the jupyter notebook you can download the 
[Processed Dataset](https://www.dropbox.com/s/rzmhet4qf2joczz/processed_imdb_movies.csv?dl=0).


## Create Database Tables
- execute 'create table'-statements from `src/main/resources/sql-data/schema.sql`
- add test data by executing `src/main/resources/sql-data/test-data.sql`

## Load Data into Database
- copy processed dataset into /tmp/ folder
- load data using `load-data.sql` file (executed with DBeaver)


- [ ] write bash script for loading data into MySQL DB

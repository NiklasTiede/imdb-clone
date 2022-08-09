
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
-[ ] write bash script for installing MySQL DB

## Process Movies/Ratings Datasets

For this I used the powerful capabilities of the Python framework Pandas which can easily process big datasets. 
All steps are verifiable through a [jupyter notebook](data-processing/process_movie_dataset.ipynb).

- download `title.basics.tsv.gz` and `title.ratings.tsv.gz` from [IMDb](https://www.imdb.com/interfaces/)
- process dataset using Python, Pandas, Numpy:
  - replace empty values by '\N'
  - remove incorrect values (consistent datatype per column)
  - merge Rating- and Movie-table
  - set `tconst` as index

## Create Database Tables
- execute 'create table'-statements from `src/main/resources/sql-data/schema.sql`
- add test data by executing `src/main/resources/sql-data/testdata.sql`

## Load Data into Database
- copy processed dataset into /tmp/ folder
- load data using `load-testdata.sql` file (executed with DBeaver)
-[ ] write bash script for loading data into MySQL DB


    ```plantuml format="png" classes="uml myDiagram" alt="My super diagram placeholder" title="My super diagram" width="300px" height="300px"
      Goofy ->  MickeyMouse: calls
      Goofy <-- MickeyMouse: responds
    ```

Syntax:

```markdown
::uml:: [format="png|svg|txt"] [classes="class1 class2 ..."] [alt="text for alt"] [title="Text for title"] [width="300px"] [height="300px"]
  PlantUML script diagram
::end-uml::
```

```shell

```




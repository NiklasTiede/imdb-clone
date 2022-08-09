SET FOREIGN_KEY_CHECKS = 0;
SET UNIQUE_CHECKS = 0;

LOAD DATA INFILE '/tmp/processed_imdb_movies.csv' INTO TABLE IMDBCLONE.movie

    FIELDS TERMINATED BY ','
    ENCLOSED BY '"'
    LINES TERMINATED BY '\n'
    IGNORE 1 LINES
    (id,movie_type,primary_title,original_title,adult,start_year,end_year,runtime_minutes,movie_genre,imdb_rating,imdb_rating_count);

SET UNIQUE_CHECKS = 1;
SET FOREIGN_KEY_CHECKS = 1;

# '/tmp/load_movies/proc_imdb_movies.00001.csv'
# '/tmp/processed_imdb_movies.csv'
# '/tmp/load_movies/%FILENAME%'
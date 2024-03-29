--  Add index after movie data import!

-- analyze which length is good optimal for the prefixed index:
SELECT
    ROUND(SUM(LENGTH(m.primary_title)<10)*100/COUNT(m.primary_title),2) AS pct_length_10,
    ROUND(SUM(LENGTH(m.primary_title)<20)*100/COUNT(m.primary_title),2) AS pct_length_20,
    ROUND(SUM(LENGTH(m.primary_title)<50)*100/COUNT(m.primary_title),2) AS pct_length_50,
    ROUND(SUM(LENGTH(m.primary_title)<100)*100/COUNT(m.primary_title),2) AS pct_length_100
FROM movie_db.movie m;

-- create prefixed index to speed up LIKE queries starting from beginning of field (example: 'term%')
create index movie_idx on movie_db.movie (primary_title(100));
alter table movie_db.movie drop index movie_idx;

-- fulltext search (I have to see its potential yet)
create fulltext index movie_title_ft_idx on movie_db.movie(primary_title(100));
alter table movie_db.movie drop index movie_title_ft_idx;

-- example query:
SELECT * FROM movie_db.movie m WHERE MATCH(m.primary_title) AGAINST('Conjuring' IN NATURAL LANGUAGE MODE) ORDER BY m.imdb_rating_count DESC;


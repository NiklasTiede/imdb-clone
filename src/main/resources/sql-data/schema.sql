
create table movie (
    id bigint AUTO_INCREMENT,
    primary_title varchar(1000),
    original_title varchar(1000),
    start_year int,
    end_year int,
    runtime_minutes int,
    created_at timestamp default current_timestamp,
    modified_at timestamp default current_timestamp on update current_timestamp,
    movie_genre int,
    movie_type int,
    imdb_rating float,
    imdb_rating_count int,
    adult boolean,
    rating float,
    rating_count int default 0,
    primary key (id)
);

-- analyze which length is good optimal for the prefixed index:
SELECT
    ROUND(SUM(LENGTH(m.primary_title)<10)*100/COUNT(m.primary_title),2) AS pct_length_10,
    ROUND(SUM(LENGTH(m.primary_title)<20)*100/COUNT(m.primary_title),2) AS pct_length_20,
    ROUND(SUM(LENGTH(m.primary_title)<50)*100/COUNT(m.primary_title),2) AS pct_length_50,
    ROUND(SUM(LENGTH(m.primary_title)<100)*100/COUNT(m.primary_title),2) AS pct_length_100
FROM IMDBCLONE.movie m;

-- create prefixed index to speed up LIKE queries starting from beginning of field (example: 'term%')
create index movie_idx on IMDBCLONE.movie (primary_title(100));
alter table IMDBCLONE.movie drop index movie_idx;

-- fulltext search (I have to see its potential yet)
create fulltext index movie_title_ft_idx on IMDBCLONE.movie(primary_title(100));
alter table IMDBCLONE.movie drop index movie_title_ft_idx;

-- example query:
SELECT * FROM IMDBCLONE.movie m WHERE MATCH(m.primary_title) AGAINST('Conjuring' IN NATURAL LANGUAGE MODE) ORDER BY m.imdb_rating_count DESC;

 create table account (
    id bigint AUTO_INCREMENT,
    username varchar(255) unique not null,
    email varchar(255) unique not null,
    password varchar(255) not null,
    first_name varchar(255),
    last_name varchar(255),
    bio text,
    phone varchar(20),
    birthday timestamp,
    created_at timestamp default current_timestamp,
    modified_at timestamp default current_timestamp ON UPDATE current_timestamp,
    primary key (id)
 );

 create table rating (
    rating decimal(2,1) not null,
    created_at timestamp default current_timestamp,
    movie_id bigint,
    account_id bigint,
    primary key (movie_id, account_id)
 );

create table watchlist (
    created_at timestamp default current_timestamp,
    movie_id bigint,
    account_id bigint,
    primary key (movie_id, account_id)
);

create table comment (
    id bigint AUTO_INCREMENT,
    message text,
    created_at timestamp default current_timestamp,
    modified_at timestamp default current_timestamp on update current_timestamp,
    movie_id bigint not null,
    account_id bigint not null,
    primary key (id)
);

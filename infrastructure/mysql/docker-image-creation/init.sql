# This is a copy of schema.sql in src/main/resources/sql

# set up database with data
DROP DATABASE IF EXISTS movie_db;

CREATE DATABASE movie_db;

USE movie_db;

# create tables
create table movie (
    id bigint AUTO_INCREMENT comment 'IMDb title id stored without the tt prefix, for example tt3235888 is stored as 3235888',
    primary_title varchar(1000),
    original_title varchar(1000),
    start_year int,
    end_year int,
    runtime_minutes int,
    created_at_in_utc timestamp default (utc_timestamp),
    modified_at_in_utc timestamp default (utc_timestamp),
    movie_genre bigint comment 'Bitmask of MovieGenreEnum values, not a foreign key',
    movie_type varchar(50) comment 'MovieTypeEnum name stored as text to avoid enum ordinal drift',
    imdb_rating float,
    imdb_rating_count int,
    adult boolean,
    rating decimal(3,1),
    rating_count int default 0,
    description text,
    image_url_token varchar(255) comment 'Object-storage token used to derive movie image object keys',
    primary key (id)
);

create table account (
    id bigint AUTO_INCREMENT,
    username varchar(255) unique not null,
    email varchar(255) unique not null,
    password varchar(255) not null,
    first_name varchar(255),
    last_name varchar(255),
    bio text,
    phone varchar(20),
    birthday date,
    image_url_token varchar(255) comment 'Object-storage token used to derive profile image object keys',
    created_at_in_utc timestamp default (utc_timestamp),
    modified_at_in_utc timestamp,
    locked boolean not null,
    enabled boolean not null,
    primary key (id)
);

create table role (
    id bigint AUTO_INCREMENT,
    name varchar(255) unique not null,
    created_at_in_utc timestamp default (utc_timestamp),
    primary key (id)
) comment 'Table storing roles available in the system';

create table account_roles (
    account_id bigint not null,
    roles_id bigint not null,
    created_at_in_utc timestamp default (utc_timestamp),
    modified_at_in_utc timestamp default (utc_timestamp),
    primary key (account_id, roles_id),
    constraint fk_account_roles_role foreign key (roles_id)
        references role(id)
        on delete cascade,
    constraint fk_account_roles_account foreign key (account_id)
        references account(id)
        on delete cascade,
    index idx_account_roles_roles_id (roles_id)
);

create table rating (
    movie_id bigint not null,
    account_id bigint not null,
    rating decimal(3,1) not null,
    created_at_in_utc timestamp default (utc_timestamp),
    modified_at_in_utc timestamp,
    primary key (movie_id, account_id),
    constraint fk_rating_movie foreign key (movie_id)
        references movie(id)
        on delete cascade,
    constraint fk_rating_account foreign key (account_id)
        references account(id)
        on delete cascade,
    index idx_rating_account_id (account_id)
) comment 'User ratings; one rating per account and movie';

create table watched_movie (
    movie_id bigint not null,
    account_id bigint not null,
    created_at_in_utc timestamp default (utc_timestamp),
    primary key (movie_id, account_id),
    constraint fk_watched_movie_movie foreign key (movie_id)
        references movie(id)
        on delete cascade,
    constraint fk_watched_movie_account foreign key (account_id)
        references account(id)
        on delete cascade,
    index idx_watched_movie_movie_id (movie_id),
    index idx_watched_movie_account_id (account_id)
) comment 'Movies marked as watched by users';

create table comment (
    id bigint AUTO_INCREMENT,
    movie_id bigint not null,
    account_id bigint not null,
    message text not null,
    created_at_in_utc timestamp default (utc_timestamp),
    modified_at_in_utc timestamp default (utc_timestamp),
    primary key (id),
    constraint fk_comment_movie foreign key (movie_id)
        references movie(id)
        on delete cascade,
    constraint fk_comment_account foreign key (account_id)
        references account(id)
        on delete cascade,
    index idx_comment_movie_id_created_at_in_utc (movie_id, created_at_in_utc),
    index idx_comment_account_id_created_at_in_utc (account_id, created_at_in_utc)
);

create table verification_token (
    id bigint AUTO_INCREMENT,
    account_id bigint not null,
    verification_type varchar(100) not null,
    token varchar(36) not null,
    expiry_date_in_utc timestamp not null,
    confirmed_at_in_utc timestamp,
    primary key (id),
    constraint fk_verification_token_account foreign key (account_id)
        references account(id)
        on delete cascade,
    index idx_verification_token_account_id (account_id)
) comment 'Verification and password-reset tokens issued to accounts';


# insert mandatory Role names
insert into movie_db.role(name) values('ROLE_ADMIN');
insert into movie_db.role(name) values('ROLE_USER');


# load data from csv into db
SET FOREIGN_KEY_CHECKS = 0;
SET UNIQUE_CHECKS = 0;

LOAD DATA INFILE '/tmp/processed_imdb_movies.csv' INTO TABLE movie_db.movie

    FIELDS TERMINATED BY ','
    ENCLOSED BY '"'
    LINES TERMINATED BY '\n'
    IGNORE 1 LINES
    (id,movie_type,primary_title,original_title,adult,start_year,end_year,runtime_minutes,movie_genre,imdb_rating,imdb_rating_count,description,image_url_token);

SET UNIQUE_CHECKS = 1;
SET FOREIGN_KEY_CHECKS = 1;

create index movie_idx on movie_db.movie (primary_title(100));

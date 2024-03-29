-- Attempt to create the database if it doesn't already exist
create database if not exists movie_db;
use movie_db;

-- Drop tables to clean
drop table if exists comment;
drop table if exists watched_movie;
drop table if exists rating;
drop table if exists account_roles;
drop table if exists verification_token;
drop table if exists movie;
drop table if exists account;
drop table if exists role;


-- Create table schemas
create table movie (
    id bigint auto_increment,
    primary_title varchar(1000),
    original_title varchar(1000),
    start_year int,
    end_year int,
    runtime_minutes int,
    created_at_in_utc timestamp default current_timestamp,
    modified_at_in_utc timestamp on update current_timestamp,
    movie_genre int,
    movie_type int,
    imdb_rating float,
    imdb_rating_count int,
    adult boolean,
    rating float,
    rating_count int default 0,
    description text,
    image_url_token varchar(50),
    primary key (id)
);

create table account (
    id bigint auto_increment,
    username varchar(255) unique not null,
    email varchar(255) unique not null,
    password varchar(255) not null,
    first_name varchar(255),
    last_name varchar(255),
    bio text,
    phone varchar(20),
    birthday date,
    image_url_token varchar(50),
    created_at_in_utc timestamp default current_timestamp,
    modified_at_in_utc timestamp on update current_timestamp,
    locked boolean not null,
    enabled boolean not null,
    primary key (id)
);

create table role (
    id bigint auto_increment,
    name varchar(255) unique not null,
    created_at_in_utc timestamp default current_timestamp,
    primary key (id)
);

create table account_roles (
    account_id bigint not null,
    roles_id bigint not null,
    created_at_in_utc timestamp default current_timestamp,
    primary key (account_id, roles_id),
    foreign key (roles_id) references role(id),
    foreign key (account_id) references account(id)
);

create table rating (
    movie_id bigint,
    account_id bigint,
    rating decimal(3,1) not null,
    created_at_in_utc timestamp default current_timestamp,
    modified_at_in_utc timestamp on update current_timestamp,
    primary key (movie_id, account_id),
    foreign key (movie_id) references movie(id),
    foreign key (account_id) references account(id)
);

create table watched_movie (
    movie_id bigint,
    account_id bigint,
    created_at_in_utc timestamp default current_timestamp,
    primary key (movie_id, account_id),
    foreign key (movie_id) references movie(id),
    foreign key (account_id) references account(id)
);

create table comment (
    id bigint auto_increment,
    movie_id bigint not null,
    account_id bigint not null,
    message text,
    created_at_in_utc timestamp default current_timestamp,
    modified_at_in_utc timestamp on update current_timestamp,
    primary key (id),
    foreign key (movie_id) references movie(id),
    foreign key (account_id) references account(id)
);

create table verification_token (
    id bigint auto_increment,
    account_id bigint not null,
    verification_type varchar(100) not null,
    token varchar(36) not null,
    expiry_date_in_utc timestamp not null,
    confirmed_at_in_utc timestamp,
    primary key (id),
    foreign key (account_id) references account(id)
);


-- Mandatory Inserts
insert into role (name) values('ROLE_ADMIN');
insert into role (name) values('ROLE_USER');

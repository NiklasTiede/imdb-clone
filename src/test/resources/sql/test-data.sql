delete from scheduled_tasks;
delete from comment;
delete from watched_movie;
delete from rating;
delete from account_roles;
delete from local_credential;
delete from account_identity_provider;
delete from verification_token;
delete from account;
delete from movie;

insert into role(id, name)
values
    (1, 'ROLE_ADMIN'),
    (2, 'ROLE_USER')
on conflict (id) do update
set name = excluded.name;

insert into movie(
    id,
    imdb_id,
    tmdb_id,
    primary_title,
    original_title,
    start_year,
    end_year,
    runtime_minutes,
    movie_genre,
    movie_type,
    imdb_rating,
    imdb_rating_count,
    adult,
    rating,
    rating_count,
    description,
    poster_image_token)
values
    (
        1,
        'tt0000001',
        100001,
        'testMovieOnePri',
        'testMovieOneOri',
        2010,
        2010,
        100,
        16409,
        'MOVIE',
        8.0,
        528339,
        false,
        null,
        0,
        'Awesome movie.',
        'superUrlTokenOne'),
    (
        2,
        'tt0000002',
        100002,
        'testMovieTwoPri',
        'testMovieTwoOri',
        2014,
        2014,
        90,
        16409,
        'MOVIE',
        7.0,
        67839,
        false,
        null,
        0,
        'Super movie.',
        'superUrlTokenTwo');

insert into account(id, username, email, password, locked, enabled)
values
    (
        1,
        'test_user_one',
        'one@gmail.com',
        '$2a$10$hjtGB7lxHBkTL51MEEsA/Oap2frjDpunlH/njP4xvuiV06RiCWKeW',
        false,
        true),
    (
        2,
        'test_user_two',
        'two@web.com',
        '$2a$10$hjtGB7lxHBkTL51MEEsA/Oap2frjDpunlH/njP4xvuiV06RiCWKeW',
        false,
        true);

insert into account_roles(account_id, roles_id)
values
    (1, 1),
    (1, 2),
    (2, 2);

insert into local_credential(account_id, password_hash)
select id, password
from account;

select setval(pg_get_serial_sequence('movie', 'id'), (select max(id) from movie));
select setval(pg_get_serial_sequence('account', 'id'), (select max(id) from account));
select setval(pg_get_serial_sequence('role', 'id'), (select max(id) from role));

insert into movie(id, primary_title, original_title, start_year, end_year, runtime_minutes, created_at_in_utc, modified_at_in_utc, movie_genre, movie_type, imdb_rating, imdb_rating_count, adult, rating, rating_count, description, image_url_token)
values(1,'testMovieOnePri','testMovieOneOri',2010,2010,100,now(),now(),16409,1,8.0,528339,0,null,0,'Awesome movie.','superUrlTokenOne');
insert into movie(id, primary_title, original_title, start_year, end_year, runtime_minutes, created_at_in_utc, modified_at_in_utc, movie_genre, movie_type, imdb_rating, imdb_rating_count, adult, rating, rating_count, description, image_url_token)
values(2,'testMovieTwoPri','testMovieTwoOri',2014,2014,90,now(),now(),16409,1,7.0,67839,0,null,0,'Super movie.','superUrlTokenTwo');

insert into account(id,username,email,password,locked,enabled)
values(1,'test_user_one','one@gmail.com','$2a$10$hjtGB7lxHBkTL51MEEsA/Oap2frjDpunlH/njP4xvuiV06RiCWKeW', false, true);
insert into account(id,username,email,password,locked,enabled)
values(2,'test_user_two','two@web.com','$2a$10$hjtGB7lxHBkTL51MEEsA/Oap2frjDpunlH/njP4xvuiV06RiCWKeW', false, true);

insert into account_roles(account_id, roles_id)
values(1, 1);
insert into account_roles(account_id, roles_id)
values(1, 2);
insert into account_roles(account_id, roles_id)
values(2, 2);

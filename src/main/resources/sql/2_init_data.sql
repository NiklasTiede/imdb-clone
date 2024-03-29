
insert into movie(id, primary_title, original_title, start_year, end_year, runtime_minutes, created_at_in_utc, modified_at_in_utc, movie_genre, movie_type, imdb_rating, imdb_rating_count, adult, rating, rating_count, description, image_url_token)
values(2872718,'Nightcrawler','Nightcrawler',2014,null,117,'2023-07-31 06:07:12','2023-07-31 06:07:12',16409,1,7.8,528339,0,null,0,'When Lou Bloom, desperate for work, muscles into the world of L.A. crime journalism, he blurs the line between observer and participant to become the star of his own story. Aiding him in his effort is Nina, a TV-news veteran.','9BGAIYNfdY90aIkV66dIJ6Olee7JGn');

# add typical scenarios

# # test data
# insert into movie_db.account(id,username,email,password,first_name,last_name,bio,phone,birthday,locked,enabled) values(1,'les_grossman','tom.cruise@yahoo.de','UnencryptedPa55worD','Tom','Cruise','I will massacre you!','01628264723', '1962-07-03 00:00:00',false,true);
# insert into movie_db.account(id,username,email,password,first_name,last_name,bio,phone,birthday,locked,enabled) values(2,'jeff_portnoy','jack.black@gmail.com','UnencryptedPa55worD','jack','black','You go, girl!','015122973088', '1969-08-28 00:00:00',false,true);
#
# insert into movie_db.rating(rating,movie_id,account_id) values(9.2,942385,1);
# insert into movie_db.rating(rating,movie_id,account_id) values(7.7,369339,1);
# insert into movie_db.rating(rating,movie_id,account_id) values(7.8,942385,2);
# insert into movie_db.rating(rating,movie_id,account_id) values(8.1,332379,2);
#
# insert into movie_db.watched_movie(movie_id,account_id) values(942385,1);
# insert into movie_db.watched_movie(movie_id,account_id) values(369339,1);
# insert into movie_db.watched_movie(movie_id,account_id) values(942385,1);
#
# insert into movie_db.comment(message,movie_id,account_id) values('What an outrageous cast!',942385,1);
# insert into movie_db.comment(message,movie_id,account_id) values('The shoot was sometimes difficult.',369339,1);
# insert into movie_db.comment(message,movie_id,account_id) values('All except for one ;-)',942385,2);
# insert into movie_db.comment(message,movie_id,account_id) values('It was fun to play!',332379,2);

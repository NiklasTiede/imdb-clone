
# mandatory inserts
insert into IMDBCLONE.role (name) values('ROLE_ADMIN');
insert into IMDBCLONE.role (name) values('ROLE_USER');


# test data
insert into IMDBCLONE.account(id,username,email,password,first_name,last_name,bio,phone,birthday,locked,enabled) values(1,'les_grossman','tom.cruise@yahoo.de','UnencryptedPa55worD','Tom','Cruise','I will massacre you!','01628264723', '1962-07-03 00:00:00',false,true);
insert into IMDBCLONE.account(id,username,email,password,first_name,last_name,bio,phone,birthday,locked,enabled) values(2,'jeff_portnoy','jack.black@gmail.com','UnencryptedPa55worD','jack','black','You go, girl!','015122973088', '1969-08-28 00:00:00',false,true);

insert into IMDBCLONE.rating(rating,movie_id,account_id) values(9.2,942385,1);
insert into IMDBCLONE.rating(rating,movie_id,account_id) values(7.7,369339,1);
insert into IMDBCLONE.rating(rating,movie_id,account_id) values(7.8,942385,2);
insert into IMDBCLONE.rating(rating,movie_id,account_id) values(8.1,332379,2);

insert into IMDBCLONE.watched_movie(movie_id,account_id) values(942385,1);
insert into IMDBCLONE.watched_movie(movie_id,account_id) values(369339,1);
insert into IMDBCLONE.watched_movie(movie_id,account_id) values(942385,1);

insert into IMDBCLONE.comment(message,movie_id,account_id) values('What an outrageous cast!',942385,1);
insert into IMDBCLONE.comment(message,movie_id,account_id) values('The shoot was sometimes difficult.',369339,1);
insert into IMDBCLONE.comment(message,movie_id,account_id) values('All except for one ;-)',942385,2);
insert into IMDBCLONE.comment(message,movie_id,account_id) values('It was fun to play!',332379,2);

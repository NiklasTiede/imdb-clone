
# test data
insert into IMDBCLONE.movie(primary_title,original_title,start_year,end_year,RUNTIME_MINUTES,MODIFIED_AT,CREATED_AT,MOVIE_GENRE,MOVIE_TYPE,IMDB_RATING,IMDB_RATING_COUNT,ADULT) values ('Le clown et ses chiens','Le clown et ses chiens',1988, 1989, 120, null, null, 2433, 1,5.8,4299, 0);

insert into IMDBCLONE.account(username,email,password,first_name,last_name,bio,phone,birthday,locked,enabled) values('berndasbrot','niklas@gmail.com','28dh36GHD23gh!!dj','niklas','tiede','ich bin student.','015122973088', '1993-03-15 00:00:00',false,false);
insert into IMDBCLONE.account(username,email,password,first_name,last_name,bio,phone,birthday,locked,enabled) values('hotgirl18','mel@yahoo.de','12345678','Eva','Adam','hey guys.','01714927653', '1999-02-19 00:00:00',false,false);
insert into IMDBCLONE.account(username,email,password,first_name,last_name,bio,phone,birthday,locked,enabled) values('Bongoman','schnuggi@yahoo.de','1345338','Bryan','Cranston','hey guys.','01628264723', '1984-08-25 00:00:00',false,false);

insert into IMDBCLONE.rating(rating,movie_id,account_id) values(5.7,1457767,1);
insert into IMDBCLONE.rating(rating,movie_id,account_id) values(6.7,1457767,2);
insert into IMDBCLONE.rating(rating,movie_id,account_id) values(7.1,10133422,1);
insert into IMDBCLONE.rating(rating,movie_id,account_id) values(6.7,10133422,2);

insert into IMDBCLONE.watchlist(movie_id,account_id) values(1457767,1);
insert into IMDBCLONE.watchlist(movie_id,account_id) values(1396484,1);
insert into IMDBCLONE.watchlist(movie_id,account_id) values(1457767,2);

insert into IMDBCLONE.comment(message,movie_id,account_id) values('Message1',1457767,1);
insert into IMDBCLONE.comment(message,movie_id,account_id) values('Message2',10133422,2);
insert into IMDBCLONE.comment(message,movie_id,account_id) values('Message3',1457767,1);
insert into IMDBCLONE.comment(message,movie_id,account_id) values('Message4',10133422,1);

insert into IMDBCLONE.verification_token  (verification_type,token,expiry_date_in_utc,account_id) values('EMAIL_CONFIRMATION','ebde1c2c-5960-421a-9e71-2c1161f3e9e4', now(), 1);
insert into IMDBCLONE.verification_token  (verification_type,token,expiry_date_in_utc,account_id) values('PASSWORD_RESET','489e781f-9134-4c76-985e-b4d39781cc87', now(), 1);
insert into IMDBCLONE.verification_token  (verification_type,token,expiry_date_in_utc,account_id) values('EMAIL_CONFIRMATION','0d971513-ea59-4ab7-87c6-8ba032937a9c', now(), 1);

# mandatory inserts
insert into IMDBCLONE.role (name) values('ROLE_ADMIN');
insert into IMDBCLONE.role (name) values('ROLE_USER');

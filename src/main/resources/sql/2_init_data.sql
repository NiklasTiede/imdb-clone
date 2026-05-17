-- Lightweight local development data.

delete from scheduled_tasks;
delete from comment;
delete from watched_movie;
delete from rating;
delete from account_roles;
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
    movie_type,
    primary_title,
    original_title,
    adult,
    start_year,
    end_year,
    runtime_minutes,
    movie_genre,
    imdb_rating,
    imdb_rating_count,
    description,
    poster_image_token)
values
    (1, 'tt2872718', 'MOVIE', 'Nightcrawler', 'Nightcrawler', false, 2014, null, 117, 16409, 7.8, 528339, 'When Lou Bloom, desperate for work, muscles into the world of L.A. crime journalism, he blurs the line between observer and participant to become the star of his own story.', '9BGAIYNfdY90aIkV66dIJ6Olee7JGn'),
    (2, 'tt0099348', 'MOVIE', 'Dances with Wolves', 'Dances with Wolves', false, 1990, null, 181, 16673, 8.0, 261990, 'Wounded Civil War soldier John Dunbar finds unlikely friendship on the Western frontier.', 'vviPMlGImqliPsQSfNHMjodNyfqCip'),
    (3, 'tt0107290', 'MOVIE', 'Jurassic Park', 'Jurassic Park', false, 1993, null, 127, 8577, 8.2, 958694, 'A wealthy entrepreneur secretly creates a theme park featuring living dinosaurs drawn from prehistoric DNA.', 'XFszo4K1Dx46CICcWWIsRDylBBdoIG'),
    (4, 'tt0109830', 'MOVIE', 'Forrest Gump', 'Forrest Gump', false, 1994, null, 142, 49153, 8.8, 2010325, 'A man with a low IQ has accomplished great things and been present during significant historic events.', 'GMhJaADtgT6sWNFwTckJoRlEwzUXVL'),
    (5, 'tt0112573', 'MOVIE', 'Braveheart', 'Braveheart', false, 1995, null, 178, 409601, 8.4, 1020751, 'Scottish warrior William Wallace leads his countrymen against English rule.', 'kp1LDdvQp7O826hLJPRaKINZwnzNo5'),
    (6, 'tt0113497', 'MOVIE', 'Jumanji', 'Jumanji', false, 1995, null, 104, 1793, 7.0, 338629, 'An enchanted board game opens the door to a dangerous world.', 'Ytfkf2FbKoMSM9H3SF0SYbvNiOuNU8'),
    (7, 'tt0119116', 'MOVIE', 'The Fifth Element', 'The Fifth Element', false, 1997, null, 126, 8577, 7.6, 466620, 'A taxi driver is unintentionally given the task of saving humanity.', 'acHu5vvpRpyzfvzNbUQtdab6D2L4WF'),
    (8, 'tt0119217', 'MOVIE', 'Good Will Hunting', 'Good Will Hunting', false, 1997, null, 126, 49153, 8.3, 942829, 'A janitor at MIT has a genius-level IQ and is pushed to reach his potential.', 'bdtSw7hcj3uelSR4KjpbIROWnDPver'),
    (9, 'tt0120382', 'MOVIE', 'The Truman Show', 'The Truman Show', false, 1998, null, 103, 17409, 8.2, 1044729, 'Truman Burbank is unknowingly the star of a 24-hour-a-day reality TV show.', 'DjDROnwbTUjH5Rh3qqZhXD3eSVeaDe'),
    (10, 'tt0120689', 'MOVIE', 'The Green Mile', 'The Green Mile', false, 1999, null, 189, 20497, 8.6, 1264986, 'A supernatural tale set on death row in a Southern prison.', '2ObxIPGNeyjTASgQ2Hy9m39cnwbOhM'),
    (11, 'tt0120737', 'MOVIE', 'The Lord of the Rings: The Fellowship of the Ring', 'The Lord of the Rings: The Fellowship of the Ring', false, 2001, null, 178, 16769, 8.8, 1808014, 'A fellowship forms to protect the ringbearer on the road to Mount Doom.', 'L5Yz2AL93WrclurlAx2BicKL2wAsDs'),
    (12, 'tt0162222', 'MOVIE', 'Cast Away', 'Cast Away', false, 2000, null, 143, 49409, 7.8, 579208, 'A FedEx manager survives a plane crash and is marooned on a deserted island.', 'eXFkN9Iy0TzgjkMhVVrPeNYVX6eveo'),
    (13, 'tt0469494', 'MOVIE', 'There Will Be Blood', 'There Will Be Blood', false, 2007, null, 158, 16385, 8.2, 573323, 'An oil prospector builds an empire and a slow-burning feud in California.', 'cIykXRDhgRNUxmcYWd9MjA07W2QXG9'),
    (14, 'tt0816692', 'MOVIE', 'Interstellar', 'Interstellar', false, 2014, null, 169, 24833, 8.6, 1744519, 'Explorers travel through a wormhole to secure humanity''s future.', 'HcFe5cZFGVUivYvn4HiCyXo4uSDonI'),
    (15, 'tt0942385', 'MOVIE', 'Tropic Thunder', 'Tropic Thunder', false, 2008, null, 107, 1217, 7.0, 408384, 'Self-absorbed actors shooting a war movie encounter real danger.', 'fQGsfDj3Bkt7Uzen76LFi2aFd9GGPw'),
    (16, 'tt1201607', 'MOVIE', 'Harry Potter and the Deathly Hallows: Part 2', 'Harry Potter and the Deathly Hallows: Part 2', false, 2011, null, 130, 4355, 8.1, 851842, 'Harry, Ron and Hermione continue their quest to defeat Voldemort.', 'xyY8UzGYRa8DPljn7kTtAZu1ovM6it'),
    (17, 'tt1298649', 'MOVIE', 'The Watch', 'The Watch', false, 2012, null, 102, 9217, 5.7, 127154, 'Four suburban neighbors discover their town has been overrun by aliens.', '0ckteLCCRMw42ww2y7uD8gomOWEnFy'),
    (18, 'tt1392190', 'MOVIE', 'Mad Max: Fury Road', 'Mad Max: Fury Road', false, 2015, null, 120, 8577, 8.1, 975227, 'Two rebels flee across a desert wasteland and challenge a tyrant.', 'XZibrHddPi9xqHk4jI0iUoByiCkOc1'),
    (19, 'tt1457767', 'MOVIE', 'The Conjuring', 'The Conjuring', false, 2013, null, 112, 15, 7.5, 493920, 'Paranormal investigators help a family terrorized by a dark presence.', 'DoamaWHqAaGNIJVf4RuS1ORnTA1yzb'),
    (20, 'tt3235888', 'MOVIE', 'It Follows', 'It Follows', false, 2014, null, 100, 15, 6.8, 235138, 'A teenager and her friends confront a fatal curse.', 'RAkRPjw3JLR1VIlMwmXMkezl4MFSe2'),
    (21, 'tt5606664', 'MOVIE', 'Doctor Sleep', 'Doctor Sleep', false, 2019, null, 152, 20483, 7.3, 185596, 'Dan Torrance meets a teenager with a powerful extrasensory gift.', '88BNOZyP06fSHIBDq8aKlSnrXfqlF8');

insert into account(id, username, email, password, first_name, last_name, bio, phone, birthday, locked, enabled)
values
    (1, 'les_grossman', 'tom.cruise@gmail.com', '$2a$10$hjtGB7lxHBkTL51MEEsA/Oap2frjDpunlH/njP4xvuiV06RiCWKeW', 'Tom', 'Cruise', 'I will massacre you!', '491628264723', '1962-07-03', false, true),
    (2, 'jeff_portnoy', 'jack.black@gmail.com', '$2a$10$hjtGB7lxHBkTL51MEEsA/Oap2frjDpunlH/njP4xvuiV06RiCWKeW', 'jack', 'black', 'You go, girl!', '4915122973088', '1969-08-28', false, true);

insert into account_roles(account_id, roles_id)
values
    (1, 1),
    (1, 2),
    (2, 2);

insert into verification_token(id, account_id, verification_type, token, expiry_date_in_utc, confirmed_at_in_utc)
values
    (1, 1, 'EMAIL_CONFIRMATION', '7386f94d-a54d-4d7a-aa65-201a43536198', now(), now() + interval '1 hour'),
    (2, 2, 'EMAIL_CONFIRMATION', '7711fb83-9dd7-47f2-81b3-ce105e993efd', now(), now() + interval '1 hour');

insert into rating(movie_id, account_id, rating)
values
    (9, 1, 9.5),
    (7, 1, 8.5),
    (9, 2, 7.7);

insert into watched_movie(movie_id, account_id)
values
    (9, 1),
    (7, 1),
    (9, 2);

insert into comment(id, movie_id, account_id, message)
values
    (1, 9, 1, 'What an outrageous cast!'),
    (2, 7, 1, 'This is my most favorite movie'),
    (3, 9, 2, 'I love this movie!');

select setval(pg_get_serial_sequence('movie', 'id'), (select max(id) from movie));
select setval(pg_get_serial_sequence('account', 'id'), (select max(id) from account));
select setval(pg_get_serial_sequence('role', 'id'), (select max(id) from role));
select setval(pg_get_serial_sequence('comment', 'id'), (select max(id) from comment));
select setval(pg_get_serial_sequence('verification_token', 'id'), (select max(id) from verification_token));

-- Legacy authorization-server tables: the application is an OAuth2 client.
-- Fail on unexpected dependencies or busy tables; never cascade to other objects.
set local lock_timeout = '5s';
drop table if exists oauth2_authorization_consent;
drop table if exists oauth2_authorization;

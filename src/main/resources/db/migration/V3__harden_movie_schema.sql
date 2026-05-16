alter table movie
    modify id bigint auto_increment
        comment 'IMDb title id stored without the tt prefix, for example tt3235888 is stored as 3235888',
    modify movie_genre bigint
        comment 'Bitmask of MovieGenreEnum values, not a foreign key',
    modify movie_type varchar(50)
        comment 'MovieTypeEnum name stored as text to avoid enum ordinal drift',
    modify rating decimal(3,1),
    modify image_url_token varchar(255)
        comment 'Object Storage object-name token used to derive movie image object keys';

update movie
set movie_type = case movie_type
    when '0' then 'SHORT'
    when '1' then 'MOVIE'
    when '2' then 'VIDEO'
    when '3' then 'TV_MOVIE'
    when '4' then 'TV_EPISODE'
    when '5' then 'TV_MINI_SERIES'
    when '6' then 'TV_SPECIAL'
    when '7' then 'TV_SERIES'
    when '8' then 'TV_SHORT'
    when '9' then 'TV_PILOT'
    when '10' then 'VIDEO_GAME'
    else movie_type
end;

alter table account
    modify image_url_token varchar(255)
        comment 'Object Storage object-name token used to derive profile image object keys';

set @constraint_name = (
    select constraint_name
    from information_schema.referential_constraints
    where constraint_schema = database()
      and table_name = 'account_roles'
      and referenced_table_name = 'role'
    limit 1
);
set @statement = if(
    @constraint_name is null,
    'select 1',
    concat('alter table account_roles drop foreign key ', @constraint_name)
);
prepare statement_to_execute from @statement;
execute statement_to_execute;
deallocate prepare statement_to_execute;

set @constraint_name = (
    select constraint_name
    from information_schema.referential_constraints
    where constraint_schema = database()
      and table_name = 'account_roles'
      and referenced_table_name = 'account'
    limit 1
);
set @statement = if(
    @constraint_name is null,
    'select 1',
    concat('alter table account_roles drop foreign key ', @constraint_name)
);
prepare statement_to_execute from @statement;
execute statement_to_execute;
deallocate prepare statement_to_execute;

set @index_name = (
    select index_name
    from information_schema.statistics
    where table_schema = database()
      and table_name = 'account_roles'
      and index_name in ('roles_id', 'idx_account_roles_roles_id')
    limit 1
);
set @statement = if(
    @index_name is not null,
    concat('alter table account_roles drop index ', @index_name),
    'select 1'
);
prepare statement_to_execute from @statement;
execute statement_to_execute;
deallocate prepare statement_to_execute;

alter table account_roles
    add constraint fk_account_roles_role foreign key (roles_id)
        references role(id)
        on delete cascade,
    add constraint fk_account_roles_account foreign key (account_id)
        references account(id)
        on delete cascade,
    add index idx_account_roles_roles_id (roles_id);

set @constraint_name = (
    select constraint_name
    from information_schema.referential_constraints
    where constraint_schema = database()
      and table_name = 'rating'
      and referenced_table_name = 'movie'
    limit 1
);
set @statement = if(
    @constraint_name is null,
    'select 1',
    concat('alter table rating drop foreign key ', @constraint_name)
);
prepare statement_to_execute from @statement;
execute statement_to_execute;
deallocate prepare statement_to_execute;

set @constraint_name = (
    select constraint_name
    from information_schema.referential_constraints
    where constraint_schema = database()
      and table_name = 'rating'
      and referenced_table_name = 'account'
    limit 1
);
set @statement = if(
    @constraint_name is null,
    'select 1',
    concat('alter table rating drop foreign key ', @constraint_name)
);
prepare statement_to_execute from @statement;
execute statement_to_execute;
deallocate prepare statement_to_execute;

set @index_name = (
    select index_name
    from information_schema.statistics
    where table_schema = database()
      and table_name = 'rating'
      and index_name in ('account_id', 'idx_rating_account_id')
    limit 1
);
set @statement = if(
    @index_name is not null,
    concat('alter table rating drop index ', @index_name),
    'select 1'
);
prepare statement_to_execute from @statement;
execute statement_to_execute;
deallocate prepare statement_to_execute;

alter table rating
    modify movie_id bigint not null,
    modify account_id bigint not null,
    add constraint fk_rating_movie foreign key (movie_id)
        references movie(id)
        on delete cascade,
    add constraint fk_rating_account foreign key (account_id)
        references account(id)
        on delete cascade,
    add index idx_rating_account_id (account_id),
    comment 'User ratings; one rating per account and movie';

set @constraint_name = (
    select constraint_name
    from information_schema.referential_constraints
    where constraint_schema = database()
      and table_name = 'comment'
      and referenced_table_name = 'movie'
    limit 1
);
set @statement = if(
    @constraint_name is null,
    'select 1',
    concat('alter table comment drop foreign key ', @constraint_name)
);
prepare statement_to_execute from @statement;
execute statement_to_execute;
deallocate prepare statement_to_execute;

set @constraint_name = (
    select constraint_name
    from information_schema.referential_constraints
    where constraint_schema = database()
      and table_name = 'comment'
      and referenced_table_name = 'account'
    limit 1
);
set @statement = if(
    @constraint_name is null,
    'select 1',
    concat('alter table comment drop foreign key ', @constraint_name)
);
prepare statement_to_execute from @statement;
execute statement_to_execute;
deallocate prepare statement_to_execute;

set @index_name = (
    select index_name
    from information_schema.statistics
    where table_schema = database()
      and table_name = 'comment'
      and index_name in ('movie_id', 'idx_comment_movie_id_created_at_in_utc')
    limit 1
);
set @statement = if(
    @index_name is not null,
    concat('alter table comment drop index ', @index_name),
    'select 1'
);
prepare statement_to_execute from @statement;
execute statement_to_execute;
deallocate prepare statement_to_execute;

set @index_name = (
    select index_name
    from information_schema.statistics
    where table_schema = database()
      and table_name = 'comment'
      and index_name in ('account_id', 'idx_comment_account_id_created_at_in_utc')
    limit 1
);
set @statement = if(
    @index_name is not null,
    concat('alter table comment drop index ', @index_name),
    'select 1'
);
prepare statement_to_execute from @statement;
execute statement_to_execute;
deallocate prepare statement_to_execute;

alter table comment
    add constraint fk_comment_movie foreign key (movie_id)
        references movie(id)
        on delete cascade,
    add constraint fk_comment_account foreign key (account_id)
        references account(id)
        on delete cascade,
    add index idx_comment_movie_id_created_at_in_utc (movie_id, created_at_in_utc),
    add index idx_comment_account_id_created_at_in_utc (account_id, created_at_in_utc);

alter table watched_movie comment 'Movies marked as watched by users';

set @constraint_name = (
    select constraint_name
    from information_schema.referential_constraints
    where constraint_schema = database()
      and table_name = 'verification_token'
      and referenced_table_name = 'account'
    limit 1
);
set @statement = if(
    @constraint_name is null,
    'select 1',
    concat('alter table verification_token drop foreign key ', @constraint_name)
);
prepare statement_to_execute from @statement;
execute statement_to_execute;
deallocate prepare statement_to_execute;

set @index_name = (
    select index_name
    from information_schema.statistics
    where table_schema = database()
      and table_name = 'verification_token'
      and index_name in ('account_id', 'idx_verification_token_account_id')
    limit 1
);
set @statement = if(
    @index_name is not null,
    concat('alter table verification_token drop index ', @index_name),
    'select 1'
);
prepare statement_to_execute from @statement;
execute statement_to_execute;
deallocate prepare statement_to_execute;

alter table verification_token
    add constraint fk_verification_token_account foreign key (account_id)
        references account(id)
        on delete cascade,
    add index idx_verification_token_account_id (account_id),
    comment 'Verification and password-reset tokens issued to accounts';

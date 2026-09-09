create table media_retired_token (
    kind varchar(20) not null check (kind in ('MOVIE', 'PROFILE')),
    token varchar(255) not null,
    retired_at timestamptz not null default current_timestamp,
    check_after timestamptz not null default current_timestamp + interval '1 day',
    primary key (kind, token)
);
create index media_retired_token_check_idx on media_retired_token(check_after);
comment on table media_retired_token is
    'Media-owned permanent retirement tombstones: forbid token resurrection and recheck for delayed remote writes.';

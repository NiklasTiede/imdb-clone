create table media_object_work (
    kind varchar(20) not null check (kind in ('MOVIE', 'PROFILE')),
    token varchar(255) not null,
    state varchar(20) not null check (state in ('STAGED', 'RETIRED')),
    created_at timestamptz not null default current_timestamp,
    available_at timestamptz not null,
    attempts integer not null default 0 check (attempts >= 0),
    primary key (kind, token)
);
create index media_object_work_available_idx on media_object_work(available_at);
comment on table media_object_work is
    'Media-owned upload intents and durable cleanup work; intentionally survives owner deletion.';

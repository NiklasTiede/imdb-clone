create table movie_projection_work (
    movie_id bigint primary key,
    revision bigint not null default 1 check (revision > 0),
    available_at timestamptz not null default current_timestamp,
    created_at timestamptz not null default current_timestamp,
    attempts integer not null default 0 check (attempts >= 0)
);
create index movie_projection_work_available_idx on movie_projection_work(available_at);
comment on table movie_projection_work is
    'Catalog-owned desired projection revisions; intentionally survives movie deletion and scheduler execution.';

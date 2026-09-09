create table movie_search_reindex_job (
    id uuid primary key,
    status varchar(16) not null default 'RUNNING' check (status in ('RUNNING', 'COMPLETED', 'FAILED')),
    phase varchar(16) not null default 'RESET' check (phase in ('RESET', 'SCAN')),
    cursor_movie_id bigint not null default 0,
    upper_movie_id bigint not null default 0,
    indexed_movies bigint not null default 0 check (indexed_movies >= 0),
    total_movies bigint not null check (total_movies >= 0),
    started_at timestamptz not null default current_timestamp,
    finished_at timestamptz,
    available_at timestamptz not null default current_timestamp,
    attempts integer not null default 0 check (attempts >= 0),
    error_message text
);

create unique index movie_search_reindex_single_active
    on movie_search_reindex_job ((status)) where status = 'RUNNING';

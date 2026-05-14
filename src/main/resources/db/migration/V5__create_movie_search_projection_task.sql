create table movie_search_projection_task (
    movie_id bigint not null,
    operation varchar(20) not null,
    requested_at_in_utc timestamp(6) not null default current_timestamp(6),
    attempts int not null default 0,
    last_attempt_at_in_utc timestamp(6),
    last_error varchar(1000),
    primary key (movie_id),
    index idx_movie_search_projection_task_requested_at (requested_at_in_utc)
) comment 'Durable catalog-owned Elasticsearch projection tasks for movie documents';

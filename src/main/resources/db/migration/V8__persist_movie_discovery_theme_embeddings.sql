create table movie_discovery_theme_embedding (
    theme_id varchar(120) not null,
    prompt_version int not null,
    model_name varchar(120) not null,
    dimensions int not null,
    embedding_json text not null,
    created_at_in_utc timestamp(6) with time zone not null default current_timestamp,
    primary key (theme_id)
);

comment on table movie_discovery_theme_embedding is
    'Versioned, derived embeddings for stable semantic discovery themes.';

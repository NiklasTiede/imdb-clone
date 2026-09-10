create table watchlist_action_receipt (
    account_id bigint not null references account(id) on delete cascade,
    operation_id uuid not null,
    movie_id bigint not null,
    created boolean not null,
    added_at timestamptz not null,
    recorded_at timestamptz not null default current_timestamp,
    primary key (account_id, operation_id)
);
create index idx_watchlist_action_receipt_retention on watchlist_action_receipt(recorded_at);

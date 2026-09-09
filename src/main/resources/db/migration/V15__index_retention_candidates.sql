-- Discovery and security telemetry already have time indexes (V6 and V3).
create index verification_token_expiry_idx on verification_token(expiry_date_in_utc);
create index movie_search_reindex_retention_idx on movie_search_reindex_job(finished_at, id)
    where status in ('COMPLETED', 'FAILED');
create index notification_delivery_retention_idx
    on notification_delivery(greatest(completed_at, expires_at), id)
    where state in ('SENT', 'EXPIRED') and completed_at is not null;

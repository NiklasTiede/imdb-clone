create table notification_delivery (
    id varchar(64) primary key,
    kind varchar(32) not null check (kind in ('EMAIL_CONFIRMATION', 'PASSWORD_RESET')),
    state varchar(16) not null default 'PENDING' check (state in ('PENDING', 'SENT', 'EXPIRED')),
    key_id varchar(64),
    encrypted_payload bytea,
    created_at timestamptz not null default current_timestamp,
    available_at timestamptz not null default current_timestamp,
    expires_at timestamptz not null,
    completed_at timestamptz,
    attempts integer not null default 0 check (attempts >= 0),
    check ((state = 'PENDING' and encrypted_payload is not null and key_id is not null)
        or (state <> 'PENDING' and encrypted_payload is null and key_id is null))
);
create index notification_delivery_pending_idx on notification_delivery(available_at)
    where state = 'PENDING';
comment on table notification_delivery is
    'Notification-owned encrypted transactional outbox. Completed IDs deduplicate repeated events; no plaintext mail capabilities.';

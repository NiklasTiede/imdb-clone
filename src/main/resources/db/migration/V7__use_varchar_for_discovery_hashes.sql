alter table discovery_event
    alter column session_hash type varchar(64),
    alter column feed_instance_hash type varchar(64);

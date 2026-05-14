create table scheduled_tasks (
    task_name varchar(100) not null,
    task_instance varchar(100) not null,
    task_data blob,
    execution_time timestamp(6) not null,
    picked boolean not null,
    picked_by varchar(50),
    last_success timestamp(6) null,
    last_failure timestamp(6) null,
    consecutive_failures int,
    last_heartbeat timestamp(6) null,
    version bigint not null,
    priority smallint,
    primary key (task_name, task_instance),
    index execution_time_idx (execution_time),
    index last_heartbeat_idx (last_heartbeat),
    index priority_execution_time_idx (priority desc, execution_time asc)
) comment 'Durable db-scheduler task executions';

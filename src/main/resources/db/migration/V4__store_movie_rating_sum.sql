alter table movie
    add rating_sum decimal(19,1) not null default 0.0
        comment 'Exact sum of app user ratings used to maintain the average rating';

update movie
set rating_count = 0
where rating_count is null;

update movie
set rating_sum = coalesce(rating, 0.0) * rating_count;

alter table movie
    modify rating_count int not null default 0;

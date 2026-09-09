-- Establish a correct baseline for the synchronous rating-delta protocol. Preserve source ratings
-- and unrelated movie metadata. An active old-version writer must finish before retrying migration;
-- NOWAIT avoids waiting in a lock order that could deadlock with its account/movie lifecycle.
lock table rating, movie, movie_projection_work in share row exclusive mode nowait;

with totals as (
    select m.id,
           coalesce(sum(r.rating), 0.0) as rating_sum,
           count(r.movie_id)::integer as rating_count,
           case when count(r.movie_id) = 0 then null
                else round(sum(r.rating) / count(r.movie_id), 1) end as rating
    from movie m
    left join rating r on r.movie_id = m.id
    group by m.id
), corrected as (
    update movie m
    set rating_sum = t.rating_sum, rating_count = t.rating_count, rating = t.rating
    from totals t
    where m.id = t.id
      and (m.rating_sum, m.rating_count, m.rating)
          is distinct from (t.rating_sum, t.rating_count, t.rating)
    returning m.id
)
insert into movie_projection_work(movie_id)
select id from corrected
on conflict (movie_id) do update
set revision = movie_projection_work.revision + 1, available_at = current_timestamp, attempts = 0;

package com.thecodinglab.imdbclone.repository;

import com.thecodinglab.imdbclone.entity.Movie;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface MovieElasticSearchRepository extends ElasticsearchRepository<Movie, Long> {

  Page<Movie> findByPrimaryTitle(String primaryTitle, Pageable pageable);
}

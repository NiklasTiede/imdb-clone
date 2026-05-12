package com.thecodinglab.imdbclone.catalog.internal.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface MovieElasticSearchRepository extends ElasticsearchRepository<Movie, Long> {

  Page<Movie> findByPrimaryTitle(String primaryTitle, Pageable pageable);
}

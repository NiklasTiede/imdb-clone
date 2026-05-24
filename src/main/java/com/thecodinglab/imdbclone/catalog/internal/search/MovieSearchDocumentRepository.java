package com.thecodinglab.imdbclone.catalog.internal.search;

import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface MovieSearchDocumentRepository
    extends ElasticsearchRepository<MovieSearchDocument, Long> {}

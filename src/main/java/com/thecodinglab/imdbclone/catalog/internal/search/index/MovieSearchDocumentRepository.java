package com.thecodinglab.imdbclone.catalog.internal.search.index;

import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface MovieSearchDocumentRepository
    extends ElasticsearchRepository<MovieSearchDocument, Long> {}

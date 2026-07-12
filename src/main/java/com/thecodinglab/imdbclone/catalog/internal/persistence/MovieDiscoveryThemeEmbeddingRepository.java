package com.thecodinglab.imdbclone.catalog.internal.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface MovieDiscoveryThemeEmbeddingRepository
    extends JpaRepository<MovieDiscoveryThemeEmbeddingEntity, String> {}

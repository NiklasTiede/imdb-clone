package com.thecodinglab.imdbclone.recommendation.internal.home;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class HomeFeedSeedTest {

  @Test
  void canonical_usesTheFeedInstanceForAStableFirstRequest() {
    String first = HomeFeedSeed.canonical(null, "browser-document-1");
    String second = HomeFeedSeed.canonical(null, "browser-document-1");

    assertThat(first).isEqualTo(second);
    assertThat(HomeFeedSeed.canonical(null, "browser-document-2")).isNotEqualTo(first);
  }

  @Test
  void canonical_keepsTheServerSeedForContinuationRequests() {
    assertThat(HomeFeedSeed.canonical("server-seed", "another-document")).isEqualTo("server-seed");
  }

  @Test
  void derive_isStableAndSeparatesDiscriminators() {
    long first = HomeFeedSeed.derive("seed", "section-a");

    assertThat(HomeFeedSeed.derive("seed", "section-a")).isEqualTo(first);
    assertThat(HomeFeedSeed.derive("seed", "section-b")).isNotEqualTo(first);
  }
}

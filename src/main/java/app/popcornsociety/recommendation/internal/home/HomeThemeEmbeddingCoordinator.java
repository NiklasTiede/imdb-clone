package app.popcornsociety.recommendation.internal.home;

import app.popcornsociety.catalog.api.MovieDiscoveryThemeEmbeddingProvider;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
class HomeThemeEmbeddingCoordinator {

  private final HomeSectionCatalog sectionCatalog;
  private final MovieDiscoveryThemeEmbeddingProvider themeEmbeddingProvider;

  HomeThemeEmbeddingCoordinator(
      HomeSectionCatalog sectionCatalog,
      MovieDiscoveryThemeEmbeddingProvider themeEmbeddingProvider) {
    this.sectionCatalog = sectionCatalog;
    this.themeEmbeddingProvider = themeEmbeddingProvider;
  }

  @EventListener(ApplicationReadyEvent.class)
  void reconcileThemeEmbeddings() {
    themeEmbeddingProvider.reconcile(sectionCatalog.semanticThemes());
  }
}

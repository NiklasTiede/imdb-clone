@ApplicationModule(
    id = "assistant",
    displayName = "Movie Concierge Gateway",
    allowedDependencies = {
      "catalog::assistant",
      "recommendation::assistant",
      "shared::api",
      "identity::assistant",
      "engagement::assistant"
    })
package app.popcornsociety.assistant;

import org.springframework.modulith.ApplicationModule;

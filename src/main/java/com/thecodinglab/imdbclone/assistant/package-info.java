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
package com.thecodinglab.imdbclone.assistant;

import org.springframework.modulith.ApplicationModule;

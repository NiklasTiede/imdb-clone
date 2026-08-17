@ApplicationModule(
    id = "assistant",
    displayName = "Movie Concierge Gateway",
    allowedDependencies = {"catalog::assistant", "recommendation::assistant", "shared::api"})
package com.thecodinglab.imdbclone.assistant;

import org.springframework.modulith.ApplicationModule;

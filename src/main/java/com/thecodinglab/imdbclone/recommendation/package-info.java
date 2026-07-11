@ApplicationModule(
    id = "recommendation",
    displayName = "Recommendations",
    allowedDependencies = {"catalog::recommendation", "shared::error"})
package com.thecodinglab.imdbclone.recommendation;

import org.springframework.modulith.ApplicationModule;

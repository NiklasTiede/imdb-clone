@ApplicationModule(
    id = "recommendation",
    displayName = "Recommendations",
    allowedDependencies = {
      "catalog::recommendation",
      "shared::error",
      "shared::persistence",
      "shared::security"
    })
package com.thecodinglab.imdbclone.recommendation;

import org.springframework.modulith.ApplicationModule;

@ApplicationModule(
    id = "engagement",
    displayName = "Engagement",
    allowedDependencies = {
      "catalog::ratings",
      "catalog::reference",
      "shared::api",
      "shared::error",
      "shared::persistence",
      "shared::security",
      "shared::validation"
    })
package com.thecodinglab.imdbclone.engagement;

import org.springframework.modulith.ApplicationModule;

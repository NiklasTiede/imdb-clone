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
package app.popcornsociety.engagement;

import org.springframework.modulith.ApplicationModule;

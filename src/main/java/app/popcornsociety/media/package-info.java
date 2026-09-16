@ApplicationModule(
    id = "media",
    displayName = "Media",
    allowedDependencies = {
      "account::media",
      "account::events",
      "catalog::events",
      "catalog::media",
      "shared::api",
      "shared::error",
      "shared::security"
    })
package app.popcornsociety.media;

import org.springframework.modulith.ApplicationModule;

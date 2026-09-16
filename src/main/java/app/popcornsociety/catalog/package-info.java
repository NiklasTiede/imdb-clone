@ApplicationModule(
    id = "catalog",
    displayName = "Movie Catalog",
    allowedDependencies = {
      "shared::api",
      "shared::error",
      "shared::persistence",
      "shared::validation"
    })
package app.popcornsociety.catalog;

import org.springframework.modulith.ApplicationModule;

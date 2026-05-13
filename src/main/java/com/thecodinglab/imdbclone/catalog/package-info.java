@ApplicationModule(
    id = "catalog",
    displayName = "Movie Catalog",
    allowedDependencies = {
      "shared::api",
      "shared::error",
      "shared::persistence",
      "shared::validation"
    })
package com.thecodinglab.imdbclone.catalog;

import org.springframework.modulith.ApplicationModule;

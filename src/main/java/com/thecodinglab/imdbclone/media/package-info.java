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
package com.thecodinglab.imdbclone.media;

import org.springframework.modulith.ApplicationModule;

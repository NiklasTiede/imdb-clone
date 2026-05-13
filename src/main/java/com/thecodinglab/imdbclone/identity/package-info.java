@ApplicationModule(
    id = "identity",
    displayName = "Identity",
    allowedDependencies = {
      "account::api",
      "shared::api",
      "shared::error",
      "shared::security",
      "shared::validation"
    })
package com.thecodinglab.imdbclone.identity;

import org.springframework.modulith.ApplicationModule;
